package com.example.aigeneratedandroid.microlearning.data

import android.content.Context
import com.example.aigeneratedandroid.microlearning.model.DailyFeed

/**
 * Remembers today's feed (card order, theme, and listening position) so reopening the app
 * mid-coffee lands you where you left off instead of re-curating with fresh feedback.
 */
class FeedCache(context: Context) {

    private val prefs = context.getSharedPreferences("microlearning_feed", Context.MODE_PRIVATE)

    fun load(dateIso: String): DailyFeed? {
        if (prefs.getString(KEY_DATE, null) != dateIso) return null
        val cards = prefs.getString(KEY_IDS, "").orEmpty()
            .split(",").filter { it.isNotBlank() }
            .mapNotNull(ContentBank::byId)
        if (cards.isEmpty()) return null
        return DailyFeed(
            dateIso = dateIso,
            cards = cards,
            theme = prefs.getString(KEY_THEME, "").orEmpty(),
            challenge = prefs.getString(KEY_CHALLENGE, "").orEmpty()
        )
    }

    fun save(feed: DailyFeed) {
        prefs.edit()
            .putString(KEY_DATE, feed.dateIso)
            .putString(KEY_IDS, feed.cards.joinToString(",") { it.id })
            .putString(KEY_THEME, feed.theme)
            .putString(KEY_CHALLENGE, feed.challenge)
            .apply()
    }

    var position: Int
        get() = prefs.getInt(KEY_POSITION, 0)
        set(value) = prefs.edit().putInt(KEY_POSITION, value).apply()

    private companion object {
        const val KEY_DATE = "date"
        const val KEY_IDS = "ids"
        const val KEY_THEME = "theme"
        const val KEY_CHALLENGE = "challenge"
        const val KEY_POSITION = "position"
    }
}
