package com.example.aigeneratedandroid.microlearning.curation

import com.example.aigeneratedandroid.microlearning.data.CardStats
import com.example.aigeneratedandroid.microlearning.data.FeedbackStore
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.Segment
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import java.time.LocalDate
import kotlin.random.Random

/**
 * Builds an offline episode from the bundled library, used when today's studio episode
 * can't be downloaded.
 *
 * Selection is a weighted draw, seeded by the date so the same day always yields the same
 * episode. Weights combine:
 *  - topic affinity: profile topics, nudged up/down by accumulated feedback on that topic
 *  - style affinity: the profile's preferred styles
 *  - author affinity: authors the user already follows
 *  - novelty: segments served in the last [cooldownDays] days are heavily suppressed
 *  - per-segment feedback: segments skipped more than liked are dropped entirely
 *
 * The day has a lead topic (rotating, but biased toward topics the user engages with) that
 * opens the episode and sets its theme; the rest is spread across topics for variety.
 */
class ContentCurationEngine(
    private val library: List<Segment>,
    private val feedback: FeedbackStore,
    private val cooldownDays: Int = 14
) {

    fun buildEpisode(profile: UserProfile, date: LocalDate): Episode {
        val today = date.toEpochDay()
        val random = Random(today * 31 + profile.hashCode())
        val stats = feedback.allStats()

        val topicAffinity = topicAffinity(profile, stats)
        val eligible = library.filter { seg ->
            seg.topic in profile.topics && (stats[seg.id]?.score ?: 0) > -4
        }

        fun weight(seg: Segment): Double {
            val s = stats[seg.id]
            var w = topicAffinity[seg.topic] ?: 1.0
            if (seg.style in profile.preferredStyles) w *= 1.6
            if (profile.followedAuthors.any { seg.source.author.contains(it, ignoreCase = true) }) w *= 1.3
            if (s != null) {
                val daysSince = if (s.lastShownEpochDay < 0) Long.MAX_VALUE else today - s.lastShownEpochDay
                // Today's own episode isn't penalised, so rebuilding the same day stays stable.
                if (daysSince in 1 until cooldownDays) w *= 0.05
                w *= (1.0 + s.score * 0.15).coerceIn(0.2, 2.5)
            }
            return w
        }

        val leadTopic = pickLeadTopic(profile, topicAffinity, today, random)
        val picked = mutableListOf<Segment>()
        val pool = eligible.toMutableList()

        pool.filter { it.topic == leadTopic }.weightedPick(random, ::weight)?.let {
            picked += it
            pool -= it
        }

        val target = profile.segmentsPerSession.coerceAtMost(eligible.size)
        while (picked.size < target && pool.isNotEmpty()) {
            val topicCounts = picked.groupingBy { it.topic }.eachCount()
            val last = picked.lastOrNull()?.topic
            // Prefer a different topic than the previous segment and cap any topic at two per day.
            val candidates = pool.filter { it.topic != last && (topicCounts[it.topic] ?: 0) < 2 }
                .ifEmpty { pool }
            val next = candidates.weightedPick(random, ::weight) ?: break
            picked += next
            pool -= next
        }

        return Episode(
            id = "offline-$date",
            date = date.toString(),
            title = picked.firstOrNull()?.title ?: "Today's ideas",
            theme = THEMES[leadTopic] ?: leadTopic,
            segments = picked,
            offline = true
        )
    }

    /** Related segments for "go deeper": same author first, then same topic, then same style. */
    fun related(segment: Segment, exclude: Collection<String> = emptySet(), limit: Int = 3): List<Segment> =
        library.asSequence()
            .filter { it.id != segment.id && it.id !in exclude }
            .sortedByDescending {
                (if (it.source.author == segment.source.author) 4 else 0) +
                    (if (it.topic == segment.topic) 2 else 0) +
                    (if (it.style == segment.style) 1 else 0)
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
