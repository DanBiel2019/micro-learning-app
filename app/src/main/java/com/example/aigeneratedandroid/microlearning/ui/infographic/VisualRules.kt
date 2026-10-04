package com.example.aigeneratedandroid.microlearning.ui.infographic

import com.example.aigeneratedandroid.microlearning.model.Visual
import com.example.aigeneratedandroid.microlearning.model.VisualItem
import com.example.aigeneratedandroid.microlearning.model.VisualKinds

/*
 * Plain rules (no Compose) shared by the renderer, the gallery and the unit tests.
 * Specs are written nightly by a small local model, so every kind has a shape it needs
 * (e.g. a matrix needs exactly four items). A spec that does not fit its kind is drawn as the
 * closest kind that does, instead of as a broken picture.
 */

/** The kind the renderer will actually draw for [visual]. */
fun resolveKind(visual: Visual): String {
    val items = visual.items
    val n = items.size
    val hasNumbers = items.any { numberText(it) != null }
    return when (visual.kind) {
        "matrix" -> if (n == 4) "matrix" else "compare"
        "before_after" -> if (n == 2) "before_after" else "compare"
        "iceberg" -> if (n in 2..6) "iceberg" else "flow"
        "funnel" -> if (n >= 2) "funnel" else "flow"
        "spectrum" -> if (n >= 2) "spectrum" else "flow"
        "waffle" -> if (waffleShares(items).isNotEmpty()) "waffle" else if (hasNumbers) "stats" else "compare"
        "big_number" -> if (hasNumbers) "big_number" else "quote"
        "stats" -> if (n == 1 && hasNumbers) "big_number" else "stats"
        "venn" -> if (n in 3..4) "venn" else "compare"
        "cycle" -> if (n in 3..6) "cycle" else "flow"
        "bars" -> if (items.any { (it.value ?: 0.0) > 0 }) "bars" else if (hasNumbers) "stats" else "flow"
        "ladder" -> if (n >= 2) "ladder" else "flow"
        "quote" -> if (n >= 1) "quote" else "flow"
        in VisualKinds.ALL -> visual.kind
        else -> "flow"
    }
}

/** The display text of an item's number: its valueLabel, or the formatted value. */
fun numberText(item: VisualItem): String? =
    item.valueLabel?.takeIf { it.isNotBlank() } ?: item.value?.let { formatNumber(it) }

fun formatNumber(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/** The highlighted (punchline) item, or null when none is marked. */
fun focusIndex(items: List<VisualItem>): Int? = items.indexOfFirst { it.emphasis }.takeIf { it >= 0 }

/**
 * Percent shares for a waffle (1 square = 1%), in item order. Accepts 0-100 values, 0-1
 * fractions, or a "70%" valueLabel. Items without a share are dropped; the total is capped at 100.
 */
fun waffleShares(items: List<VisualItem>): List<Pair<VisualItem, Int>> {
    val raw = items.map { item ->
        item to (item.value ?: item.valueLabel?.let { Regex("""-?\d+(\.\d+)?""").find(it)?.value?.toDoubleOrNull() })
    }.filter { (_, v) -> v != null && v > 0 }
    if (raw.isEmpty()) return emptyList()
    val fractions = raw.all { (_, v) -> v!! <= 1.0 }
    var left = 100
    return raw.take(3).map { (item, v) ->
        val pct = Math.round(if (fractions) v!! * 100 else v!!).toInt().coerceIn(0, left)
        left -= pct
        item to pct
    }.filter { it.second > 0 }
}

/** A spoken description of the whole graphic, for TalkBack. */
fun describe(visual: Visual): String = buildString {
    append(visual.title.trim().trimEnd('.')).append(". ")
    val kind = resolveKind(visual)
    when (kind) {
        "matrix" -> if (visual.xAxis.isNotBlank() || visual.yAxis.isNotBlank()) {
            append("Two by two: ${visual.yAxis} by ${visual.xAxis}. ")
        }
        "ladder" -> append("Levels, weakest first. ")
        "flow" -> append("Steps in order. ")
        "cycle" -> append("A repeating loop. ")
        "iceberg" -> append("What you see, then what's hidden. ")
        "before_after" -> append("Before, then after. ")
        "waffle" -> append("Shares of a hundred. ")
    }
    visual.items.forEachIndexed { i, item ->
        if (kind == "before_after") append(if (i == 0) "Before: " else "After: ")
        numberText(item)?.let { append(it).append(", ") }
        append(item.label.trim().trimEnd('.'))
        if (item.detail.isNotBlank()) append(": ").append(item.detail.trim().trimEnd('.'))
        if (item.emphasis) append(" (key point)")
        append(". ")
    }
    if (visual.caption.isNotBlank()) append(visual.caption.trim())
}.trim()
