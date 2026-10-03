package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.playback.PlayerState
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing
import com.example.aigeneratedandroid.microlearning.ui.theme.palette

/**
 * Persistent "now playing" bar above the navigation bar (the podcast-app convention): what's
 * playing, play/pause and one skip. Tap or swipe up to open the full player.
 */
@Composable
fun MiniPlayer(
    episode: Episode,
    state: PlayerState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onForward30: () -> Unit,
    onNext: () -> Unit
) {
    val seg = episode.segments.getOrNull(state.segmentIndex) ?: return
    val pal = palette(seg.topic)
    var drag by remember { mutableFloatStateOf(0f) }
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
            .draggable(
                rememberDraggableState { drag += it },
                Orientation.Vertical,
                onDragStarted = { drag = 0f },
                onDragStopped = { if (drag < -40f) onOpen() }
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 3.dp
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Open player", onClick = onOpen)
                    .padding(start = Spacing.xs, end = Spacing.xxs, top = Spacing.xs, bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentGlyph(seg, pal.soft, size = 44.dp, corner = 10.dp, fallback = "🎧")
                Spacer(Modifier.width(Spacing.s))
                Column(
                    Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Now playing: ${seg.title}. Idea ${state.segmentIndex + 1} of ${episode.segments.size}."
                        }
                ) {
                    Text(seg.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (state.deviceVoice) "Idea ${state.segmentIndex + 1} of ${episode.segments.size} · device voice"
                        else "Idea ${state.segmentIndex + 1} of ${episode.segments.size} · ${formatDuration((state.durationMs - state.positionMs).coerceAtLeast(0))} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledIconButton(
                    onClick = onToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = pal.accent, contentColor = MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play"
                    )
                }
                if (!state.deviceVoice) {
                    IconButton(onClick = onForward30) { Icon(Icons.Rounded.Forward30, "Forward 30 seconds") }
                } else {
                    IconButton(onClick = onNext, enabled = state.segmentIndex < episode.segments.lastIndex) {
                        Icon(Icons.Rounded.SkipNext, "Next idea")
                    }
                }
            }
            // Progress through the current idea, along the bottom edge.
            Box(
                Modifier
                    .padding(horizontal = Spacing.s)
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(pal.soft)
            ) {
                Box(Modifier.fillMaxWidth(progress).height(2.dp).background(pal.accent))
            }
            Spacer(Modifier.height(Spacing.xxs))
        }
    }
}
