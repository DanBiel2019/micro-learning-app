package com.example.aigeneratedandroid.microlearning.ui.infographic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.VisualItem

/** Numbered steps on a rail, cause to effect; the punchline step is the only coloured one. */
@Composable
fun FlowGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    Column {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(t)) {
                Box(Modifier.width(40.dp).fillMaxHeight()) {
                    if (i < items.lastIndex) {
                        Canvas(Modifier.fillMaxSize()) {
                            val x = size.width / 2
                            val top = 40.dp.toPx() + 4.dp.toPx()
                            val bottom = size.height - 4.dp.toPx()
                            if (bottom > top) {
                                drawLine(ink.mark, Offset(x, top), Offset(x, bottom), 2.dp.toPx(), StrokeCap.Round)
                                val head = Path().apply {
                                    moveTo(x - 5.dp.toPx(), bottom - 6.dp.toPx()); lineTo(x, bottom); lineTo(x + 5.dp.toPx(), bottom - 6.dp.toPx())
                                }
                                drawPath(head, ink.mark, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                            }
                        }
                    }
                    EmojiToken(item.emoji, ink, item.emphasis, fallback = "${i + 1}")
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(bottom = if (i < items.lastIndex) 18.dp else 0.dp)) {
                    Eyebrow("Step ${i + 1}", if (item.emphasis) ink.accent else ink.muted)
                    Text(
                        item.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.emphasis) ink.accent else ink.text
                    )
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                    }
                }
            }
        }
    }
}

/** Dated events on a vertical line, the date set in the serif as the reading anchor. */
@Composable
fun TimelineGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    Column {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(t)) {
                Text(
                    numberText(item) ?: "",
                    style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                    color = if (item.emphasis) ink.accent else ink.text,
                    modifier = Modifier.width(60.dp).padding(top = 1.dp),
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
                Box(Modifier.width(30.dp).fillMaxHeight()) {
                    Canvas(Modifier.fillMaxSize()) {
                        val x = size.width / 2
                        val cy = 12.dp.toPx()
                        if (i < items.lastIndex) {
                            drawLine(ink.rail, Offset(x, cy), Offset(x, size.height + cy), 2.dp.toPx())
                        }
                        if (item.emphasis) {
                            drawCircle(ink.accentSoft, 10.dp.toPx(), Offset(x, cy))
                            drawCircle(ink.accent, 6.dp.toPx(), Offset(x, cy))
                        } else {
                            drawCircle(ink.surface, 6.dp.toPx(), Offset(x, cy))
                            drawCircle(ink.mark, 5.dp.toPx(), Offset(x, cy), style = Stroke(2.dp.toPx()))
                        }
                    }
                }
                Column(Modifier.weight(1f).padding(bottom = if (i < items.lastIndex) 18.dp else 0.dp)) {
                    Text(
                        if (item.emoji.isBlank()) item.label else "${item.emoji}  ${item.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.emphasis) ink.accent else ink.text
                    )
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                    }
                }
            }
        }
    }
}

/**
 * Levels stacked strongest on top (the list arrives weakest first). Each step widens and
 * deepens in tint, so "higher = stronger" reads without a legend; [direction] labels the axis.
 */
@Composable
fun LadderGraphic(items: List<VisualItem>, ink: Ink, progress: Float, direction: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow("▲ " + direction.ifBlank { "Stronger" }, ink.muted, Modifier.padding(bottom = 2.dp))
        items.withIndex().reversed().forEach { (i, item) ->
            val t = stagger(progress, items.size - 1 - i, items.size)
            val fraction = 0.6f + 0.4f * (i + 1) / items.size
            val fill = when {
                item.emphasis -> ink.accent
                else -> ink.accent.copy(alpha = 0.06f + 0.16f * (i + 1) / items.size)
            }
            val onFill = if (item.emphasis) ink.onAccent else ink.text
            Row(
                Modifier
                    .fillMaxWidth(fraction)
                    .enter(t)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fill)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.emoji.isNotBlank()) {
                    Text(item.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(item.label, style = MaterialTheme.typography.titleSmall, color = onFill)
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = onFill.copy(alpha = 0.78f))
                    }
                }
            }
        }
    }
}

/**
 * Stages that narrow, widest first. With numbers, each stage's width is proportional to its
 * value (honest scale); without, the funnel narrows evenly. Labels sit beside the shape.
 */
