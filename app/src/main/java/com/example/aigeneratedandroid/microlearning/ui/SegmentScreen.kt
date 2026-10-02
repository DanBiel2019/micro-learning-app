package com.example.aigeneratedandroid.microlearning.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.data.Reaction
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.Segment
import com.example.aigeneratedandroid.microlearning.playback.PlayerState
import com.example.aigeneratedandroid.microlearning.ui.theme.Display
import com.example.aigeneratedandroid.microlearning.ui.theme.TopicPalette
import com.example.aigeneratedandroid.microlearning.ui.theme.palette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@Composable
fun SegmentScreen(
    episode: Episode,
    startIndex: Int,
    player: PlayerState,
    reactions: Map<String, Int>,
    contentPadding: PaddingValues,
    keyOf: (Segment) -> String,
    related: (Segment) -> List<Segment>,
    onBack: () -> Unit,
    onPageSettled: (Int) -> Unit,
    onPlaySegment: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onReact: (Segment, Reaction) -> Unit
) {
    val pager = rememberPagerState(initialPage = startIndex) { episode.segments.size }
    val thisEpisode = player.episodeId == episode.id

    // Swiping while listening skips the audio to that segment.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().drop(1).collect(onPageSettled)
    }
    // When playback moves on by itself, follow it if you were on the segment that just ended.
    LaunchedEffect(player.segmentIndex, thisEpisode) {
        if (thisEpisode && pager.settledPage != player.segmentIndex &&
            (pager.settledPage == player.segmentIndex - 1 || pager.settledPage == player.segmentIndex + 1)
        ) {
            pager.animateScrollToPage(player.segmentIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(
                episode.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            PageDots(episode, pager.currentPage)
            Spacer(Modifier.width(16.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            val seg = episode.segments[page]
            SegmentPage(
                episode = episode,
                index = page,
                segment = seg,
                isCurrent = thisEpisode && player.segmentIndex == page,
                player = player,
                reaction = reactions[keyOf(seg)] ?: 0,
                related = remember(seg.id) { related(seg) },
                contentPadding = contentPadding,
                onPlay = { onPlaySegment(page) },
                onSeek = onSeek,
                onReact = { onReact(seg, it) }
            )
        }
    }
}

@Composable
private fun PageDots(episode: Episode, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        episode.segments.forEachIndexed { i, s ->
            val pal = palette(s.topic)
            Box(
                Modifier
                    .size(width = if (i == current) 18.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i == current) pal.accent else MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun SegmentPage(
    episode: Episode,
    index: Int,
    segment: Segment,
    isCurrent: Boolean,
    player: PlayerState,
    reaction: Int,
    related: List<Segment>,
    contentPadding: PaddingValues,
    onPlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onReact: (Reaction) -> Unit
) {
    val pal = palette(segment.topic)
    val context = LocalContext.current
    fun open(url: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TopicChip(segment.topic, pal)
            if (segment.adjacent) {
                Spacer(Modifier.width(6.dp))
                Text("NEW TERRITORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text("${index + 1} of ${episode.segments.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        if (segment.kicker.isNotBlank()) {
            Text(segment.kicker, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Display, fontStyle = FontStyle.Italic), color = pal.accent)
            Spacer(Modifier.height(4.dp))
        }
        Text(segment.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))

        if (!(isCurrent && player.isPlaying)) {
            Surface(onClick = onPlay, shape = RoundedCornerShape(50), color = pal.accent, contentColor = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Headphones, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (isCurrent) "Resume" else "Listen") + (segment.durationMs?.let { " · ${formatDuration(it)}" } ?: ""),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Equalizer(pal.accent)
                Spacer(Modifier.width(10.dp))
                Text("Now playing", style = MaterialTheme.typography.labelLarge, color = pal.accent)
            }
        }
        Spacer(Modifier.height(20.dp))

        segment.visual?.let { Infographic(it, pal) }
        Spacer(Modifier.height(22.dp))

        Text(segment.summary, style = MaterialTheme.typography.bodyLarge)

        if (segment.keyPoints.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            segment.keyPoints.forEach { point ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Box(Modifier.padding(top = 8.dp).size(7.dp).clip(CircleShape).background(pal.accent))
                    Spacer(Modifier.width(12.dp))
                    Text(point, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (segment.takeaway.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(pal.accent))
                Spacer(Modifier.width(16.dp))
                Text(segment.takeaway, style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal))
            }
        }

        if (segment.challenge.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(pal.soft)
                    .padding(18.dp)
            ) {
                Text("TRY THIS TODAY", style = MaterialTheme.typography.labelSmall, color = pal.accent)
                Spacer(Modifier.height(6.dp))
                Text(segment.challenge, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Transcript(episode, segment, isCurrent, player, pal, onSeek)

        if (segment.deeperQuestions.isNotEmpty() || segment.furtherReading.isNotEmpty() || related.isNotEmpty()) {
            SectionTitle("Go deeper")
            segment.deeperQuestions.forEach { q ->
                Text("→  $q", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 5.dp))
            }
            segment.furtherReading.forEach { r ->
                LinkRow(r.title, r.note, r.url?.let { { open(it) } })
            }
            related.forEach { rel ->
                LinkRow("From your library: ${rel.title}", "${rel.source.title} · ${rel.source.author}", null)
            }
        }

        SectionTitle("Source")
        LinkRow(
            segment.source.title,
            listOfNotNull(segment.source.author, segment.source.year?.toString(), segment.source.format.replaceFirstChar { it.uppercase() }).joinToString(" · "),
            segment.source.url?.let { { open(it) } }
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReactionButton(Icons.Rounded.ThumbUp, "More like this", reaction > 0, pal) { onReact(Reaction.LIKED) }
            ReactionButton(Icons.Rounded.ThumbDown, "Less like this", reaction < 0, pal) { onReact(Reaction.SKIPPED) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Transcript(
    episode: Episode,
    segment: Segment,
    isCurrent: Boolean,
    player: PlayerState,
    pal: TopicPalette,
    onSeek: (Long) -> Unit
) {
    val timed = segment.lines
    val plain = segment.script
    if (timed.isEmpty() && plain.isEmpty()) return
    var expanded by rememberSaveable(segment.id) { mutableStateOf(false) }
    val following = isCurrent && player.isPlaying
    SectionTitle(
        "Transcript",
        trailing = {
            Icon(if (expanded || following) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        },
        onClick = { expanded = !expanded }
    )
    val lines = if (timed.isNotEmpty()) timed.map { Triple(it.speaker, it.text, it.startMs) } else plain.map { Triple(it.speaker, it.text, -1L) }
    val visible = if (expanded || following) lines.indices else 0 until minOf(3, lines.size)
    visible.forEach { i ->
        val (speaker, text, start) = lines[i]
        val active = isCurrent && player.lineIndex == i
        val bg by animateColorAsState(if (active) pal.soft else Color.Transparent, label = "line")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable(enabled = start >= 0 && isCurrent) { onSeek(start) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                episode.hostName(speaker),
                style = MaterialTheme.typography.labelLarge,
                color = if (speaker == "host") pal.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (active || !following) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
    if (!expanded && !following && lines.size > 3) {
        Text(
            "Show all ${lines.size} lines",
            style = MaterialTheme.typography.labelLarge,
            color = pal.accent,
            modifier = Modifier.clickable { expanded = true }.padding(10.dp)
        )
    }
}

@Composable
private fun TopicChip(topic: String, pal: TopicPalette) {
    Text(
        topic.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = pal.accent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pal.soft)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun SectionTitle(text: String, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 8.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReactionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    pal: TopicPalette,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) pal.accent else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
