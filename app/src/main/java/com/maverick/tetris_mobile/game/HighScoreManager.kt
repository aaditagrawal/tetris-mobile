package com.maverick.tetris_mobile.game

import android.content.Context

data class HighScoreEntry(
    val score: Int,
    val level: Int,
    val lines: Int,
    val timestamp: Long
)

class HighScoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("tetris_high_scores", Context.MODE_PRIVATE)

    fun getHighScores(): List<HighScoreEntry> {
        val count = prefs.getInt("count", 0)
        return (0 until count).map { i ->
            HighScoreEntry(
                score = prefs.getInt("score_$i", 0),
                level = prefs.getInt("level_$i", 0),
                lines = prefs.getInt("lines_$i", 0),
                timestamp = prefs.getLong("time_$i", 0L)
            )
        }
    }

    fun getScoreHistory(): List<HighScoreEntry> {
        val count = prefs.getInt(HISTORY_COUNT_KEY, 0)
        if (count <= 0) return emptyList()

        return (0 until count).mapNotNull { i ->
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

    fun addScore(score: Int, level: Int, lines: Int): Int {
        appendScoreHistory(score.coerceAtLeast(0), level, lines)
        if (score <= 0) return -1

        val entries = getHighScores().toMutableList()
        val newEntry = HighScoreEntry(score, level, lines, System.currentTimeMillis())
        entries.add(newEntry)
        entries.sortByDescending { it.score }

        val top = entries.take(MAX_ENTRIES)
        val rank = top.indexOfFirst { it === newEntry }

        if (rank == -1) return -1

        val editor = prefs.edit()
        editor.putInt("count", top.size)
        top.forEachIndexed { i, entry ->
            editor.putInt("score_$i", entry.score)
            editor.putInt("level_$i", entry.level)
            editor.putInt("lines_$i", entry.lines)
            editor.putLong("time_$i", entry.timestamp)
        }
        editor.apply()

        return rank + 1
    }

    fun isHighScore(score: Int): Boolean {
        if (score <= 0) return false
        val entries = getHighScores()
        return entries.size < MAX_ENTRIES || score > (entries.lastOrNull()?.score ?: 0)
    }

    private fun appendScoreHistory(score: Int, level: Int, lines: Int) {
        val timestamp = System.currentTimeMillis()
        val existing = getScoreHistory().toMutableList()
        existing.add(HighScoreEntry(score, level, lines, timestamp))
        existing.sortByDescending { it.timestamp }

        val history = existing.take(MAX_HISTORY_ENTRIES)
        val editor = prefs.edit()
        editor.putInt(HISTORY_COUNT_KEY, history.size)
        history.forEachIndexed { i, entry ->
            editor.putInt("history_score_$i", entry.score)
            editor.putInt("history_level_$i", entry.level)
            editor.putInt("history_lines_$i", entry.lines)
            editor.putLong("history_time_$i", entry.timestamp)
        }
        editor.apply()
    }

    companion object {
        const val MAX_ENTRIES = 5
        private const val HISTORY_COUNT_KEY = "history_count"
        private const val MAX_HISTORY_ENTRIES = 100
    }
}
