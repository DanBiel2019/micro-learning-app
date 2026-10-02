package com.example.aigeneratedandroid.microlearning.model

import kotlinx.serialization.Serializable

/**
 * One day's listen. Mirrors pipeline/schema.py (PublishedEpisode) so the feed published to
 * GitHub Releases deserialises directly. Defaults keep older or bundled content readable.
 */
@Serializable
data class Episode(
    val schemaVersion: Int = 2,
    val id: String,
    val date: String,
    val title: String,
    val theme: String,
    val hosts: Map<String, String> = emptyMap(),
    val segments: List<Segment>,
    val totalDurationMs: Long = 0,
    val previous: List<PreviousEpisode> = emptyList(),
    /** True when built on-device from the bundled library (no studio audio). */
    val offline: Boolean = false
) {
    val hasAudio: Boolean get() = segments.isNotEmpty() && segments.all { it.audioUrl != null }

    fun hostName(speaker: String): String =
        hosts[speaker] ?: if (speaker == "host") "Maya" else "Theo"
}

@Serializable
data class Segment(
    val id: String,
    val topic: String,
    val adjacent: Boolean = false,
    val style: String = "story",
    val kicker: String = "",
    val title: String,
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val takeaway: String = "",
    val challenge: String = "",
    val source: Source,
    val visual: Visual? = null,
    val script: List<Line> = emptyList(),
    val deeperQuestions: List<String> = emptyList(),
    val furtherReading: List<Reading> = emptyList(),
    val audioUrl: String? = null,
    val durationMs: Long? = null,
    /** Script lines with their start offset in [audioUrl], for transcript highlighting. */
    val lines: List<TimedLine> = emptyList()
)

@Serializable
data class Source(
    val title: String,
    val author: String,
    val year: Int? = null,
    val url: String? = null,
    val format: String = "book"
)

@Serializable
data class Visual(
    val kind: String,
    val title: String,
    val items: List<VisualItem>,
    val caption: String = ""
)

@Serializable
data class VisualItem(
    val label: String,
    val detail: String = "",
    val emoji: String = "",
    val value: Double? = null,
    val valueLabel: String? = null,
    val emphasis: Boolean = false
)

@Serializable
data class Line(val speaker: String, val text: String)

@Serializable
data class TimedLine(val speaker: String, val text: String, val startMs: Long)

@Serializable
data class Reading(val title: String, val url: String? = null, val note: String = "")

@Serializable
data class PreviousEpisode(val id: String, val date: String, val title: String, val feedUrl: String)