@Composable
fun FunnelGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val values = items.map { it.value }
    val proportional = values.all { it != null && it > 0 }
    val max = if (proportional) values.maxOf { it!! } else 1.0
    val widths = items.indices.map { i ->
        if (proportional) (values[i]!! / max).toFloat().coerceIn(0.14f, 1f)
        else 1f - 0.55f * i / (items.size - 1).coerceAtLeast(1)
    }
    Column {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            val top = widths[i]
            val bottom = widths.getOrNull(i + 1) ?: (top * 0.82f)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(t), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                        .heightIn(min = 54.dp)
                        .drawBehind {
                            val w = size.width
                            val gap = 3.dp.toPx()
                            val path = Path().apply {
                                moveTo(w * (1 - top) / 2, gap / 2)
                                lineTo(w * (1 + top) / 2, gap / 2)
                                lineTo(w * (1 + bottom) / 2, size.height - gap / 2)
                                lineTo(w * (1 - bottom) / 2, size.height - gap / 2)
                                close()
                            }
                            drawPath(path, if (item.emphasis) ink.accent else ink.accent.copy(alpha = 0.10f + 0.10f * (items.size - i) / items.size))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val number = numberText(item)
                    Text(
                        number ?: item.emoji,
                        style = if (number != null) MaterialTheme.typography.titleMedium.merge(NumberStyle) else MaterialTheme.typography.titleMedium,
                        color = if (item.emphasis) ink.onAccent else ink.text,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(0.56f).padding(vertical = 6.dp)) {
                    Text(
                        if (numberText(item) != null && item.emoji.isNotBlank()) "${item.emoji}  ${item.label}" else item.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.emphasis) ink.accent else ink.text
                    )
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                    }
                }
            }
        }
    }
}

/**
 * Positions between two extremes. Up to three fit side by side under one track (the classic
 * "scale" graphic); more are laid out down a vertical track so labels stay readable on a phone.
 */
@Composable
fun SpectrumGraphic(items: List<VisualItem>, ink: Ink, progress: Float, scale: String) {
    if (items.size <= 3) {
        Column {
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val cy = size.height / 2
                val n = items.size
                val firstX = size.width / (2 * n)
                val lastX = size.width - firstX
                val h = 8.dp.toPx()
                val end = firstX + (lastX - firstX) * progress
                drawRoundRect(ink.rail, Offset(firstX - h, cy - h / 2), Size(lastX - firstX + 2 * h, h), CornerRadius(h / 2))
                drawRoundRect(ink.mark.copy(alpha = 0.5f), Offset(firstX - h, cy - h / 2), Size(end - firstX + 2 * h, h), CornerRadius(h / 2))
                items.forEachIndexed { i, item ->
                    val x = size.width * (i + 0.5f) / n
                    if (x <= end + 1f) {
                        if (item.emphasis) {
                            drawCircle(ink.accentSoft, 14.dp.toPx(), Offset(x, cy))
                            drawCircle(ink.accent, 9.dp.toPx(), Offset(x, cy))
                        } else {
                            drawCircle(ink.surface, 8.dp.toPx(), Offset(x, cy))
                            drawCircle(ink.mark, 7.dp.toPx(), Offset(x, cy), style = Stroke(2.5f.dp.toPx()))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { i, item ->
                    Column(
                        Modifier.weight(1f).enter(stagger(progress, i, items.size)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (item.emoji.isNotBlank()) Text(item.emoji, fontSize = 24.sp)
                        Text(
                            item.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (item.emphasis) ink.accent else ink.text,
                            textAlign = TextAlign.Center
                        )
                        if (item.detail.isNotBlank()) {
                            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (scale.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Eyebrow("$scale  →", ink.muted, Modifier.align(Alignment.End))
            }
        }
    } else {
        Column {
            if (scale.isNotBlank()) Eyebrow("$scale  ↓", ink.muted, Modifier.padding(bottom = 8.dp))
            items.forEachIndexed { i, item ->
                val t = stagger(progress, i, items.size)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(t)) {
                    Box(Modifier.width(28.dp).fillMaxHeight()) {
                        Canvas(Modifier.fillMaxSize()) {
                            val x = size.width / 2
                            val cy = 11.dp.toPx()
                            val w = 8.dp.toPx()
                            val top = if (i == 0) cy - w / 2 else 0f
                            val bottom = if (i == items.lastIndex) cy + w / 2 else size.height
                            drawRoundRect(ink.rail, Offset(x - w / 2, top), Size(w, bottom - top), CornerRadius(if (i == 0 || i == items.lastIndex) w / 2 else 0f))
                            if (item.emphasis) {
                                drawCircle(ink.accentSoft, 13.dp.toPx(), Offset(x, cy))
                                drawCircle(ink.accent, 8.dp.toPx(), Offset(x, cy))
                            } else {
                                drawCircle(ink.surface, 7.dp.toPx(), Offset(x, cy))
                                drawCircle(ink.mark, 6.dp.toPx(), Offset(x, cy), style = Stroke(2.5f.dp.toPx()))
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).padding(bottom = if (i < items.lastIndex) 14.dp else 0.dp)) {
                        Text(
                            if (item.emoji.isBlank()) item.label else "${item.emoji}  ${item.label}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (item.emphasis) ink.accent else ink.text
                        )
                        if (item.detail.isNotBlank()) {
                            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                        }
                    }
                }
            }
        }
    }
}
