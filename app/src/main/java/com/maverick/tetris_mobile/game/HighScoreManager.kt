package com.maverick.tetris_mobile.game

import android.content.Context
import java.io.IOException
import androidx.core.content.edit

data class HighScoreEntry(
    val score: Int,
    val level: Int,
    val lines: Int,
    val timestamp: Long
)

class HighScoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("tetris_high_scores", Context.MODE_PRIVATE)

    fun getHighScores(): List<HighScoreEntry> = synchronized(PREFERENCES_LOCK) {
        val count = prefs.getInt("count", 0)
        (0 until count).map { i ->
            HighScoreEntry(
                score = prefs.getInt("score_$i", 0),
                level = prefs.getInt("level_$i", 0),
                lines = prefs.getInt("lines_$i", 0),
                timestamp = prefs.getLong("time_$i", 0L)
            )
        }
    }

    fun getScoreHistory(): List<HighScoreEntry> = synchronized(PREFERENCES_LOCK) {
        val count = prefs.getInt(HISTORY_COUNT_KEY, 0)
        if (count <= 0) return@synchronized emptyList()

        (0 until count).mapNotNull { i ->
            val timestamp = prefs.getLong("history_time_$i", 0L)
            if (timestamp == 0L) {
                null
            } else {
                HighScoreEntry(
                    score = prefs.getInt("history_score_$i", 0),
                    level = prefs.getInt("history_level_$i", 0),
                    lines = prefs.getInt("history_lines_$i", 0),
                    timestamp = timestamp
                )
            }
        }.sortedByDescending { it.timestamp }
    }

    fun lastCompletedSessionId(): Long = synchronized(PREFERENCES_LOCK) { prefs.getLong("last_completed_session", 0L) }

    fun addScore(score: Int, level: Int, lines: Int, sessionId: Long = 0L): Int = synchronized(PREFERENCES_LOCK) {
        val completed = prefs.getLong("last_completed_session", 0L)
        if (sessionId > 0 && sessionId <= completed) {
            return@synchronized if (sessionId == completed) prefs.getInt("last_session_rank", -1) else -1
        }
        val previousValues = prefs.all
        val newEntry = HighScoreEntry(score.coerceAtLeast(0), level, lines, System.currentTimeMillis())
        val history = (getScoreHistory() + newEntry)
            .sortedByDescending { it.timestamp }
            .take(MAX_HISTORY_ENTRIES)
        val top = if (score > 0) {
            (getHighScores() + newEntry).sortedByDescending { it.score }.take(MAX_ENTRIES)
        } else {
            getHighScores()
        }
        val rank = top.indexOfFirst { it === newEntry }

        // History and leaderboard change in one transaction, including scores outside the top five.
        val committed = prefs.edit().apply {
            if (sessionId > 0) {
                putLong("last_completed_session", sessionId)
                putInt("last_session_rank", if (rank == -1) -1 else rank + 1)
            }
            putInt(HISTORY_COUNT_KEY, history.size)
            history.forEachIndexed { i, entry ->
                putInt("history_score_$i", entry.score)
                putInt("history_level_$i", entry.level)
                putInt("history_lines_$i", entry.lines)
                putLong("history_time_$i", entry.timestamp)
            }
            putInt("count", top.size)
            top.forEachIndexed { i, entry ->
                putInt("score_$i", entry.score)
                putInt("level_$i", entry.level)
                putInt("lines_$i", entry.lines)
                putLong("time_$i", entry.timestamp)
            }
        }.commit()
        if (!committed) {
            // commit updates memory even if the disk write fails. Restore it so a retry
            // cannot mistake an uncommitted session for an already saved result.
            prefs.edit {
                clear()
                previousValues.forEach { (key, value) ->
                    when (value) {
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                    }
                }
            }
            throw IOException("Could not save score")
        }

        if (rank == -1) -1 else rank + 1
    }

    fun isHighScore(score: Int): Boolean = synchronized(PREFERENCES_LOCK) {
        if (score <= 0) return@synchronized false
        val entries = getHighScores()
        entries.size < MAX_ENTRIES || score > (entries.lastOrNull()?.score ?: 0)
    }

    companion object {
        private val PREFERENCES_LOCK = Any()
        const val MAX_ENTRIES = 5
        private const val HISTORY_COUNT_KEY = "history_count"
        private const val MAX_HISTORY_ENTRIES = 100
    }
}
