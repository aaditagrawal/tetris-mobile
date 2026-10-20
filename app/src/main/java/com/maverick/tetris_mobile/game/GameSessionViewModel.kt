package com.maverick.tetris_mobile.game

import android.app.Application
import android.util.AtomicFile
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One serialized writer, retained across Activity recreation. Disk work stays off the UI thread. */
class GameSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val file = AtomicFile(File(application.filesDir, "game-checkpoint.bin"))
    private val scores = HighScoreManager(application)
    private val writes = Mutex()
    private var bound = false
    private var loadFailed = false
    private val sequence = AtomicLong()
    private val lastSavedAt = AtomicLong()
    private var writtenSequence = 0L
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val saveError = mutableError.asStateFlow()

    fun bind(model: GameViewModel) {
        if (bound) return
        bound = true
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    modelFloorAndCheckpoint()
                }
                model.reserveSessionIdsAfter(saved.first)
                saved.second?.let { snapshot ->
                    if (model.sessionId == 0L) model.restore(snapshot)
                    if (snapshot.state.isGameOver) saveSnapshot(snapshot)
                }
            } catch (_: IOException) {
                loadFailed = true
                mutableError.value = "Couldn't load saved progress. Please try again."
            }
            mutableReady.value = true
            model.gameState.collectLatest { state ->
                if (state.sessionId == 0L) return@collectLatest
                // Pause/finish checkpoints are immediate. Movement writes are coalesced.
                if (!state.isPaused && !state.isGameOver) {
                    delay((250 - (SystemClock.elapsedRealtime() - lastSavedAt.get())).coerceAtLeast(0))
                }
                try { saveNow(model) } catch (_: IOException) {
                    mutableError.value = "Couldn't save progress. Please try again."
                }
            }
        }
    }

    private fun modelFloorAndCheckpoint(): Pair<Long, GameSnapshot?> {
        val floor = scores.lastCompletedSessionId()
        val saved = try {
            file.openRead().use { GameSnapshotCodec.decode(it.readNBytes(1025)) }
        } catch (_: java.io.FileNotFoundException) { null }
        // A damaged/obsolete checkpoint must never prevent starting a new game.
        if (saved != null && saved.state.sessionId <= floor) file.delete()
        return floor to saved?.takeIf { it.state.sessionId > floor }
    }

    suspend fun retrySave(model: GameViewModel) {
        if (loadFailed && model.sessionId == 0L) {
            val saved = withContext(Dispatchers.IO) { modelFloorAndCheckpoint() }
            model.reserveSessionIdsAfter(saved.first)
            saved.second?.let { model.restore(it) }
        }
        saveNow(model)
        loadFailed = false
        mutableError.value = null
    }

    suspend fun saveNow(model: GameViewModel) {
        val snapshot = model.snapshot() ?: return
        saveSnapshot(snapshot)
        loadFailed = false
        mutableError.value = null
    }

    private suspend fun saveSnapshot(snapshot: GameSnapshot) {
        val request = sequence.incrementAndGet()
        withContext(Dispatchers.IO) {
            writes.withLock {
                if (request < writtenSequence) return@withLock
                val bytes = GameSnapshotCodec.encode(snapshot)
                val stream = file.startWrite()
                try {
                    stream.write(bytes)
                    stream.fd.sync()
                    file.finishWrite(stream)
                } catch (failure: IOException) {
                    file.failWrite(stream)
                    throw failure
                }
                if (snapshot.state.isGameOver) {
                    // Keep the terminal checkpoint until its score is durably recorded.
                    scores.addScore(snapshot.state.score, snapshot.state.level,
                        snapshot.state.linesCleared, snapshot.state.sessionId)
                    file.delete()
                }
                writtenSequence = request
                lastSavedAt.set(SystemClock.elapsedRealtime())
            }
        }
    }
}
