package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.model.PreviousEpisode
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing

/** Everything you can go back to: earlier studio episodes and the built-in classics. */
@Composable
fun LibraryScreen(
    ui: UiState,
    contentPadding: PaddingValues,
    onOpenEpisode: (PreviousEpisode) -> Unit,
    onOpenToday: () -> Unit,
    onOpenClassics: () -> Unit
) {
    StatusBarIcons(overDarkContent = false)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xl)
                    .semantics { heading() }
            )
        }

        val showing = ui.episode
        if (showing != null && ui.todayId != null && showing.id != ui.todayId) {
            item {
                NoticeBanner(
                    "Today shows an earlier episode right now.",
                    icon = Icons.Rounded.History,
                    actionLabel = "Back to today",
                    onAction = onOpenToday,
                    modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, top = Spacing.m)
                )
            }
        }

        item { SectionHeader("Earlier episodes", subtitle = if (ui.history.isEmpty()) null else "${ui.history.size}") }
        if (ui.history.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "Nothing to catch up on yet",
                    body = "Each morning's episode is kept here, so you can replay one or catch up on a day you missed."
                )
            }
        } else {
            items(ui.history, key = { it.id }) { prev ->
                EpisodeRow(prev, downloaded = prev.id in ui.downloaded, onClick = { onOpenEpisode(prev) })
            }
        }

        item { SectionHeader("Classics") }
        item {
            Surface(
                onClick = onOpenClassics,
                modifier = Modifier.padding(horizontal = Spacing.m).fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    Spacer(Modifier.width(Spacing.m))
                    Column(Modifier.weight(1f)) {
                        Text("Your classics library", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A fresh set of ideas from 30 books you love. Works offline, read by your phone's voice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

@Composable
private fun EpisodeRow(prev: PreviousEpisode, downloaded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open episode", onClick = onClick)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s)
            .semantics(mergeDescendants = true) {
                contentDescription = "${formatDate(prev.date)}: ${prev.title}. " +
                    if (downloaded) "Downloaded." else "Needs a connection."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))) {
            CoverArt(prev.id, listOf(prev.title), Modifier.fillMaxSize(), animate = false)
        }
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(formatShortDate(prev.date).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(prev.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (downloaded) Icons.Rounded.DownloadDone else Icons.Rounded.CloudOff,
                    null,
                    Modifier.size(14.dp),
                    tint = if (downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.xxs))
                Text(
                    if (downloaded) "Opens offline" else "Needs a connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
