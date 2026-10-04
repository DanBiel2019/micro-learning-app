package com.example.aigeneratedandroid.microlearning.ui.infographic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.VisualItem
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Steps around a loop with arrowheads; the centre explains the step that matters most. */
@Composable
fun CycleGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val focus = items.firstOrNull { it.emphasis } ?: items.last()
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val w = constraints.maxWidth.toFloat()
        val center = Offset(w / 2, w / 2)
        val radius = w * 0.33f
        val n = items.size
        Canvas(Modifier.fillMaxSize()) {
            val sweepEach = 360f / n
            val stroke = 2.5.dp.toPx()
            for (i in 0 until n) {
                val t = stagger(progress, i, n)
                val start = -90f + i * sweepEach + 22f
                val sweep = (sweepEach - 44f) * t
                drawArc(
                    ink.mark, start, sweep, false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                if (t > 0.95f) {
                    val endAngle = Math.toRadians((start + sweep).toDouble())
                    val tip = Offset(center.x + radius * cos(endAngle).toFloat(), center.y + radius * sin(endAngle).toFloat())
                    val tangent = endAngle + Math.PI / 2
                    val len = 9.dp.toPx()
                    val back = Offset(tip.x - len * cos(tangent).toFloat(), tip.y - len * sin(tangent).toFloat())
                    val perp = Offset(-sin(tangent).toFloat(), cos(tangent).toFloat())
                    val half = 5.5.dp.toPx()
                    drawLine(ink.mark, tip, Offset(back.x + perp.x * half, back.y + perp.y * half), stroke, StrokeCap.Round)
                    drawLine(ink.mark, tip, Offset(back.x - perp.x * half, back.y - perp.y * half), stroke, StrokeCap.Round)
                }
            }
        }
        val density = LocalDensity.current
        val nodeWidth = 108.dp
        items.forEachIndexed { i, item ->
            val angle = Math.toRadians(-90.0 + i * 360.0 / n)
            val px = center.x + radius * cos(angle).toFloat()
            val py = center.y + radius * sin(angle).toFloat()
            with(density) {
                Column(
                    Modifier
                        .offset(x = px.toDp() - nodeWidth / 2, y = py.toDp() - 20.dp)
                        .width(nodeWidth)
                        .alpha(stagger(progress, i, n)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmojiToken(item.emoji, ink, item.emphasis)
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = if (item.emphasis) ink.accent else ink.text,
                        maxLines = 3,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ink.surface)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 132.dp).alpha(progress),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("↻", fontSize = 22.sp, color = ink.accent)
            if (focus.detail.isNotBlank()) {
                Text(
                    focus.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Overlapping circles in neutral ink, with only the shared region filled in the accent: the
 * overlap is the idea, so it is the one thing coloured. The LAST item names the overlap.
 */
@Composable
fun VennGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val circles = items.dropLast(1).take(3)
    val overlap = items.lastOrNull() ?: return
    val three = circles.size == 3
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(if (three) 1.05f else 1.6f)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val r = if (three) w * 0.25f else h * 0.42f
        val centers = if (three) {
            listOf(Offset(w * 0.37f, h * 0.36f), Offset(w * 0.63f, h * 0.36f), Offset(w * 0.5f, h * 0.62f))
        } else {
            listOf(Offset(w * 0.36f, h * 0.5f), Offset(w * 0.64f, h * 0.5f))
        }
        val middle = Offset(centers.map { it.x }.average().toFloat(), centers.map { it.y }.average().toFloat())
        Canvas(Modifier.fillMaxSize()) {
            val shapes = centers.map { c -> Path().apply { addOval(Rect(c, r)) } }
            centers.forEachIndexed { i, c ->
                val t = stagger(progress, i, centers.size)
                drawCircle(ink.mark.copy(alpha = 0.08f * t), r, c)
                drawCircle(ink.mark.copy(alpha = 0.9f * t), r, c, style = Stroke(1.5.dp.toPx()))
            }
            var shared = shapes.first()
            for (s in shapes.drop(1)) shared = Path().apply { op(shared, s, PathOperation.Intersect) }
            drawPath(shared, ink.accent.copy(alpha = 0.32f * progress))
        }
        val density = LocalDensity.current
        circles.forEachIndexed { i, item ->
            val c = centers[i]
            // Push each label away from the shared middle so the overlap stays readable.
            val dx = c.x - middle.x
            val dy = c.y - middle.y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val pos = Offset(c.x + dx / len * r * 0.45f, c.y + dy / len * r * 0.45f)
            with(density) {
                Column(
                    Modifier
                        .offset(x = pos.x.toDp() - 50.dp, y = pos.y.toDp() - 24.dp)
                        .width(100.dp)
                        .alpha(stagger(progress, i, circles.size)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (item.emoji.isNotBlank()) Text(item.emoji, fontSize = 18.sp)
                    Text(item.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, color = ink.text, maxLines = 2)
                }
            }
        }
        with(density) {
            Column(
                Modifier
                    .offset(x = middle.x.toDp() - 52.dp, y = middle.y.toDp() - (if (three) 16.dp else 20.dp))
                    .width(104.dp)
                    .alpha(progress),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (overlap.emoji.isBlank()) overlap.label else "${overlap.emoji} ${overlap.label}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ink.accent,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ink.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
    val described = items.filter { it.detail.isNotBlank() }
    if (described.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            described.forEach { item ->
                val isOverlap = item === overlap
                Text(
                    "${item.label}: ${item.detail}",
                    style = MaterialTheme.typography.bodySmall.let { if (isOverlap) it.copy(fontWeight = FontWeight.SemiBold) else it },
                    color = if (isOverlap) ink.accent else ink.muted
                )
            }
        }
    }
}

/**
 * What everyone sees (the first item) above the waterline and what actually drives it below.
 * The submerged mass is drawn much larger than the tip: that size difference is the message.
 */
@Composable
fun IcebergGraphic(items: List<VisualItem>, ink: Ink, progress: Float) {
    val visible = items.first()
    val hidden = items.drop(1)
    var waterY by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                if (waterY == 0) return@drawBehind
                val wy = waterY.toFloat()
                val w = size.width
                val h = size.height
                // Water.
                drawRect(ink.accentSoft, Offset(0f, wy), Size(w, h - wy))
                // The tip: a small peak above the line.
                val rise = (wy * 0.78f) * progress.coerceAtLeast(0.2f)
                val tip = Path().apply {
                    moveTo(w * 0.05f, wy)
                    lineTo(w * 0.13f, wy - rise * 0.55f)
                    lineTo(w * 0.19f, wy - rise)
                    lineTo(w * 0.25f, wy - rise * 0.7f)
                    lineTo(w * 0.33f, wy)
                    close()
                }
                drawPath(tip, ink.card)
                drawPath(tip, ink.mark, style = Stroke(1.5.dp.toPx()))
                // The mass below, several times the size of the tip.
                val depth = (h - wy)
                val mass = Path().apply {
                    moveTo(w * 0.03f, wy)
                    lineTo(w * 0.36f, wy)
                    lineTo(w * 0.62f, wy + depth * 0.18f)
                    lineTo(w * 0.94f, wy + depth * 0.45f)
                    lineTo(w * 0.80f, wy + depth * 0.80f)
                    lineTo(w * 0.42f, wy + depth * 0.96f)
                    lineTo(w * 0.06f, wy + depth * 0.70f)
                    lineTo(w * 0.01f, wy + depth * 0.30f)
                    close()
                }
                drawPath(mass, ink.accent.copy(alpha = 0.12f * progress))
                drawPath(mass, ink.accent.copy(alpha = 0.35f * progress), style = Stroke(1.5.dp.toPx()))
                // Waterline.
                drawLine(
                    ink.accent.copy(alpha = 0.6f), Offset(0f, wy), Offset(w, wy), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .onSizeChanged { waterY = it.height }
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Spacer(Modifier.fillMaxWidth(0.38f))
            Column(Modifier.weight(1f).padding(end = 8.dp).enter(stagger(progress, 0, items.size))) {
                Eyebrow("What you see", ink.muted)
                Text(
                    if (visible.emoji.isBlank()) visible.label else "${visible.emoji}  ${visible.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (visible.emphasis) ink.accent else ink.text
                )
                if (visible.detail.isNotBlank()) {
                    Text(visible.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
                }
            }
        }
        Column(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Eyebrow("Below the surface", ink.accent)
            hidden.forEachIndexed { i, item ->
                Row(Modifier.enter(stagger(progress, i + 1, items.size)), verticalAlignment = Alignment.Top) {
                    Text(item.emoji.ifBlank { "•" }, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.titleSmall.let { if (item.emphasis) it.copy(fontWeight = FontWeight.Bold) else it },
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

/**
 * A 2x2: items fill top-left, top-right, bottom-left, bottom-right. The axes are drawn as
 * arrows and named directly (no legend); top-right is "high on both".
 */
@Composable
fun MatrixGraphic(items: List<VisualItem>, ink: Ink, progress: Float, xAxis: String, yAxis: String) {
    Column {
        if (yAxis.isNotBlank()) Eyebrow("▲ $yAxis", ink.muted, Modifier.padding(start = 4.dp, bottom = 6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val s = 2.dp.toPx()
                    val head = 6.dp.toPx()
                    // y axis on the left, x axis along the bottom, both pointing to "more".
                    drawLine(ink.mark, Offset(0f, size.height), Offset(0f, 0f), s, StrokeCap.Round)
                    drawLine(ink.mark, Offset(0f, 0f), Offset(-head * 0.7f, head), s, StrokeCap.Round)
                    drawLine(ink.mark, Offset(0f, 0f), Offset(head * 0.7f, head), s, StrokeCap.Round)
                    drawLine(ink.mark, Offset(0f, size.height), Offset(size.width, size.height), s, StrokeCap.Round)
                    drawLine(ink.mark, Offset(size.width, size.height), Offset(size.width - head, size.height - head * 0.7f), s, StrokeCap.Round)
                    drawLine(ink.mark, Offset(size.width, size.height), Offset(size.width - head, size.height + head * 0.7f), s, StrokeCap.Round)
                }
                .padding(start = 10.dp, bottom = 10.dp, top = 10.dp, end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(4).chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { col, item ->
                        MatrixCell(item, ink, stagger(progress, row * 2 + col, 4), Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
        if (xAxis.isNotBlank()) Eyebrow("$xAxis  ▶", ink.muted, Modifier.align(Alignment.End).padding(top = 6.dp))
    }
}

@Composable
private fun MatrixCell(item: VisualItem, ink: Ink, t: Float, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .enter(t)
            .clip(shape)
            .background(if (item.emphasis) ink.accentSoft else ink.card)
            .then(if (item.emphasis) Modifier.border(1.5.dp, ink.accent, shape) else Modifier)
            .heightIn(min = 104.dp)
            .padding(12.dp)
    ) {
        if (item.emoji.isNotBlank()) {
            Text(item.emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
        }
        Text(item.label, style = MaterialTheme.typography.titleSmall, color = if (item.emphasis) ink.accent else ink.text)
        if (item.detail.isNotBlank()) {
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = ink.muted)
        }
    }
}
