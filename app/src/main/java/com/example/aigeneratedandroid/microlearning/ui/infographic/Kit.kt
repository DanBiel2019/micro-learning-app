package com.example.aigeneratedandroid.microlearning.ui.infographic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.ui.theme.Display
import com.example.aigeneratedandroid.microlearning.ui.theme.TopicPalette

/**
 * The infographic colour system: one accent (the topic colour) for the punchline, and
 * greys for everything else. People look at the most saturated thing first, so only the
 * emphasised item gets the accent (Datawrapper, "Emphasize what you want readers to see").
 */
@Immutable
class Ink(
    /** The punchline colour; also safe as text on [surface]. */
    val accent: Color,
    /** Tinted background for the punchline's container. */
    val accentSoft: Color,
    /** Text / icons drawn on a solid [accent] fill. */
    val onAccent: Color,
    /** Primary text. */
    val text: Color,
    /** Secondary text (details, eyebrows, axis names). */
    val muted: Color,
    /** Non-emphasised marks: bars, dots, shapes. */
    val mark: Color,
    /** Tracks, rails, connectors and empty cells. */
    val rail: Color,
    /** Container fill for non-emphasised cards. */
    val card: Color,
    /** The card background everything sits on. */
    val surface: Color
)

@Composable
fun ink(palette: TopicPalette): Ink {
    val cs = MaterialTheme.colorScheme
    return Ink(
        accent = palette.accent,
        accentSoft = palette.soft,
        onAccent = cs.surface,
        text = cs.onSurface,
        muted = cs.onSurfaceVariant,
        mark = cs.onSurfaceVariant.copy(alpha = 0.42f),
        rail = cs.onSurfaceVariant.copy(alpha = 0.18f),
        card = cs.surfaceVariant.copy(alpha = 0.6f),
        surface = cs.surface
    )
}

/** Big figures use the editorial serif, so numbers read as the hero of the graphic. */
val NumberStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold)

/** Stagger helper: item [i] of [n] fades/slides in as [progress] goes 0 -> 1. */
fun stagger(progress: Float, i: Int, n: Int): Float {
    if (progress >= 1f) return 1f
    val start = i.toFloat() / (n + 1)
    return ((progress - start) / (1f - start).coerceAtLeast(0.01f) * 1.6f).coerceIn(0f, 1f)
}

fun Modifier.enter(t: Float) = if (t >= 1f) this else this.graphicsLayer {
    alpha = t
    translationY = (1f - t) * 24f
}

/** Small uppercase label used for direct annotations ("STEP 2", "BEFORE", "WHAT YOU SEE"). */
@Composable
fun Eyebrow(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** The item's icon in a round token: accent-filled for the punchline, grey otherwise. */
@Composable
fun EmojiToken(emoji: String, ink: Ink, emphasis: Boolean, size: Dp = 40.dp, fallback: String = "•") {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (emphasis) ink.accent else ink.card),
        contentAlignment = Alignment.Center
    ) {
        val glyph = emoji.ifBlank { fallback }
        Text(
            glyph,
            fontSize = (size.value * 0.46f).sp,
            color = if (emphasis) ink.onAccent else ink.text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
