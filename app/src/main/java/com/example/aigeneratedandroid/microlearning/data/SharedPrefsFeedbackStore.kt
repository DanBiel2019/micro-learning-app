package com.example.aigeneratedandroid.microlearning.data

import android.content.Context
import org.json.JSONObject

/** Stores each card's [CardStats] as a small JSON blob keyed by card id. */
class SharedPrefsFeedbackStore(context: Context) : FeedbackStore {

    private val prefs = context.getSharedPreferences("microlearning_feedback", Context.MODE_PRIVATE)

    override fun stats(cardId: String): CardStats =
        prefs.getString(cardId, null)?.let(::decode) ?: CardStats()

    override fun allStats(): Map<String, CardStats> =
        prefs.all.mapNotNull { (id, raw) -> (raw as? String)?.let { id to decode(it) } }.toMap()

    override fun record(cardId: String, reaction: Reaction) {
        save(cardId, stats(cardId).applying(reaction))
    }

    override fun markShown(cardIds: List<String>, epochDay: Long) {
        cardIds.forEach { save(it, stats(it).copy(lastShownEpochDay = epochDay)) }
    }

    private fun save(cardId: String, s: CardStats) {
        val json = JSONObject()
            .put("likes", s.likes)
            .put("skips", s.skips)
            .put("completions", s.completions)
            .put("deepDives", s.deepDives)
            .put("lastShown", s.lastShownEpochDay)
        prefs.edit().putString(cardId, json.toString()).apply()
    }

    private fun decode(raw: String): CardStats = runCatching {
        val j = JSONObject(raw)
        CardStats(
            likes = j.optInt("likes"),
            skips = j.optInt("skips"),
            completions = j.optInt("completions"),
            deepDives = j.optInt("deepDives"),
            lastShownEpochDay = j.optLong("lastShown", -1)
        )
    }.getOrDefault(CardStats())
}
