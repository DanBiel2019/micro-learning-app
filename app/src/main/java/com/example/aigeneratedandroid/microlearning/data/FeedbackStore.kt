package com.example.aigeneratedandroid.microlearning.data

/** What the listener did with one card. Drives the curation weights for future feeds. */
enum class Reaction { LIKED, SKIPPED, COMPLETED, DEEP_DIVED }

/** Per-card history: reaction counts plus the last day it was served (epoch day, -1 = never). */
data class CardStats(
    val likes: Int = 0,
    val skips: Int = 0,
    val completions: Int = 0,
    val deepDives: Int = 0,
    val lastShownEpochDay: Long = -1
) {
    /** Net signal: deep dives and likes count most, skips push the card (and its topic) down. */
    val score: Int
        get() = likes * 2 + deepDives * 2 + completions - skips * 2
}

/**
 * Persistence boundary for the feedback loop. Kept Android-free so ContentCurationEngine
 * can be unit tested; the app uses SharedPrefsFeedbackStore.
 */
interface FeedbackStore {
    fun stats(cardId: String): CardStats
    fun allStats(): Map<String, CardStats>
    fun record(cardId: String, reaction: Reaction)
    fun markShown(cardIds: List<String>, epochDay: Long)
}

class InMemoryFeedbackStore : FeedbackStore {
    private val data = mutableMapOf<String, CardStats>()

    override fun stats(cardId: String) = data[cardId] ?: CardStats()

    override fun allStats(): Map<String, CardStats> = data.toMap()

    override fun record(cardId: String, reaction: Reaction) {
        data[cardId] = stats(cardId).applying(reaction)
    }

    override fun markShown(cardIds: List<String>, epochDay: Long) {
        cardIds.forEach { data[it] = stats(it).copy(lastShownEpochDay = epochDay) }
    }
}

internal fun CardStats.applying(reaction: Reaction): CardStats = when (reaction) {
    Reaction.LIKED -> copy(likes = likes + 1)
    Reaction.SKIPPED -> copy(skips = skips + 1)
    Reaction.COMPLETED -> copy(completions = completions + 1)
    Reaction.DEEP_DIVED -> copy(deepDives = deepDives + 1)
}
