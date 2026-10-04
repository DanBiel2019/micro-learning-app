package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.aigeneratedandroid.microlearning.model.Visual
import com.example.aigeneratedandroid.microlearning.ui.infographic.BarsGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.BeforeAfterGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.BigNumberGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.CompareGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.CycleGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.FlowGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.FunnelGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.IcebergGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.LadderGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.MatrixGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.QuoteGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.SpectrumGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.StatsGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.TimelineGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.VennGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.WaffleGraphic
import com.example.aigeneratedandroid.microlearning.ui.infographic.describe
import com.example.aigeneratedandroid.microlearning.ui.infographic.ink
import com.example.aigeneratedandroid.microlearning.ui.infographic.resolveKind
import com.example.aigeneratedandroid.microlearning.ui.theme.LocalReducedMotion
import com.example.aigeneratedandroid.microlearning.ui.theme.TopicPalette

/**
 * Draws a segment's infographic from its [Visual] spec. Specs come from the content pipeline,
 * so every new episode gets illustrations without shipping new artwork.
 *
 * Every kind shares one frame, built on editorial practice: a headline that states the idea
 * (an "action title"), the graphic with its labels placed directly on the marks, one accent
 * colour reserved for the punchline item, and the caption set as the takeaway annotation.
 * Kinds live in ui/infographic/; [resolveKind] picks a fallback when a spec doesn't fit its kind.
 */
@Composable
fun Infographic(visual: Visual, palette: TopicPalette, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val reveal = remember(visual, reduced) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(visual, reduced) {
        if (!reduced) reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val ink = ink(palette)
    val description = remember(visual) { describe(visual) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Infographic. $description" },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp)) {
            if (visual.title.isNotBlank()) {
                Text(visual.title, style = MaterialTheme.typography.titleLarge, color = ink.text)
                Spacer(Modifier.height(18.dp))
            }
            val items = visual.items
            val p = reveal.value
            if (items.isNotEmpty()) {
                when (resolveKind(visual)) {
                    "flow" -> FlowGraphic(items, ink, p)
                    "cycle" -> CycleGraphic(items, ink, p)
                    "compare" -> CompareGraphic(items, ink, p)
                    "stats" -> StatsGraphic(items, ink, p)
                    "bars" -> BarsGraphic(items, ink, p)
                    "venn" -> VennGraphic(items, ink, p)
                    "ladder" -> LadderGraphic(items, ink, p, visual.yAxis)
                    "timeline" -> TimelineGraphic(items, ink, p)
                    "quote" -> QuoteGraphic(items, ink, p)
                    "before_after" -> BeforeAfterGraphic(items, ink, p)
                    "matrix" -> MatrixGraphic(items, ink, p, visual.xAxis, visual.yAxis)
                    "iceberg" -> IcebergGraphic(items, ink, p)
                    "funnel" -> FunnelGraphic(items, ink, p)
                    "spectrum" -> SpectrumGraphic(items, ink, p, visual.xAxis)
                    "waffle" -> WaffleGraphic(items, ink, p)
                    "big_number" -> BigNumberGraphic(items, ink, p)
                    else -> FlowGraphic(items, ink, p)
                }
            }
            if (visual.caption.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                // The takeaway, annotated like an editor's note: an accent rule and plain words.
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(ink.accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        visual.caption,
                        style = MaterialTheme.typography.titleSmall,
                        color = ink.text,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
