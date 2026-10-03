package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.data.ListeningStats
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.Segment
import com.example.aigeneratedandroid.microlearning.playback.PlayerState
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing
import com.example.aigeneratedandroid.microlearning.ui.theme.palette

private val HERO_HEIGHT = 400.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    ui: UiState,
    player: PlayerState,
    contentPadding: PaddingValues,
    onPlayEpisode: () -> Unit,
    onOpenSegment: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBackToToday: () -> Unit,
    onDismissNotice: () -> Unit,
    completedKey: (Episode, Segment) -> String
) {
    val episode = ui.episode
    if (episode == null) {
        StatusBarIcons(overDarkContent = false)
        HomeSkeleton(contentPadding)
        return
    }
    val thisPlaying = player.episodeId == episode.id
    val list = rememberLazyListState()
    val heroPx = with(LocalDensity.current) { HERO_HEIGHT.toPx() }
    // Light status-bar icons while the dark hero is behind them, theme icons once scrolled past.
    val overHero by remember { derivedStateOf { list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset < heroPx * 0.85f } }
    StatusBarIcons(overDarkContent = overHero)

    val done = episode.segments.map { completedKey(episode, it) in ui.completed }

    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = ui.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = ui.refreshing,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
            )
        }
    ) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = contentPadding) {
            item(key = "hero") {
                Hero(
                    episode = episode,
                    done = done,
                    playing = thisPlaying && player.isPlaying,
                    started = thisPlaying,
                    currentIndex = if (thisPlaying) player.segmentIndex else -1,
                    onPlay = onPlayEpisode
                )
            }

            if (ui.todayId != null && ui.todayId != episode.id) {
                item(key = "earlier") {
                    NoticeBanner(
                        "You're listening to ${if (episode.offline) "a set from your classics library" else "the episode from ${formatShortDate(episode.date)}"}.",
                        icon = Icons.Rounded.History,
                        actionLabel = "Back to today",
                        onAction = onBackToToday,
                        modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, top = Spacing.m)
                    )
                }
            }
            ui.notice?.let { notice ->
                item(key = "notice") {
                    NoticeBanner(
                        notice.text,
                        actionLabel = if (notice.canRetry) "Try again" else null,
                        onAction = if (notice.canRetry) onRefresh else null,
                        onDismiss = onDismissNotice,
                        modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, top = Spacing.m)
                    )
                }
            }
            if (ui.stats.ideasHeard > 0) {
                item(key = "stats") { StatsStrip(ui.stats) }
            }

            item(key = "header") {
                SectionHeader("In this episode", subtitle = "${episode.segments.size} ideas · ${episodeMinutes(episode)} min")
            }
            itemsIndexed(episode.segments, key = { _, s -> s.id }) { i, seg ->
                val current = thisPlaying && player.segmentIndex == i
                SegmentRow(
                    index = i,
                    count = episode.segments.size,
                    segment = seg,
                    current = current,
                    playing = current && player.isPlaying,
                    progress = if (current && player.durationMs > 0) player.positionMs.toFloat() / player.durationMs else 0f,
                    done = done[i],
                    onClick = { onOpenSegment(i) }
                )
            }

            item(key = "footer") {
                Text(
                    "A new studio episode arrives every morning. Every idea cites its sources.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.xl)
                )
            }
        }
    }
}

