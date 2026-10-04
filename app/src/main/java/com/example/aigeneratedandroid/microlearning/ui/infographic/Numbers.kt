package com.example.aigeneratedandroid.microlearning.ui.infographic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.VisualItem

/** 2-4 headline numbers in tiles. The number is the hero; only the punchline's is coloured. */
@Composable
fun StatsGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { col, item ->
                    val t = stagger(progress, row * 2 + col, items.size)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .enter(t)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (item.emphasis) ink.accentSoft else ink.card)
                            .padding(14.dp)
                    ) {
                        if (item.emoji.isNotBlank()) Text(item.emoji, fontSize = 20.sp)
                        val number = numberText(item) ?: ""
                        Text(
                            number,
                            style = NumberStyle.copy(fontSize = if (number.length <= 6) 32.sp else 24.sp, lineHeight = 38.sp),
                            color = if (item.emphasis) ink.accent else ink.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(item.label, style = MaterialTheme.typography.titleSmall, color = ink.text)
                        if (item.detail.isNotBlank()) {
                            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Horizontal bars on a shared zero baseline, labelled directly (name left, value right). */
@Composable
fun BarsGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val max = items.maxOfOrNull { it.value ?: 0.0 }?.takeIf { it > 0 } ?: 1.0
    val anyFocus = items.any { it.emphasis }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            val hot = item.emphasis || !anyFocus
            Column(Modifier.enter(t)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.emoji.isNotBlank()) {
                        Text(item.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(item.label, style = MaterialTheme.typography.titleSmall, color = ink.text, modifier = Modifier.weight(1f))
                    Text(
                        numberText(item) ?: "",
                        style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                        color = if (item.emphasis) ink.accent else ink.text
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ink.rail)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(((item.value ?: 0.0) / max).toFloat().coerceIn(0.015f, 1f) * t)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (hot) ink.accent else ink.mark)
                    )
                }
                if (item.detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                }
            }
        }
    }
}

/**
 * One striking number, set huge, with what it counts right underneath (annotate the
 * number, don't make people hunt for it). Further items add context in a quieter row.
 */
@Composable
fun BigNumberGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val heroIndex = items.indexOfFirst { it.emphasis && numberText(it) != null }.takeIf { it >= 0 }
        ?: items.indexOfFirst { numberText(it) != null }.coerceAtLeast(0)
    val hero = items[heroIndex]
    val rest = items.filterIndexed { i, _ -> i != heroIndex }.take(2)
    val number = numberText(hero) ?: ""
    val size = when {
        number.length <= 4 -> 76.sp
        number.length <= 7 -> 60.sp
        number.length <= 10 -> 44.sp
        else -> 34.sp
    }
    Column {
        Row(Modifier.fillMaxWidth().enter(progress), verticalAlignment = Alignment.CenterVertically) {
            Text(
                number,
                style = NumberStyle.copy(fontSize = size, lineHeight = size * 1.05f),
                color = ink.accent,
                modifier = Modifier.weight(1f)
            )
            if (hero.emoji.isNotBlank()) Text(hero.emoji, fontSize = 40.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(hero.label, style = MaterialTheme.typography.titleLarge, color = ink.text, modifier = Modifier.enter(progress))
        if (hero.detail.isNotBlank()) {
            Text(hero.detail, style = MaterialTheme.typography.bodyMedium, color = ink.muted, modifier = Modifier.enter(progress))
        }
        if (rest.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink.rail))
            rest.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp).enter(stagger(progress, i + 1, rest.size + 1)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        numberText(item) ?: item.emoji,
                        style = MaterialTheme.typography.titleLarge.merge(NumberStyle),
                        color = ink.text,
                        modifier = Modifier.width(96.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.titleSmall, color = ink.text)
                        if (item.detail.isNotBlank()) {
                            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A 10x10 grid where one square is 1%: people read shares more accurately by counting
 * squares than by comparing pie angles. The first part is the accent; later parts are greys.
 */
@Composable
fun WaffleGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val shares = waffleShares(items)
    val colors = listOf(ink.accent, ink.text.copy(alpha = 0.55f), ink.mark.copy(alpha = 0.6f))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(0.46f).aspectRatio(1f)) {
            val gap = size.width * 0.025f
            val cell = (size.width - gap * 9) / 10
            val filled = (100 * progress).toInt()
            var index = 0
            val owners = IntArray(100) { -1 }
            shares.forEachIndexed { s, (_, pct) -> repeat(pct) { if (index < 100) owners[index++] = s } }
            for (k in 0 until 100) {
                // Fill from the bottom-left, row by row, like a glass filling up.
                val row = 9 - k / 10
                val col = k % 10
                val owner = owners[k]
                val color: Color = if (owner >= 0 && k < filled) colors[owner] else ink.rail
                drawRoundRect(
                    color,
                    topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.22f)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(0.54f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shares.forEachIndexed { s, (item, pct) ->
                Column(Modifier.enter(stagger(progress, s, shares.size))) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colors[s]))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.valueLabel?.takeIf { it.isNotBlank() } ?: "$pct%",
                            style = (if (s == 0) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge).merge(NumberStyle),
                            color = if (s == 0) ink.accent else ink.text
                        )
                    }
                    Text(
                        if (item.emoji.isBlank()) item.label else "${item.emoji} ${item.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ink.text
                    )
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                    }
                }
            }
            Eyebrow("1 square = 1%", ink.muted)
        }
    }
}
