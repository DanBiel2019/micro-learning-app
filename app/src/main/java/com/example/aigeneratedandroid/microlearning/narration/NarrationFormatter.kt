package com.example.aigeneratedandroid.microlearning.narration

import com.example.aigeneratedandroid.microlearning.model.DailyFeed
import com.example.aigeneratedandroid.microlearning.model.IdeaCard
import com.example.aigeneratedandroid.microlearning.model.LearningStyle

/**
 * One spoken chunk of the feed (one card). [segments] are sentence-sized so playback can
 * pause and resume mid-card without restarting it.
 */
data class NarrationChunk(
    val cardId: String,
    val segments: List<String>
) {
    val wordCount: Int get() = segments.sumOf { it.split(Regex("\\s+")).count(String::isNotBlank) }

    /** At a relaxed ~150 words per minute, the pace used for the morning-coffee listen. */
    val estimatedSeconds: Int get() = (wordCount * 60 / WORDS_PER_MINUTE).coerceAtLeast(1)

    companion object {
        const val WORDS_PER_MINUTE = 150
    }
}

/**
 * Turns cards into podcast-style narration: an intro on the first chunk, the idea itself,
 * a bridge to the listener's own work (network engineering, observability, resilience), a
 * reflection prompt matching the card's style, the challenge, and a hand-off to the next card.
 */
object NarrationFormatter {

    fun format(feed: DailyFeed): List<NarrationChunk> =
        feed.cards.mapIndexed { i, card ->
            chunkFor(card, i, feed.cards.size, feed.theme, feed.cards.getOrNull(i + 1))
        }

    fun chunkFor(card: IdeaCard, index: Int, total: Int, theme: String, next: IdeaCard?): NarrationChunk {
        val parts = mutableListOf<String>()

        if (index == 0) {
            parts += "Good morning. Today's thread is: $theme."
            parts += "$total ideas, a couple of minutes each. Swipe to skip, tap to pause, and dig deeper on anything that sticks."
        }
        parts += "Idea ${index + 1} of $total. ${card.title}."
        parts += "This one comes from ${spokenSource(card)}."
        parts += sentences(card.insight)
        parts += sentences(bridgeFor(card))
        parts += reflectionFor(card.style)
        parts += "Your challenge for today. ${card.challenge}"
        parts += if (next != null) {
            "Next up: ${next.title}."
        } else {
            "That's the set for today. Pick one challenge, just one, and try it before lunch."
        }

        return NarrationChunk(card.id, parts.map(::cleanForSpeech).filter { it.isNotBlank() })
    }

    /** Splits on sentence boundaries; keeps each segment short enough to resume cleanly. */
    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

    private fun spokenSource(card: IdeaCard): String {
        val source = card.sourceName.replace(Regex("\\s*\\(.*?\\)"), "")
        val author = card.author.replace(Regex("\\s*\\(.*?\\)"), "")
        return if (source.contains(author, ignoreCase = true)) source else "$source, by $author"
    }

    /** Ties each topic back to the listener's day job. Picked deterministically per card. */
    private fun bridgeFor(card: IdeaCard): String {
        val options = BRIDGES[card.topic] ?: return ""
        return options[Math.floorMod(card.id.hashCode(), options.size)]
    }

    private fun reflectionFor(style: LearningStyle): String = when (style) {
        LearningStyle.COUNTERINTUITIVE -> "Sit with that for a second. What did you assume before you heard it?"
        LearningStyle.STORY -> "Think of a time you watched this exact story play out, maybe from the inside."
        LearningStyle.BIG_PICTURE -> "Zoom out. Where in your world is this pattern quietly running right now?"
        LearningStyle.PRACTICAL -> "Picture the very next moment this week where you could actually use it."
        LearningStyle.DATA_DRIVEN -> "Ask yourself which number in your world deserves this kind of agreement."
    }

    /** Strips characters TTS engines read literally or stumble over. */
    private fun cleanForSpeech(s: String): String = s
        .replace("∩", "and")
        .replace("&", "and")
        .replace(Regex("[\\[\\]{}<>*_#|]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val BRIDGES = mapOf(
        "Systems & Measurement" to listOf(
            "In observability work this shows up constantly. An alert count that jumps after you add new telemetry may mean you finally see the problem, not that it got worse.",
            "Think about your dashboards. Every graph encodes a decision about what matters, and the outage you miss is usually the one nobody chose to measure.",
            "Resilience engineering lives here. The interesting question after an incident is rarely what broke, it's why the system let it break quietly."
        ),
        "Technology & AI" to listOf(
            "On a network team, this is the gap between the change ticket and the change that actually happened at two in the morning.",
            "As AI tools land in operations, this matters more, not less. Automation amplifies whatever you specified, including the parts you specified badly.",
            "Map this onto your own change windows and on-call rotation, and see where the pattern is already costing you."
        ),
        "Business & Entrepreneurship" to listOf(
            "Infrastructure teams are tiny businesses inside bigger ones. Your internal customers vote with workarounds instead of wallets.",
            "Even if you never start a company, this is how leadership decides what your team gets funded to build next year."
        ),
        "Leadership" to listOf(
            "For a senior engineer, influence without authority is the job. Postmortems, design reviews, and vendor calls are all crucial conversations in disguise.",
            "The engineers people trust most during an outage are rarely the loudest. They're the ones who made it safe to say what's actually happening."
        ),
        "Creativity" to listOf(
            "Troubleshooting is a creative act. The fastest fixes often come from asking a question nobody on the bridge call thought to ask.",
            "Network design rewards borrowing from other fields. Queueing theory, epidemiology, and traffic engineering all solved your problems first."
        )
    )
}
