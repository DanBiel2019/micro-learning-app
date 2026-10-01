package com.example.aigeneratedandroid.microlearning.curation

import com.example.aigeneratedandroid.microlearning.data.CardStats
import com.example.aigeneratedandroid.microlearning.data.FeedbackStore
import com.example.aigeneratedandroid.microlearning.model.DailyFeed
import com.example.aigeneratedandroid.microlearning.model.IdeaCard
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import java.time.LocalDate
import kotlin.random.Random

/**
 * Builds one day's feed from the content library.
 *
 * Selection is a weighted draw, seeded by the date so the same day always yields the same
 * feed (reopening the app doesn't reshuffle what you were listening to). Weights combine:
 *  - topic affinity: profile topics, nudged up/down by accumulated feedback on that topic
 *  - style affinity: the profile's preferred learning styles
 *  - author affinity: authors the user already follows
 *  - novelty: cards served in the last [cooldownDays] days are heavily suppressed
 *  - per-card feedback: cards skipped more than liked are dropped entirely
 *
 * The day has a lead topic (rotating, but biased toward topics the user engages with) that
 * opens the feed and sets its theme; the rest is spread across topics for variety.
 */
class ContentCurationEngine(
    private val library: List<IdeaCard>,
    private val feedback: FeedbackStore,
    private val cooldownDays: Int = 14
) {

    fun buildFeed(profile: UserProfile, date: LocalDate): DailyFeed {
        val today = date.toEpochDay()
        val random = Random(today * 31 + profile.hashCode())
        val stats = feedback.allStats()

        val topicAffinity = topicAffinity(profile, stats)
        val eligible = library.filter { card ->
            card.topic in profile.topics && (stats[card.id]?.score ?: 0) > -4
        }

        fun weight(card: IdeaCard): Double {
            val s = stats[card.id]
            var w = topicAffinity[card.topic] ?: 1.0
            if (card.style in profile.preferredStyles) w *= 1.6
            if (profile.followedAuthors.any { card.author.contains(it, ignoreCase = true) }) w *= 1.3
            if (s != null) {
                val daysSince = if (s.lastShownEpochDay < 0) Long.MAX_VALUE else today - s.lastShownEpochDay
                // Today's own feed isn't penalised, so rebuilding the same day stays stable.
                if (daysSince in 1 until cooldownDays) w *= 0.05
                w *= (1.0 + s.score * 0.15).coerceIn(0.2, 2.5)
            }
            return w
        }

        val leadTopic = pickLeadTopic(profile, topicAffinity, today, random)
        val picked = mutableListOf<IdeaCard>()
        val pool = eligible.toMutableList()

        pool.filter { it.topic == leadTopic }.weightedPick(random, ::weight)?.let {
            picked += it
            pool -= it
        }

        val target = profile.cardsPerSession.coerceAtMost(eligible.size)
        while (picked.size < target && pool.isNotEmpty()) {
            val topicCounts = picked.groupingBy { it.topic }.eachCount()
            val last = picked.lastOrNull()?.topic
            // Prefer a different topic than the previous card and cap any topic at two per day.
            val candidates = pool.filter { it.topic != last && (topicCounts[it.topic] ?: 0) < 2 }
                .ifEmpty { pool }
            val next = candidates.weightedPick(random, ::weight) ?: break
            picked += next
            pool -= next
        }

        return DailyFeed(
            dateIso = date.toString(),
            cards = picked,
            theme = themeFor(leadTopic),
            challenge = picked.firstOrNull()?.challenge.orEmpty()
        )
    }

    /** Related cards for "go deeper": same author first, then same topic, then same style. */
    fun related(card: IdeaCard, exclude: Collection<String> = emptySet(), limit: Int = 3): List<IdeaCard> =
        library.asSequence()
            .filter { it.id != card.id && it.id !in exclude }
            .sortedByDescending {
                (if (it.author == card.author) 4 else 0) +
                    (if (it.topic == card.topic) 2 else 0) +
                    (if (it.style == card.style) 1 else 0)
            }
            .take(limit)
            .toList()

    private fun topicAffinity(profile: UserProfile, stats: Map<String, CardStats>): Map<String, Double> {
        val byTopic = library.groupBy { it.topic }
        return profile.topics.associateWith { topic ->
            val net = byTopic[topic].orEmpty().sumOf { stats[it.id]?.score ?: 0 }
            (1.0 + net * 0.08).coerceIn(0.3, 2.5)
        }
    }

    private fun pickLeadTopic(
        profile: UserProfile,
        affinity: Map<String, Double>,
        epochDay: Long,
        random: Random
    ): String {
        // Rotate through topics day by day, but let strong preferences occasionally win the slot.
        val rotating = profile.topics[(epochDay % profile.topics.size).toInt()]
        val favourite = affinity.maxByOrNull { it.value }?.key ?: rotating
        return if (favourite != rotating && random.nextDouble() < 0.3) favourite else rotating
    }

    private fun themeFor(topic: String): String = THEMES[topic] ?: "Today: $topic"

    companion object {
        val THEMES = mapOf(
            "Systems & Measurement" to "What you count shapes what you see",
            "Technology & AI" to "Lessons the machines keep teaching us",
            "Business & Entrepreneurship" to "How good companies stumble, and great ones don't",
            "Leadership" to "The hard conversations that move teams",
            "Creativity" to "Better questions beat faster answers"
        )
    }
}

private fun <T> List<T>.weightedPick(random: Random, weight: (T) -> Double): T? {
    if (isEmpty()) return null
    val weights = map { weight(it).coerceAtLeast(0.0001) }
    var roll = random.nextDouble() * weights.sum()
    forEachIndexed { i, item ->
        roll -= weights[i]
        if (roll <= 0) return item
    }
    return last()
}
