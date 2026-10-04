package com.example.aigeneratedandroid.microlearning.ui.infographic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.ui.Infographic
import com.example.aigeneratedandroid.microlearning.ui.theme.Spacing
import com.example.aigeneratedandroid.microlearning.ui.theme.palette

/**
 * A review screen (reached from You > About) that renders every infographic kind with sample
 * data, its when-to-use rule and the kind id the generator writes, plus the fallbacks for
 * specs that don't fit. It is for checking designs on a real phone, not for everyday use.
 */
@Composable
fun InfographicGalleryScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    var onlyNew by rememberSaveable { mutableStateOf(false) }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    val guides = GallerySamples.guides.filter { !onlyNew || it.isNew }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = Spacing.xs)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                    Text(
                        "Infographic gallery",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    IconButton(onClick = { replay++ }) { Icon(Icons.Rounded.Replay, "Replay animations") }
                }
                Text(
                    "Every visual style the daily episode can use, drawn with sample data. " +
                        "Each one has a headline, labels on the marks, and one accent colour for the punchline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.xs)
                )
                Row(Modifier.padding(horizontal = Spacing.gutter), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FilterChip(selected = !onlyNew, onClick = { onlyNew = false }, label = { Text("All ${GallerySamples.guides.size}") })
                    FilterChip(
                        selected = onlyNew,
                        onClick = { onlyNew = true },
                        label = { Text("New ${GallerySamples.guides.count { it.isNew }}") }
                    )
                }
            }
        }
        items(guides, key = { it.kind }) { guide ->
            Column(Modifier.padding(horizontal = Spacing.m)) {
                Row(Modifier.padding(horizontal = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                    Text(guide.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        guide.kind,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (guide.isNew) {
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    guide.whenToUse,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xxs, vertical = Spacing.xs)
                )
                key(replay) { Infographic(guide.sample, palette(guide.topic)) }
            }
        }
        if (!onlyNew) {
            item {
                Column(Modifier.padding(horizontal = Spacing.gutter)) {
                    Text("Fallbacks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(
                        "Specs are written by a small model each night. When one doesn't fit its kind, " +
                            "the app draws the closest kind that does.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(GallerySamples.fallbacks, key = { it.second.title }) { (note, visual) ->
                Column(Modifier.padding(horizontal = Spacing.m)) {
                    Text(
                        "$note  (drawn as ${resolveKind(visual)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.xxs, vertical = Spacing.xs)
                    )
                    key(replay) { Infographic(visual, palette("Systems & Measurement")) }
                }
            }
            item { Spacer(Modifier.height(Spacing.m)) }
        }
    }
}
