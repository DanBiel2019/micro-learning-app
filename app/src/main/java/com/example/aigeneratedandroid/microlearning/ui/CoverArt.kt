package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.aigeneratedandroid.microlearning.ui.theme.LocalReducedMotion
import com.example.aigeneratedandroid.microlearning.ui.theme.topicPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Abstract, generated cover art for an episode: deterministic per [seed] and tinted by the
 * episode's topics, with a slow drift so the home screen feels alive.
 */
@Composable
fun CoverArt(seed: String, topics: List<String>, modifier: Modifier = Modifier, animate: Boolean = true) {
    val colors = remember(seed, topics) {
        topics.ifEmpty { listOf("") }.map { topicPalette(it, dark = true).accent }.distinct()
    }
    val shapes = remember(seed) { CoverShapes.from(seed) }
    val drift by if (animate && !LocalReducedMotion.current) {
        rememberInfiniteTransition(label = "cover").animateFloat(
            0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse), label = "drift"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.3f) }
    }

    Canvas(modifier) {
        val base = colors.first()
        val second = colors.getOrElse(1) { colors.first() }
        val third = colors.getOrElse(2) { second }
        // Deep background wash.
        drawRect(
            Brush.linearGradient(
                listOf(lerp(base, Color.Black, 0.72f), lerp(second, Color.Black, 0.82f), Color(0xFF0E0C0B)),
                start = Offset.Zero, end = Offset(size.width, size.height)
            )
        )
        // Soft glowing orbs.
        shapes.orbs.forEachIndexed { i, orb ->
            val c = listOf(base, second, third)[i % 3]
            val wobble = sin((drift * 2 * PI + orb.phase).toFloat())
            val center = Offset(size.width * (orb.x + 0.04f * wobble), size.height * (orb.y + 0.03f * wobble))
            val radius = size.minDimension * orb.r
            drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0f)), center, radius),
                radius, center
            )
        }
        // Concentric rings, like ripples from a signal.
        val ringCenter = Offset(size.width * shapes.ringX, size.height * shapes.ringY)
        for (k in 1..shapes.rings) {
            drawCircle(
                Color.White.copy(alpha = 0.07f + 0.02f * (k % 2)),
                size.minDimension * (0.08f * k + 0.02f * drift),
                ringCenter,
                style = Stroke(1.6f)
            )
        }
        // A bold arc in the lead colour.
        drawArc(
            base.copy(alpha = 0.85f), shapes.arcStart + 40f * drift, shapes.arcSweep, false,
            topLeft = Offset(ringCenter.x - size.minDimension * 0.3f, ringCenter.y - size.minDimension * 0.3f),
            size = Size(size.minDimension * 0.6f, size.minDimension * 0.6f),
            style = Stroke(10f, cap = StrokeCap.Round)
        )
        // Dot grid texture.
        val step = size.minDimension / 14f
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                val d = (Offset(x, y) - ringCenter).getDistance() / size.maxDimension
                drawCircle(Color.White.copy(alpha = (0.10f - d * 0.12f).coerceAtLeast(0f)), 1.6f, Offset(x, y))
                x += step
            }
            y += step
        }
        // Orbiting satellites.
        shapes.satellites.forEach { s ->
            val a = s.angle + drift * 0.6f * s.speed
            val r = size.minDimension * s.orbit
            drawCircle(Color.White.copy(alpha = 0.75f), s.size, Offset(ringCenter.x + r * cos(a), ringCenter.y + r * sin(a)))
        }
    }
}

private class CoverShapes(
    val orbs: List<Orb>,
    val rings: Int,
    val ringX: Float,
    val ringY: Float,
    val arcStart: Float,
    val arcSweep: Float,
    val satellites: List<Satellite>
) {
    class Orb(val x: Float, val y: Float, val r: Float, val phase: Float)
    class Satellite(val orbit: Float, val angle: Float, val speed: Float, val size: Float)

    companion object {
        fun from(seed: String): CoverShapes {
            val rnd = Random(seed.hashCode())
            return CoverShapes(
                orbs = List(4) { Orb(rnd.nextFloat(), rnd.nextFloat() * 0.8f, 0.35f + rnd.nextFloat() * 0.35f, rnd.nextFloat() * 6f) },
                rings = 3 + rnd.nextInt(4),
                ringX = 0.55f + rnd.nextFloat() * 0.35f,
                ringY = 0.15f + rnd.nextFloat() * 0.35f,
                arcStart = rnd.nextFloat() * 360f,
                arcSweep = 70f + rnd.nextFloat() * 120f,
                satellites = List(3) {
                    Satellite(0.16f + 0.08f * it, rnd.nextFloat() * 6.28f, 1f + rnd.nextFloat() * 2f, 3f + rnd.nextFloat() * 4f)
                }
            )
        }
    }
}
