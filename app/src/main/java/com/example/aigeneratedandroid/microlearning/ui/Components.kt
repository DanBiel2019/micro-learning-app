package com.example.aigeneratedandroid.microlearning.ui

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.Segment
import com.example.aigeneratedandroid.microlearning.ui.theme.LocalDarkTheme
import com.example.aigeneratedandroid.microlearning.ui.theme.LocalReducedMotion
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Sets status-bar icon colour for the screen on top: light icons over dark art, else follow the theme. */
@Composable
fun StatusBarIcons(overDarkContent: Boolean) {
    val view = LocalView.current
    val dark = LocalDarkTheme.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !overDarkContent && !dark
        }
    }
}

/** Section title, announced as a heading by TalkBack so screen-reader users can jump between sections. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xxl, bottom = Spacing.s),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Three bouncing bars that mean "this is playing". Static when animations are turned off. */
@Composable
fun Equalizer(color: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val t = rememberInfiniteTransition(label = "eq")
    Row(
        modifier.height(18.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(420 to 0.6f, 300 to 1f, 520 to 0.8f).forEach { (period, still) ->
            val h by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(period), RepeatMode.Reverse), label = "bar")
            Box(
                Modifier
                    .width(4.dp)
                    .height((18 * if (reduced) still else h).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/** The emoji "thumbnail" for a segment, on its topic tint. Decorative for TalkBack. */
@Composable
fun SegmentGlyph(segment: Segment?, tint: Color, size: Dp, corner: Dp, fallback: String = "💡") {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(tint)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(segmentEmoji(segment, fallback), fontSize = (size.value * 0.46f).sp)
    }
}

fun segmentEmoji(segment: Segment?, fallback: String = "💡"): String =
    segment?.visual?.items?.firstOrNull { it.emphasis && it.emoji.isNotBlank() }?.emoji
        ?: segment?.visual?.items?.firstOrNull { it.emoji.isNotBlank() }?.emoji
        ?: fallback

/** Inline message with an optional action, e.g. offline + "Try again" (NN/g: say what happened and what to do). */
@Composable
fun NoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Rounded.CloudOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(Modifier.padding(start = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.s))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = Spacing.s)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Dismiss message", Modifier.size(20.dp)) }
            } else {
                Spacer(Modifier.width(Spacing.xs))
            }
        }
    }
}

/** Friendly empty state: what this place is for, and how it gets filled. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        Spacer(Modifier.height(Spacing.m))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.m))
            action()
        }
    }
}

/** Placeholder block for skeleton screens; gently pulses unless animations are off. */
@Composable
fun SkeletonBlock(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(8.dp)) {
    val reduced = LocalReducedMotion.current
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
    )
    Box(
        modifier
            .graphicsLayer { this.alpha = if (reduced) 0.7f else alpha }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
}.getOrDefault(iso)

fun formatShortDate(iso: String): String = runCatching {
    val d = LocalDate.parse(iso)
    when (d) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
}.getOrDefault(iso)

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** Spoken form for TalkBack, e.g. "2 minutes 5 seconds". */
fun spokenDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return listOfNotNull(
        if (m > 0) "$m minute${if (m == 1L) "" else "s"}" else null,
        if (r > 0 || m == 0L) "$r second${if (r == 1L) "" else "s"}" else null
    ).joinToString(" ")
}

fun episodeMinutes(ep: Episode): Long {
    val ms = ep.totalDurationMs.takeIf { it > 0 } ?: ep.segments.sumOf { it.durationMs ?: 100_000L }
    return ((ms + 30_000) / 60_000).coerceAtLeast(1)
}

fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 4..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
