package com.example.aigeneratedandroid.microlearning.narration

import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.Segment

/**
 * Spoken text for one segment when there's no studio audio (offline episodes read by the
 * device's own TTS). [segments] are sentence-sized so playback can pause and resume mid-way.
 */
data class NarrationChunk(
    val segmentId: String,
    val segments: List<String>
) {
    val wordCount: Int get() = segments.sumOf { it.split(Regex("\\s+")).count(String::isNotBlank) }

    val estimatedSeconds: Int get() = (wordCount * 60 / WORDS_PER_MINUTE).coerceAtLeast(1)

    companion object {
        const val WORDS_PER_MINUTE = 150
    }
}

/**
 * Turns a segment into narration: an intro on the first one, the idea, a bridge to the
 * listener's work (network engineering, observability, resilience), a reflection prompt
 * matching the segment's style, the challenge, and a hand-off to the next segment.
 * Segments that already have a written two-host script are read from that script instead.
 */
object NarrationFormatter {

    fun format(episode: Episode): List<NarrationChunk> =
        episode.segments.mapIndexed { i, seg ->
            chunkFor(seg, i, episode.segments.size, episode.theme, episode.segments.getOrNull(i + 1))
        }

    fun chunkFor(seg: Segment, index: Int, total: Int, theme: String, next: Segment?): NarrationChunk {
        if (seg.script.isNotEmpty()) {
            return NarrationChunk(seg.id, seg.script.map { cleanForSpeech(it.text) })
        }
        val parts = mutableListOf<String>()

        if (index == 0) {
            parts += "Good morning. Today's thread is: $theme."
            parts += "$total ideas, a couple of minutes each. Swipe to skip, tap to pause, and dig deeper on anything that sticks."
        }
        parts += "Idea ${index + 1} of $total. ${seg.title}."
        parts += "This one comes from ${spokenSource(seg)}."
        parts += sentences(seg.summary)
        parts += sentences(bridgeFor(seg))
        parts += reflectionFor(seg.style)
        parts += "Your challenge for today. ${seg.challenge}"
        parts += if (next != null) {
            "Next up: ${next.title}."
        } else {
            "That's the set for today. Pick one challenge, just one, and try it before lunch."
        }

        return NarrationChunk(seg.id, parts.map(::cleanForSpeech).filter { it.isNotBlank() })
    }

    /** Splits on sentence boundaries; keeps each segment short enough to resume cleanly. */
    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

    private fun spokenSource(seg: Segment): String {
        val source = seg.source.title.replace(Regex("\\s*\\(.*?\\)"), "")
        val author = seg.source.author.replace(Regex("\\s*\\(.*?\\)"), "")
        return if (source.contains(author, ignoreCase = true)) source else "$source, by $author"
    }

    /** Ties each topic back to the listener's day job. Picked deterministically per segment. */
    private fun bridgeFor(seg: Segment): String {
        val options = BRIDGES[seg.topic] ?: return ""
        return options[Math.floorMod(seg.id.hashCode(), options.size)]
    }

    private fun reflectionFor(style: String): String = when (style) {
        "counterintuitive" -> "Sit with that for a second. What did you assume before you heard it?"
        "story" -> "Think of a time you watched this exact story play out, maybe from the inside."
        "big_picture" -> "Zoom out. Where in your world is this pattern quietly running right now?"
        "practical" -> "Picture the very next moment this week where you could actually use it."
        else -> "Ask yourself which number in your world deserves this kind of agreement."
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
