package com.example.aigeneratedandroid.microlearning.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.aigeneratedandroid.R

@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int) = Font(
    R.font.fraunces, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Display = FontFamily(fraunces(400), fraunces(600), fraunces(700))
val Body = FontFamily(inter(400), inter(500), inter(600), inter(700))

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.01).em),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.08.em),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.08.em)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFD2551E),
    onPrimary = Color.White,
    background = Color(0xFFF7F3EC),
    onBackground = Color(0xFF1B1712),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1712),
    surfaceVariant = Color(0xFFEFE8DD),
    onSurfaceVariant = Color(0xFF6E655A),
    surfaceContainer = Color(0xFFFFFCF7),
    outline = Color(0xFFE3DACC),
    outlineVariant = Color(0xFFEDE5D8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A4C),
    onPrimary = Color(0xFF2A1205),
    background = Color(0xFF12100E),
    onBackground = Color(0xFFF3EDE4),
    surface = Color(0xFF1C1916),
    onSurface = Color(0xFFF3EDE4),
    surfaceVariant = Color(0xFF27221D),
    onSurfaceVariant = Color(0xFFA89E91),
    surfaceContainer = Color(0xFF221E1A),
    outline = Color(0xFF332D27),
    outlineVariant = Color(0xFF2B2622)
)

/** One accent per topic family so segments are recognisable at a glance. */
@Immutable
data class TopicPalette(val accent: Color, val soft: Color, val onSoft: Color)

private data class TopicHues(val light: Color, val dark: Color)

private val TOPIC_HUES = listOf(
    listOf("system", "measure") to TopicHues(Color(0xFF0F7F7F), Color(0xFF4FD1C5)),
    listOf("tech", "ai", "comput") to TopicHues(Color(0xFF4F46E5), Color(0xFF9198FF)),
    listOf("business", "entrepreneur", "econom") to TopicHues(Color(0xFFB7700A), Color(0xFFF5B544)),
    listOf("leader", "communicat") to TopicHues(Color(0xFFC2416B), Color(0xFFF284A6)),
    listOf("creativ", "design") to TopicHues(Color(0xFF8B4FD6), Color(0xFFC29BFF)),
    listOf("fresh", "network", "reliab") to TopicHues(Color(0xFF1E6FD0), Color(0xFF6CB4FF)),
    listOf("medic", "health", "science", "psych") to TopicHues(Color(0xFF2F8F4E), Color(0xFF6FD08C)),
    listOf("history", "aviation", "military", "space") to TopicHues(Color(0xFFA2522B), Color(0xFFE9A07A))
)

private val FALLBACK_HUE = TopicHues(Color(0xFF5B6B7A), Color(0xFFA9B8C6))

fun topicPalette(topic: String, dark: Boolean): TopicPalette {
    val t = topic.lowercase()
    val hues = TOPIC_HUES.firstOrNull { (keys, _) -> keys.any { it in t } }?.second ?: FALLBACK_HUE
    val accent = if (dark) hues.dark else hues.light
    return if (dark) {
        TopicPalette(accent, accent.copy(alpha = 0.16f), accent)
    } else {
        TopicPalette(accent, accent.copy(alpha = 0.10f), accent)
    }
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MicroLearningTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}

@Composable
fun palette(topic: String): TopicPalette = topicPalette(topic, LocalDarkTheme.current)
