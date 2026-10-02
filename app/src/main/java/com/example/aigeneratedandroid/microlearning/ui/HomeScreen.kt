package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.PreviousEpisode
import com.example.aigeneratedandroid.microlearning.model.Segment
import com.example.aigeneratedandroid.microlearning.playback.PlayerState
import com.example.aigeneratedandroid.microlearning.ui.theme.palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    ui: UiState,
    player: PlayerState,
    contentPadding: PaddingValues,
    onPlayEpisode: () -> Unit,
    onOpenSegment: (Int) -> Unit,
    onOpenPrevious: (PreviousEpisode) -> Unit,
    onOpenLibrary: () -> Unit,
    onDismissNotice: () -> Unit,
    completedKey: (Episode, Segment) -> String
) {
    val episode = ui.episode ?: return
    val thisPlaying = player.episodeId == episode.id
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { Hero(episode, playing = thisPlaying && player.isPlaying, started = thisPlaying, onPlay = onPlayEpisode) }

        if (ui.refreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary) }
        }
        ui.notice?.let { notice ->
            item { Notice(notice, onDismissNotice) }
        }

        item {
            SectionHeader("In this episode", "${episode.segments.size} ideas")
        }
        itemsIndexed(episode.segments, key = { _, s -> s.id }) { i, seg ->
            SegmentRow(
                index = i,
                segment = seg,
                current = thisPlaying && player.segmentIndex == i,
                playing = thisPlaying && player.segmentIndex == i && player.isPlaying,
                progress = if (thisPlaying && player.segmentIndex == i && player.durationMs > 0) player.positionMs.toFloat() / player.durationMs else 0f,
                done = completedKey(episode, seg) in ui.completed,
                onClick = { onOpenSegment(i) }
            )
        }

        if (ui.history.isNotEmpty()) {
            item { SectionHeader("Earlier episodes", "Catch up anytime") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.history, key = { it.id }) { prev -> PreviousCard(prev) { onOpenPrevious(prev) } }
                }
            }
        }

        item {
            Row(
                Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenLibrary)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your classics library", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "30 ideas from the books you love, available offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text(
                "A new studio episode arrives every morning. Every segment cites its sources.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Hero(episode: Episode, playing: Boolean, started: Boolean, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(440.dp)) {
        CoverArt(episode.id, episode.segments.map { it.topic }, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                formatDate(episode.date).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(8.dp))
            Text(episode.title, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(episode.theme, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onPlay,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    contentColor = Color(0xFF15120F)
                ) {
                    Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                playing -> "Pause"
                                started -> "Resume"
                                else -> "Play episode"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(episodeLength(episode), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text(
                        if (episode.offline) "Device voice" else "with ${episode.hostName("host")} & ${episode.hostName("cohost")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String, onDismiss: () -> Unit) {
    Row(
        Modifier
            .padding(start = 20.dp, end = 12.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Dismiss", Modifier.size(18.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SegmentRow(
    index: Int,
    segment: Segment,
    current: Boolean,
    playing: Boolean,
    progress: Float,
    done: Boolean,
    onClick: () -> Unit
) {
    val pal = palette(segment.topic)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (current) pal.soft else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pal.soft),
            contentAlignment = Alignment.Center
        ) {
            Text(segment.visual?.items?.firstOrNull { it.emphasis }?.emoji ?: segment.visual?.items?.firstOrNull()?.emoji ?: "💡", fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (segment.kicker.ifBlank { segment.topic }).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = pal.accent,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(segment.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(segment.durationMs?.let(::formatDuration), segment.topic, segment.source.author)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (current && progress > 0f) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)),
                    color = pal.accent,
                    trackColor = pal.soft
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        when {
            playing -> Equalizer(pal.accent)
            done -> Box(
                Modifier.size(26.dp).clip(CircleShape).background(pal.accent),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Check, "Listened", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp)) }
            else -> Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun Equalizer(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    Row(modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        listOf(420, 300, 520).forEach { period ->
            val h by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(period), RepeatMode.Reverse), label = "bar")
            Box(Modifier.width(4.dp).fillMaxHeightFraction(h).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

private fun Modifier.fillMaxHeightFraction(f: Float) = this.then(Modifier.height((18 * f).dp))

@Composable
private fun PreviousCard(prev: PreviousEpisode, onClick: () -> Unit) {
    Column(
        Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp))) {
            CoverArt(prev.id, listOf(prev.title), Modifier.fillMaxSize(), animate = false)
        }
        Spacer(Modifier.height(8.dp))
        Text(formatDate(prev.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(prev.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
}.getOrDefault(iso)

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

private fun episodeLength(ep: Episode): String {
    val ms = ep.totalDurationMs.takeIf { it > 0 } ?: (ep.segments.size * 100_000L)
    val min = ((ms + 30_000) / 60_000).coerceAtLeast(1)
    return "$min min · ${ep.segments.size} ideas"
}
