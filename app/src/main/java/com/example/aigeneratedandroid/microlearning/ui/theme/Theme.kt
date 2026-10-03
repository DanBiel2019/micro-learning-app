package com.example.aigeneratedandroid.microlearning.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

/** Editorial serif for headlines; Inter for everything you read or tap. */
val Display = FontFamily(fraunces(400), fraunces(600), fraunces(700))
val Body = FontFamily(inter(400), inter(500), inter(600), inter(700))

/**
 * Type scale, following the Material 3 roles (display / headline / title / body / label).
 * All sizes are in sp so they follow the system font-size setting.
 */
private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.08.em)
)

/** Material 3 shape scale: small things get small radii, sheets and heroes get large ones. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** One spacing scale (4dp grid) used by every screen. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Horizontal page margin. */
    val gutter = 20.dp
}

// Warm editorial palette. Every role is set so stock M3 components (navigation bar
// indicator, slider, sheets, snackbars) pick up the brand instead of the baseline purple.
// Text-bearing roles meet WCAG AA (4.5:1) against background and surface.
// surface / outline / outlineVariant keep their previous values because Infographic.kt
// (owned by the infographic work) draws its cards and chips with them; outline is therefore
// a soft divider tone here, not the M3 3:1 boundary tone.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB4481B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3A0F00),
    inversePrimary = Color(0xFFFFB693),
    secondary = Color(0xFF77574A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6DDD0),
    onSecondaryContainer = Color(0xFF2C160C),
    tertiary = Color(0xFF1B66C2),
    onTertiary = Color.White,
    background = Color(0xFFF7F3EC),
    onBackground = Color(0xFF1B1712),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1712),
    surfaceVariant = Color(0xFFEFE8DD),
    onSurfaceVariant = Color(0xFF625A4F),
    surfaceTint = Color(0xFFB4481B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF9F4),
    surfaceContainer = Color(0xFFF2ECE3),
    surfaceContainerHigh = Color(0xFFECE5DB),
    surfaceContainerHighest = Color(0xFFE6DFD4),
    inverseSurface = Color(0xFF33302B),
    inverseOnSurface = Color(0xFFF7F0E7),
    outline = Color(0xFFE3DACC),
    outlineVariant = Color(0xFFEDE5D8),
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A4C),
    onPrimary = Color(0xFF2A1205),
    primaryContainer = Color(0xFF5A2410),
    onPrimaryContainer = Color(0xFFFFDBCB),
    inversePrimary = Color(0xFFB4481B),
    secondary = Color(0xFFE7BDAD),
    onSecondary = Color(0xFF442A1F),
    secondaryContainer = Color(0xFF45322A),
    onSecondaryContainer = Color(0xFFF6DDD0),
    tertiary = Color(0xFF6CB4FF),
    onTertiary = Color(0xFF00315C),
    background = Color(0xFF12100E),
    onBackground = Color(0xFFF3EDE4),
    surface = Color(0xFF1C1916),
    onSurface = Color(0xFFF3EDE4),
    surfaceVariant = Color(0xFF27221D),
    onSurfaceVariant = Color(0xFFB0A699),
    surfaceTint = Color(0xFFFF8A4C),
    surfaceContainerLowest = Color(0xFF0D0B0A),
    surfaceContainerLow = Color(0xFF1A1714),
    surfaceContainer = Color(0xFF201C19),
    surfaceContainerHigh = Color(0xFF2A2521),
    surfaceContainerHighest = Color(0xFF35302A),
    inverseSurface = Color(0xFFE9E1D8),
    inverseOnSurface = Color(0xFF33302B),
    outline = Color(0xFF332D27),
    outlineVariant = Color(0xFF2B2622),
    scrim = Color.Black
)

/** One accent per topic family so segments are recognisable at a glance. */
@Immutable
data class TopicPalette(val accent: Color, val soft: Color, val onSoft: Color)

private data class TopicHues(val light: Color, val dark: Color)

// Light accents are dark enough to be used as text on the cream background (>= 4.5:1).
private val TOPIC_HUES = listOf(
    listOf("system", "measure") to TopicHues(Color(0xFF0E7474), Color(0xFF4FD1C5)),
    listOf("tech", "ai", "comput") to TopicHues(Color(0xFF4F46E5), Color(0xFF9198FF)),
    listOf("business", "entrepreneur", "econom") to TopicHues(Color(0xFF935A08), Color(0xFFF5B544)),
    listOf("leader", "communicat") to TopicHues(Color(0xFFB83A62), Color(0xFFF284A6)),
    listOf("creativ", "design") to TopicHues(Color(0xFF8045C9), Color(0xFFC29BFF)),
    listOf("fresh", "network", "reliab") to TopicHues(Color(0xFF1B66C2), Color(0xFF6CB4FF)),
    listOf("medic", "health", "science", "psych") to TopicHues(Color(0xFF267A41), Color(0xFF6FD08C)),
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

/**
 * True when the user turned on "Remove animations" (animator duration scale 0). Decorative,
 * looping motion (cover-art drift, equalizer bars, shimmer) stops when it is set.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun MicroLearningTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(LocalDarkTheme provides dark, LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

@Composable
fun palette(topic: String): TopicPalette = topicPalette(topic, LocalDarkTheme.current)
