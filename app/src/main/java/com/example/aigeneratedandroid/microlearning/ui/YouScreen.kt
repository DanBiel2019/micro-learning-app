package com.example.aigeneratedandroid.microlearning.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.data.ListeningStats
import com.example.aigeneratedandroid.microlearning.playback.EpisodePlayer
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing

/** A row on the You tab that leads somewhere else (the extension point for new screens). */
data class YouEntry(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

/** Your listening, playback preferences and about. */
@Composable
fun YouScreen(
    stats: ListeningStats,
    speed: Float,
    feedRepo: String,
    appVersion: String,
    contentPadding: PaddingValues,
    onSpeed: (Float) -> Unit,
    entries: List<YouEntry>,
    /** Opens the infographic review gallery; listed last under About, out of the way. */
    onOpenInfographicGallery: (() -> Unit)? = null
) {
    StatusBarIcons(overDarkContent = false)
    val context = LocalContext.current
    fun open(url: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
    ) {
        Text(
            "You",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xl)
                .semantics { heading() }
        )

        SectionHeader("Your listening")
        Row(
            Modifier.padding(horizontal = Spacing.m).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            StatTile("${stats.streakDays}", "day streak", Modifier.weight(1f))
            StatTile("${stats.ideasHeard}", "ideas heard", Modifier.weight(1f))
            StatTile("${stats.minutesHeard}", "minutes", Modifier.weight(1f))
        }
        Text(
            if (stats.ideasHeard == 0) "Finish an idea to start your streak. Stats stay on this phone."
            else "A streak counts every day you finish at least one idea. Stats stay on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.s)
        )

        SectionHeader("Playback")
        var speedMenu by remember { mutableStateOf(false) }
        Box {
            SettingRow(
                icon = Icons.Rounded.Speed,
                title = "Playback speed",
                subtitle = "Studio episodes · ${speedLabel(speed)}",
                onClick = { speedMenu = true }
            )
            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                EpisodePlayer.SPEEDS.sorted().forEach { s ->
                    DropdownMenuItem(
                        text = { Text(speedLabel(s) + if (s == speed) "  ✓" else "") },
                        onClick = { onSpeed(s); speedMenu = false }
                    )
                }
            }
        }

        if (entries.isNotEmpty()) {
            SectionHeader("Explore")
            entries.forEach { e -> SettingRow(e.icon, e.title, e.subtitle, onClick = e.onClick) }
        }

        SectionHeader("About")
        SettingRow(
            icon = Icons.Rounded.Verified,
            title = "How episodes are made",
            subtitle = "Each morning two AI hosts talk through five researched ideas. Every idea cites its sources; tap a source to read it."
        )
        SettingRow(
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            title = "Episode feed",
            subtitle = "github.com/$feedRepo",
            onClick = { open("https://github.com/$feedRepo/releases") }
        )
        SettingRow(icon = Icons.Rounded.Info, title = "Version", subtitle = appVersion)
        if (onOpenInfographicGallery != null) {
            SettingRow(
                icon = Icons.Rounded.Dashboard,
                title = "Infographic gallery",
                subtitle = "Every visual style with sample data, for design review",
                onClick = onOpenInfographicGallery
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}

fun speedLabel(speed: Float): String = if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${speed}×"

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$value $label" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(Spacing.m)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = Spacing.gutter, vertical = Spacing.s)
                .semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(Modifier.padding(start = 60.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
