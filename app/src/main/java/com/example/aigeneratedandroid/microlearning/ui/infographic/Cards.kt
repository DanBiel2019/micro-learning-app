package com.example.aigeneratedandroid.microlearning.ui.infographic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.VisualItem
import com.example.aigeneratedandroid.microlearning.ui.theme.Display

@Composable
private fun CompareCard(item: VisualItem, ink: Ink, t: Float, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .enter(t)
            .clip(shape)
            .background(if (item.emphasis) ink.accentSoft else ink.card)
            .then(if (item.emphasis) Modifier.border(1.5.dp, ink.accent, shape) else Modifier)
            .padding(14.dp)
    ) {
        if (item.emoji.isNotBlank()) {
            Text(item.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
        }
        Text(item.label, style = MaterialTheme.typography.titleMedium, color = if (item.emphasis) ink.accent else ink.text)
        numberText(item)?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = if (item.emphasis) ink.onAccent else ink.surface,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (item.emphasis) ink.accent else ink.muted)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        if (item.detail.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
        }
    }
}

/** Side-by-side options; the recommended one carries the accent. */
@Composable
fun CompareGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    when {
        items.size == 2 -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
            CompareCard(items[0], ink, stagger(progress, 0, 2), Modifier.weight(1f).fillMaxHeight())
            Text(
                "vs",
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = Display, fontStyle = FontStyle.Italic),
                color = ink.muted,
                modifier = Modifier.padding(horizontal = 8.dp).alpha(progress)
            )
            CompareCard(items[1], ink, stagger(progress, 1, 2), Modifier.weight(1f).fillMaxHeight())
        }
        items.size == 3 -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { i, item ->
                CompareCard(item, ink, stagger(progress, i, 3), Modifier.weight(1f).fillMaxHeight())
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEachIndexed { col, item ->
                        CompareCard(item, ink, stagger(progress, row * 2 + col, items.size), Modifier.weight(1f).fillMaxHeight())
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * The social-media "before / after" split: the old way muted on the left, the new way in the
 * accent on the right, an arrow between. Optional numbers make the change concrete.
 */
@Composable
fun BeforeAfterGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val before = items[0]
    val after = items[1]
    Box {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftPanel(before, "Before", ink, false, stagger(progress, 0, 2), Modifier.weight(1f).fillMaxHeight())
            ShiftPanel(after, "After", ink, true, stagger(progress, 1, 2), Modifier.weight(1f).fillMaxHeight())
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .alpha(progress)
                .size(32.dp)
                .clip(CircleShape)
                .background(ink.surface)
                .padding(3.dp)
                .clip(CircleShape)
                .background(ink.accent),
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = ink.onAccent, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ShiftPanel(item: VisualItem, tag: String, ink: Ink, isAfter: Boolean, t: Float, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .enter(t)
            .clip(shape)
            .background(if (isAfter) ink.accentSoft else ink.card)
            .then(if (isAfter) Modifier.border(1.5.dp, ink.accent, shape) else Modifier)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Eyebrow(tag, if (isAfter) ink.accent else ink.muted)
        Spacer(Modifier.height(8.dp))
        if (item.emoji.isNotBlank()) Text(item.emoji, fontSize = 28.sp)
        numberText(item)?.let {
            Text(
                it,
                style = NumberStyle.copy(fontSize = if (it.length <= 7) 26.sp else 20.sp, lineHeight = 32.sp),
                color = if (isAfter) ink.accent else ink.muted
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (isAfter) ink.accent else ink.text
        )
        if (item.detail.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
        }
    }
}

/** A pull quote: oversized accent quote mark, the words in the serif, attribution beneath. */
@Composable
fun QuoteGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val item = items.firstOrNull() ?: return
    Column(Modifier.enter(progress)) {
        run {
            Text("“", style = NumberStyle.copy(fontSize = 72.sp, lineHeight = 52.sp), color = ink.accent)
            Text(
                item.label,
                style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                color = ink.text
            )
            if (item.detail.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(20.dp).height(2.dp).background(ink.accent))
                    Spacer(Modifier.width(8.dp))
                    Text(item.detail, style = MaterialTheme.typography.titleSmall, color = ink.muted)
                }
            }
        }
    }
}