@Composable
private fun Hero(
    episode: Episode,
    done: List<Boolean>,
    playing: Boolean,
    started: Boolean,
    currentIndex: Int,
    onPlay: () -> Unit
) {
    Box(Modifier.fillMaxWidth().heightIn(min = HERO_HEIGHT)) {
        CoverArt(episode.id, episode.segments.map { it.topic }, Modifier.matchParentSize())
        // Bottom scrim for legible text, top scrim so status-bar icons stay readable over any art.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f)))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl, top = 120.dp)
        ) {
            Text(
                "${greeting()} · ${formatDate(episode.date)}".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                episode.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )
            if (episode.theme.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(episode.theme, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f))
            }
            Spacer(Modifier.height(Spacing.m))
            IdeaProgress(episode, done, currentIndex)
            Spacer(Modifier.height(Spacing.l))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF15120F)),
                    contentPadding = PaddingValues(start = Spacing.m, end = Spacing.l, top = Spacing.s, bottom = Spacing.s),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        when {
                            playing -> "Pause"
                            started -> "Resume"
                            else -> "Play episode"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.width(Spacing.m))
                Column {
                    Text("${episodeMinutes(episode)} min · ${episode.segments.size} ideas", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text(
                        if (episode.offline) "Read by your phone's voice" else "with ${episode.hostName("host")} & ${episode.hostName("cohost")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/** One pill per idea, in its topic colour: solid when heard, outlined while playing. */
@Composable
private fun IdeaProgress(episode: Episode, done: List<Boolean>, currentIndex: Int) {
    val heard = done.count { it }
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$heard of ${episode.segments.size} ideas heard" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        episode.segments.forEachIndexed { i, seg ->
            val accent = com.example.aigeneratedandroid.microlearning.ui.theme.topicPalette(seg.topic, dark = true).accent
            Box(
                Modifier
                    .weight(1f)
                    .height(if (i == currentIndex) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            done.getOrElse(i) { false } -> accent
                            i == currentIndex -> accent.copy(alpha = 0.7f)
                            else -> Color.White.copy(alpha = 0.25f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun StatsStrip(stats: ListeningStats) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.m, end = Spacing.m, top = Spacing.m),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (stats.heardToday) "🔥" else "☕", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { })
            Spacer(Modifier.width(Spacing.s))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        stats.streakDays > 1 -> "${stats.streakDays}-day streak"
                        stats.heardToday -> "You listened today"
                        stats.streakDays == 1 -> "Keep yesterday's streak going"
                        else -> "Ready when you are"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${stats.ideasHeard} ideas heard · ${stats.minutesHeard} min listened",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SegmentRow(
    index: Int,
    count: Int,
    segment: Segment,
    current: Boolean,
    playing: Boolean,
    progress: Float,
    done: Boolean,
    onClick: () -> Unit
) {
    val pal = palette(segment.topic)
    val status = when {
        playing -> "Playing now. "
        current -> "Paused. "
        done -> "Listened. "
        else -> ""
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open idea", onClick = onClick)
            .background(if (current) pal.soft else Color.Transparent)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s)
            .semantics(mergeDescendants = true) {
                contentDescription = "Idea ${index + 1} of $count: ${segment.title}. ${segment.topic}." +
                    (segment.durationMs?.let { " ${spokenDuration(it)}." } ?: "") + " $status"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentGlyph(segment, pal.soft, size = 56.dp, corner = 16.dp)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(
                (segment.kicker.ifBlank { segment.topic }).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = pal.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(segment.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(segment.durationMs?.let(::formatDuration), segment.topic, segment.source.author.ifBlank { null })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (current && progress > 0f) {
                Spacer(Modifier.height(Spacing.xs))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = pal.accent,
                    trackColor = pal.soft,
                    drawStopIndicator = {}
                )
            }
        }
        Spacer(Modifier.width(Spacing.s))
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when {
                playing -> Equalizer(pal.accent)
                done -> Box(
                    Modifier.size(24.dp).clip(CircleShape).background(pal.accent),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp)) }
                else -> Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Skeleton of the Today screen while the first episode loads (NN/g: skeletons for full-page loads). */
@Composable
private fun HomeSkeleton(contentPadding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .semantics { contentDescription = "Loading today's episode" }
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        SkeletonBlock(
            Modifier.padding(Spacing.m).fillMaxWidth().height(HERO_HEIGHT - 60.dp),
            RoundedCornerShape(28.dp)
        )
        SkeletonBlock(Modifier.padding(start = Spacing.gutter, top = Spacing.l).width(180.dp).height(24.dp))
        repeat(4) {
            Row(Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(56.dp), RoundedCornerShape(16.dp))
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(1f)) {
                    SkeletonBlock(Modifier.width(90.dp).height(10.dp))
                    Spacer(Modifier.height(Spacing.xs))
                    SkeletonBlock(Modifier.fillMaxWidth(0.85f).height(16.dp))
                    Spacer(Modifier.height(Spacing.xs))
                    SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(10.dp))
                }
            }
        }
    }
}
