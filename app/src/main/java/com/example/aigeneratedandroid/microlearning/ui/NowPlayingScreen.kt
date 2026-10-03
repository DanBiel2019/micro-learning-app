package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.playback.EpisodePlayer
import com.example.aigeneratedandroid.microlearning.playback.PlayerState
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing
import com.example.aigeneratedandroid.microlearning.ui.theme.topicPalette

/**
 * Full-screen player, opened from the mini player. Always dark (like the cover art) so it reads
 * as a separate, focused surface. Swipe down or tap the chevron to collapse.
 */
@Composable
fun NowPlayingScreen(
    episode: Episode,
    state: PlayerState,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSkipTo: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
    onOpenTranscript: (Int) -> Unit
) {
    StatusBarIcons(overDarkContent = true)
    val index = state.segmentIndex.coerceIn(0, episode.segments.lastIndex)
    val seg = episode.segments[index]
    val accent = topicPalette(seg.topic, dark = true).accent
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dismissPx = with(density) { 140.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    val dragOffset by animateFloatAsState(drag, tween(if (drag == 0f) 200 else 0), label = "drag")
    val onDark = Color.White
    val muted = Color.White.copy(alpha = 0.72f)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragOffset.coerceAtLeast(0f) }
            .background(
                Brush.verticalGradient(
                    listOf(lerp(accent, Color.Black, 0.55f), Color(0xFF12100E), Color(0xFF0E0C0B))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header: the drag handle area.
            Row(
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        rememberDraggableState { drag = (drag + it).coerceAtLeast(0f) },
                        Orientation.Vertical,
                        onDragStopped = { if (drag > dismissPx) onClose() else drag = 0f }
                    )
                    .padding(horizontal = Spacing.xxs, vertical = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint = onDark) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = muted)
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = onDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { onOpenTranscript(index) }) {
                    Icon(Icons.AutoMirrored.Rounded.Notes, "Transcript and sources", tint = onDark)
                }
            }

            // Artwork (also draggable, the biggest target for "swipe down to close").
            AnimatedContent(
                targetState = index,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "art",
                modifier = Modifier
                    .padding(horizontal = Spacing.xl, vertical = Spacing.m)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .draggable(
                        rememberDraggableState { drag = (drag + it).coerceAtLeast(0f) },
                        Orientation.Vertical,
                        onDragStopped = { if (drag > dismissPx) onClose() else drag = 0f }
                    )
            ) { i ->
                val s = episode.segments[i]
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).clearAndSetSemantics { }) {
                    CoverArt("${episode.id}/${s.id}", listOf(s.topic), Modifier.fillMaxSize())
                    Text(
                        segmentEmoji(s, "🎧"),
                        fontSize = 72.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Title block.
            Column(Modifier.padding(horizontal = Spacing.xl)) {
                Text(
                    (seg.kicker.ifBlank { seg.topic }).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(seg.title, style = MaterialTheme.typography.headlineSmall, color = onDark, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "Idea ${index + 1} of ${episode.segments.size} · " +
                        if (state.deviceVoice) "read by your phone's voice" else "${episode.hostName("host")} & ${episode.hostName("cohost")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted
                )
            }

            // Scrubber.
            Spacer(Modifier.height(Spacing.m))
            if (!state.deviceVoice) {
                Scrubber(state, accent, onSeek)
            } else {
                Spacer(Modifier.height(Spacing.s))
            }

            // Transport controls.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.s),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSkipTo(index - 1) }, enabled = index > 0) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous idea", tint = if (index > 0) onDark else muted.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                }
                if (!state.deviceVoice) {
                    IconButton(onClick = { onSeekBy(-15_000) }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Replay, "Back 15 seconds", tint = onDark, modifier = Modifier.size(32.dp))
                            Text("15", fontSize = 8.sp, color = onDark, modifier = Modifier.padding(top = 3.dp).clearAndSetSemantics { })
                        }
                    }
                }
                Surface(
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onToggle() },
                    shape = CircleShape,
                    color = Color.White,
                    contentColor = Color(0xFF15120F),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                if (!state.deviceVoice) {
                    IconButton(onClick = { onSeekBy(30_000) }) {
                        Icon(Icons.Rounded.Forward30, "Forward 30 seconds", tint = onDark, modifier = Modifier.size(32.dp))
                    }
                }
                val hasNext = index < episode.segments.lastIndex
                IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSkipTo(index + 1) }, enabled = hasNext) {
                    Icon(Icons.Rounded.SkipNext, "Next idea", tint = if (hasNext) onDark else muted.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                }
            }

            // Secondary actions: speed and transcript.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally)
            ) {
                if (!state.deviceVoice) {
                    var speedMenu by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(
                            onClick = { speedMenu = true },
                            label = { Text("Speed ${speedLabel(state.speed)}") },
                            leadingIcon = { Icon(Icons.Rounded.Speed, null, Modifier.size(18.dp)) },
                            colors = darkChipColors(),
                            border = AssistChipDefaults.assistChipBorder(true, borderColor = Color.White.copy(alpha = 0.3f))
                        )
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            EpisodePlayer.SPEEDS.sorted().forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(speedLabel(s) + if (s == state.speed) "  ✓" else "") },
                                    onClick = { onSpeed(s); speedMenu = false }
                                )
                            }
                        }
                    }
                }
                AssistChip(
                    onClick = { onOpenTranscript(index) },
                    label = { Text("Transcript & sources") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null, Modifier.size(18.dp)) },
                    colors = darkChipColors(),
                    border = AssistChipDefaults.assistChipBorder(true, borderColor = Color.White.copy(alpha = 0.3f))
                )
            }

            // What's being said right now (handy when you glance over from the kettle).
            val line = seg.lines.getOrNull(state.lineIndex)?.let { it.speaker to it.text }
                ?: seg.script.getOrNull(state.lineIndex)?.let { it.speaker to it.text }
            if (line != null) {
                Surface(
                    onClick = { onOpenTranscript(index) },
                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs).fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = Color.White.copy(alpha = 0.08f),
                    contentColor = onDark
                ) {
                    Column(Modifier.padding(Spacing.m)) {
                        Text(episode.hostName(line.first).uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                        Spacer(Modifier.height(Spacing.xxs))
                        AnimatedContent(line.second, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "line") { text ->
                            Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Chapters: every idea in the episode, tap to jump.
            Text(
                "Ideas in this episode",
                style = MaterialTheme.typography.titleLarge,
                color = onDark,
                modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xl, bottom = Spacing.xs).semantics { heading() }
            )
            episode.segments.forEachIndexed { i, s ->
                val c = topicPalette(s.topic, dark = true).accent
                val current = i == index
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(onClickLabel = "Play this idea") { onSkipTo(i) }
                        .background(if (current) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = Spacing.xl, vertical = Spacing.s)
                        .semantics(mergeDescendants = true) {
                            stateDescription = if (current) "Playing" else ""
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        if (current && state.isPlaying) Equalizer(c)
                        else Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = if (current) c else muted)
                    }
                    Spacer(Modifier.width(Spacing.s))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = MaterialTheme.typography.titleSmall, color = onDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(s.topic, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 1)
                    }
                    s.durationMs?.let {
                        Text(formatDuration(it), style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun darkChipColors() = AssistChipDefaults.assistChipColors(
    labelColor = Color.White,
    leadingIconContentColor = Color.White
)

@Composable
private fun Scrubber(state: PlayerState, accent: Color, onSeek: (Long) -> Unit) {
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0)
    val fraction = scrubbing ?: if (duration > 0) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownPos = (fraction * duration).toLong()
    Column(Modifier.padding(horizontal = Spacing.l)) {
        Slider(
            value = fraction,
            onValueChange = { scrubbing = it },
            onValueChangeFinished = {
                scrubbing?.let { onSeek((it * duration).toLong()) }
                scrubbing = null
            },
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.semantics {
                contentDescription = "Position in this idea"
                stateDescription = "${spokenDuration(shownPos)} of ${spokenDuration(duration)}"
            }
        )
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            Text(formatDuration(shownPos), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
            Spacer(Modifier.weight(1f))
            Text(
                "-" + formatDuration((duration - shownPos).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.End
            )
        }
    }
}
