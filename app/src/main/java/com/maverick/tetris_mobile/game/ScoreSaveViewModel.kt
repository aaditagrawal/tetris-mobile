package com.maverick.tetris_mobile.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/** Retains score submissions across screen changes and activity recreation. */
class ScoreSaveViewModel : ViewModel() {
    private val submissions = mutableMapOf<Long, Deferred<Int>>()
    private var latestSessionId = 0L

    suspend fun saveScore(manager: HighScoreManager, state: GameState): Int {
        if (state.sessionId <= 0L) return -1
        if (state.sessionId < latestSessionId) {
            // A stale UI event must not submit a completed older session again.
            return submissions[state.sessionId]?.await() ?: -1
        }
        if (state.sessionId > latestSessionId) {
            latestSessionId = state.sessionId
            submissions.entries.removeAll { it.value.isCompleted }
        }
        // Callers run on the UI thread. Register the job before awaiting it so concurrent
        // game-over, Back, and restart events share the same submission.
        val submission = submissions.getOrPut(state.sessionId) {
            viewModelScope.async(Dispatchers.IO) {
                manager.addScore(state.score, state.level, state.linesCleared, state.sessionId)
            }
        }
        return try { submission.await() } catch (failure: java.io.IOException) {
            if (submissions[state.sessionId] === submission) submissions.remove(state.sessionId)
            throw failure
        }
    }
}
