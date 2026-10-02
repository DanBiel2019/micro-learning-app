package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.playback.PlayerState
import com.example.aigeneratedandroid.microlearning.ui.theme.palette

@Composable
fun MiniPlayer(
    episode: Episode,
    state: PlayerState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onBack15: () -> Unit,
    onForward30: () -> Unit,
    onNext: () -> Unit,
    onSpeed: () -> Unit
) {
    val seg = episode.segments.getOrNull(state.segmentIndex) ?: return
    val pal = palette(seg.topic)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(Modifier.navigationBarsPadding()) {
            // Segment progress across the top edge.
            Box(Modifier.fillMaxWidth().height(3.dp).background(pal.soft)) {
                val f = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
                Box(Modifier.fillMaxWidth(f).height(3.dp).background(pal.accent))
            }
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(pal.soft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(seg.visual?.items?.firstOrNull { it.emphasis }?.emoji ?: "🎧", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(seg.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (state.deviceVoice) "${state.segmentIndex + 1} of ${episode.segments.size} · device voice"
                        else "${state.segmentIndex + 1} of ${episode.segments.size} · ${formatDuration(state.positionMs)} / ${formatDuration(state.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (!state.deviceVoice) {
                    Text(
                        if (state.speed == 1f) "1×" else "${state.speed}×",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSpeed).padding(8.dp)
                    )
                    IconButton(onClick = onBack15) { Icon(Icons.Rounded.Replay, "Back 15 seconds") }
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(pal.accent).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
                if (!state.deviceVoice) {
                    IconButton(onClick = onForward30) { Icon(Icons.Rounded.Forward30, "Forward 30 seconds") }
                } else {
                    IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "Next idea") }
                }
            }
        }
    }
}
