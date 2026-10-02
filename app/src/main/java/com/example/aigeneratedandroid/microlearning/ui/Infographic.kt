package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.Visual
import com.example.aigeneratedandroid.microlearning.model.VisualItem
import com.example.aigeneratedandroid.microlearning.ui.theme.Display
import com.example.aigeneratedandroid.microlearning.ui.theme.TopicPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a segment's infographic from its [Visual] spec. Specs come from the content pipeline,
 * so every new episode gets illustrations without shipping new artwork.
 */
@Composable
fun Infographic(visual: Visual, palette: TopicPalette, modifier: Modifier = Modifier) {
    val reveal = remember(visual) { Animatable(0f) }
    LaunchedEffect(visual) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                visual.title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            val items = visual.items
            val p = reveal.value
            when (visual.kind) {
                "flow" -> FlowGraphic(items, palette, p)
                "cycle" -> CycleGraphic(items, palette, p)
                "compare" -> CompareGraphic(items, palette, p)
                "stats" -> StatsGraphic(items, palette, p)
                "bars" -> BarsGraphic(items, palette, p)
                "venn" -> VennGraphic(items, palette, p)
                "ladder" -> LadderGraphic(items, palette, p)
                "timeline" -> TimelineGraphic(items, palette, p)
                "quote" -> QuoteGraphic(items, palette, p)
                else -> FlowGraphic(items, palette, p)
            }
            if (visual.caption.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    visual.caption,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Display, fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Stagger helper: item [i] of [n] fades/slides in as [progress] goes 0 -> 1. */
private fun stagger(progress: Float, i: Int, n: Int): Float {
    val start = i.toFloat() / (n + 1)
    return ((progress - start) / (1f - start).coerceAtLeast(0.01f) * 1.6f).coerceIn(0f, 1f)
}

private fun Modifier.enter(t: Float) = this.graphicsLayer {
    alpha = t
    translationY = (1f - t) * 24f
}

@Composable
private fun EmojiBubble(emoji: String, palette: TopicPalette, emphasis: Boolean, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (emphasis) palette.accent else palette.soft),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
private fun FlowGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    Column {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            Row(
                Modifier
                    .fillMaxWidth()
                    .enter(t)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (item.emphasis) palette.soft else Color.Transparent)
                    .then(if (item.emphasis) Modifier.border(1.5.dp, palette.accent, RoundedCornerShape(16.dp)) else Modifier)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiBubble(item.emoji, palette, item.emphasis)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    "${i + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.outline
                )
            }
            if (i < items.lastIndex) {
                Canvas(
                    Modifier
                        .padding(start = 31.dp)
                        .size(width = 12.dp, height = 22.dp)
                        .alpha(stagger(progress, i + 1, items.size))
                ) {
                    val x = size.width / 2
                    drawLine(palette.accent.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height - 6f), 2.5f, StrokeCap.Round)
                    val head = Path().apply {
                        moveTo(x - 9f, size.height - 12f); lineTo(x, size.height); lineTo(x + 9f, size.height - 12f)
                    }
                    drawPath(head, palette.accent.copy(alpha = 0.6f), style = Stroke(2.5f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

@Composable
private fun CycleGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    val ring = palette.accent
    val focus = items.firstOrNull { it.emphasis } ?: items.last()
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val w = constraints.maxWidth.toFloat()
        val center = Offset(w / 2, w / 2)
        val radius = w * 0.34f
        val n = items.size
        Canvas(Modifier.fillMaxSize()) {
            val sweepEach = 360f / n
            for (i in 0 until n) {
                val t = stagger(progress, i, n)
                val start = -90f + i * sweepEach + 16f
                val sweep = (sweepEach - 32f) * t
                drawArc(
                    ring.copy(alpha = 0.55f), start, sweep, false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(5f, cap = StrokeCap.Round)
                )
                if (t > 0.95f) {
                    val endAngle = Math.toRadians((start + sweep).toDouble())
                    val tip = Offset(center.x + radius * cos(endAngle).toFloat(), center.y + radius * sin(endAngle).toFloat())
                    val tangent = endAngle + Math.PI / 2
                    val back = Offset(tip.x - 14f * cos(tangent).toFloat(), tip.y - 14f * sin(tangent).toFloat())
                    val perp = Offset(-sin(tangent).toFloat(), cos(tangent).toFloat())
                    drawLine(ring.copy(alpha = 0.7f), tip, Offset(back.x + perp.x * 9f, back.y + perp.y * 9f), 5f, StrokeCap.Round)
                    drawLine(ring.copy(alpha = 0.7f), tip, Offset(back.x - perp.x * 9f, back.y - perp.y * 9f), 5f, StrokeCap.Round)
                }
            }
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val nodeWidth = 104.dp
        items.forEachIndexed { i, item ->
            val angle = Math.toRadians(-90.0 + i * 360.0 / n)
            val px = center.x + radius * cos(angle).toFloat()
            val py = center.y + radius * sin(angle).toFloat()
            with(density) {
                Column(
                    Modifier
                        .offset(x = px.toDp() - nodeWidth / 2, y = py.toDp() - 22.dp)
                        .width(nodeWidth)
                        .alpha(stagger(progress, i, n)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmojiBubble(item.emoji, palette, item.emphasis)
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 150.dp).alpha(progress),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(focus.emoji, fontSize = 20.sp)
            Text(focus.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CompareCard(item: VisualItem, palette: TopicPalette, t: Float, modifier: Modifier) {
    Column(
        modifier
            .enter(t)
            .clip(RoundedCornerShape(18.dp))
            .background(if (item.emphasis) palette.soft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .then(if (item.emphasis) Modifier.border(1.5.dp, palette.accent, RoundedCornerShape(18.dp)) else Modifier)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(item.emoji, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text(item.label, style = MaterialTheme.typography.titleMedium, color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.onSurface)
        item.valueLabel?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = if (item.emphasis) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (item.emphasis) palette.accent else MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        if (item.detail.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompareGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    when {
        items.size == 2 -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
            CompareCard(items[0], palette, stagger(progress, 0, 2), Modifier.weight(1f).fillMaxHeight())
            Text(
                "vs",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = Display, fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp).alpha(progress)
            )
            CompareCard(items[1], palette, stagger(progress, 1, 2), Modifier.weight(1f).fillMaxHeight())
        }
        items.size == 3 -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { i, item ->
                CompareCard(item, palette, stagger(progress, i, 3), Modifier.weight(1f).fillMaxHeight())
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEachIndexed { col, item ->
                        CompareCard(item, palette, stagger(progress, row * 2 + col, items.size), Modifier.weight(1f).fillMaxHeight())
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatsGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
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
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (item.emphasis) palette.soft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(14.dp)
                    ) {
                        Text(item.emoji, fontSize = 20.sp)
                        Text(
                            item.valueLabel ?: item.value?.let { formatNumber(it) } ?: "",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                            color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(item.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        if (item.detail.isNotBlank()) {
                            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BarsGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    val max = items.maxOfOrNull { it.value ?: 0.0 }?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            Column(Modifier.enter(t)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(
                        item.valueLabel ?: formatNumber(item.value ?: 0.0),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Display),
                        color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(((item.value ?: 0.0) / max).toFloat().coerceIn(0.02f, 1f) * t)
                            .clip(RoundedCornerShape(50))
                            .background(if (item.emphasis) palette.accent else palette.accent.copy(alpha = 0.35f))
                    )
                }
                if (item.detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun VennGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    val circles = items.dropLast(1).take(3)
    val overlap = items.lastOrNull() ?: return
    val tints = listOf(palette.accent, palette.accent.copy(red = palette.accent.blue, blue = palette.accent.red), palette.accent.copy(green = palette.accent.red))
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(if (circles.size == 3) 1f else 1.5f)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val r = if (circles.size == 3) w * 0.27f else h * 0.40f
        val centers = if (circles.size == 3) {
            listOf(Offset(w * 0.37f, h * 0.38f), Offset(w * 0.63f, h * 0.38f), Offset(w * 0.5f, h * 0.62f))
        } else {
            listOf(Offset(w * 0.37f, h * 0.5f), Offset(w * 0.63f, h * 0.5f))
        }
        val middle = Offset(centers.map { it.x }.average().toFloat(), centers.map { it.y }.average().toFloat())
        Canvas(Modifier.fillMaxSize()) {
            centers.forEachIndexed { i, c ->
                val t = stagger(progress, i, centers.size)
                drawCircle(tints[i].copy(alpha = 0.16f * t), r * (0.85f + 0.15f * t), c)
                drawCircle(tints[i].copy(alpha = 0.55f * t), r * (0.85f + 0.15f * t), c, style = Stroke(3f))
            }
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        circles.forEachIndexed { i, item ->
            val c = centers[i]
            // Push each label away from the shared middle so the overlap stays readable.
            val dx = c.x - middle.x
            val dy = c.y - middle.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val pos = Offset(c.x + dx / len * r * 0.42f, c.y + dy / len * r * 0.42f)
            with(density) {
                Column(
                    Modifier
                        .offset(x = pos.x.toDp() - 50.dp, y = pos.y.toDp() - 26.dp)
                        .width(100.dp)
                        .alpha(stagger(progress, i, circles.size)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.emoji, fontSize = 18.sp)
                    Text(item.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                }
            }
        }
        with(density) {
            Column(
                Modifier
                    .offset(x = middle.x.toDp() - 48.dp, y = middle.y.toDp() - 20.dp)
                    .width(96.dp)
                    .alpha(progress),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${overlap.emoji} ${overlap.label}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
    val described = items.filter { it.detail.isNotBlank() }
    if (described.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        described.forEach { item ->
            Text(
                "${item.emoji}  ${item.label}: ${item.detail}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LadderGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    // Strongest level on top, like a podium: the list arrives weakest first.
    val levels = items.withIndex().reversed()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (i, item) ->
            val t = stagger(progress, items.size - 1 - i, items.size)
            val fraction = 0.55f + 0.45f * (i + 1) / items.size
            Row(
                Modifier
                    .fillMaxWidth(fraction)
                    .enter(t)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (item.emphasis) palette.accent else palette.accent.copy(alpha = 0.10f + 0.10f * i))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    val onColor = if (item.emphasis) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                    Text(item.label, style = MaterialTheme.typography.titleSmall, color = onColor)
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = onColor.copy(alpha = 0.75f))
                    }
                }
            }
        }
        Text(
            "▲ more leverage",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(progress)
        )
    }
}

@Composable
private fun TimelineGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    Column {
        items.forEachIndexed { i, item ->
            val t = stagger(progress, i, items.size)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(t)) {
                Text(
                    item.valueLabel ?: "",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Display),
                    color = if (item.emphasis) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp).padding(top = 2.dp),
                    textAlign = TextAlign.End
                )
                Box(Modifier.width(32.dp).fillMaxHeight()) {
                    Canvas(Modifier.fillMaxSize()) {
                        val x = size.width / 2
                        if (i < items.lastIndex) {
                            drawLine(palette.accent.copy(alpha = 0.35f), Offset(x, 14f), Offset(x, size.height), 3f)
                        }
                        drawCircle(if (item.emphasis) palette.accent else palette.accent.copy(alpha = 0.45f), if (item.emphasis) 11f else 8f, Offset(x, 22f))
                    }
                }
                Column(Modifier.weight(1f).padding(bottom = 16.dp)) {
                    Text("${item.emoji}  ${item.label}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (item.detail.isNotBlank()) {
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteGraphic(items: List<VisualItem>, palette: TopicPalette, progress: Float) {
    val item = items.firstOrNull() ?: return
    Column(Modifier.enter(progress)) {
        Text("“", style = MaterialTheme.typography.displaySmall.copy(fontSize = 64.sp, lineHeight = 48.sp), color = palette.accent)
        Text(item.label, style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurface)
        if (item.detail.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("— ${item.detail}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatNumber(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

