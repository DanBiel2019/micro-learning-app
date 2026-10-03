package com.example.aigeneratedandroid.microlearning.data

import android.content.Context
import java.time.LocalDate

/** Small, private listening history used for the streak and "ideas heard" stats. */
data class ListeningStats(
    /** Consecutive days (ending today or yesterday) with at least one finished idea. */
    val streakDays: Int = 0,
    val ideasHeard: Int = 0,
    val minutesHeard: Int = 0,
    val heardToday: Boolean = false
)

class ListeningLog(context: Context) {

    private val prefs = context.getSharedPreferences("microlearning_listening", Context.MODE_PRIVATE)

    fun recordCompletion(durationMs: Long?, today: LocalDate = LocalDate.now()) {
        val days = prefs.getStringSet(KEY_DAYS, emptySet()).orEmpty() + today.toEpochDay().toString()
        prefs.edit()
            .putStringSet(KEY_DAYS, days.toSet())
            .putInt(KEY_IDEAS, prefs.getInt(KEY_IDEAS, 0) + 1)
            .putLong(KEY_MS, prefs.getLong(KEY_MS, 0) + (durationMs ?: 0L).coerceAtLeast(0))
            .apply()
    }

    fun stats(today: LocalDate = LocalDate.now()): ListeningStats {
        val days = prefs.getStringSet(KEY_DAYS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        return ListeningStats(
            streakDays = streak(days, today.toEpochDay()),
            ideasHeard = prefs.getInt(KEY_IDEAS, 0),
            minutesHeard = (prefs.getLong(KEY_MS, 0) / 60_000).toInt(),
            heardToday = today.toEpochDay() in days
        )
    }

    companion object {
        private const val KEY_DAYS = "days"
        private const val KEY_IDEAS = "ideas"
        private const val KEY_MS = "ms"

        /** A streak survives until the end of the day after the last listen. */
        fun streak(days: Set<Long>, today: Long): Int {
            var day = if (today in days) today else today - 1
            var n = 0
            while (day in days) {
                n++
                day--
            }
            return n
        }
    }
}
