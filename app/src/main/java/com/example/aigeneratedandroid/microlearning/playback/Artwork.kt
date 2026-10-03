package com.example.aigeneratedandroid.microlearning.playback

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.ui.theme.topicPalette
import java.io.ByteArrayOutputStream
import kotlin.random.Random

/**
 * Square artwork for the media notification, lock screen, Bluetooth/car displays: the
 * segment's topic colour as a deep gradient with the app's sunrise-and-ripples mark, so a
 * glance at the lock screen tells you which idea is playing. Pure android.graphics so it can
 * run without a Compose UI.
 */
internal object Artwork {

    private const val SIZE = 512
    private val cache = object : LinkedHashMap<String, ByteArray>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?) = size > 12
    }

    @Synchronized
    fun forSegment(episode: Episode, index: Int): ByteArray? = runCatching {
        val seg = episode.segments[index]
        cache.getOrPut("${episode.id}/$index") { render(seed = "${episode.id}/${seg.id}", topic = seg.topic) }
    }.getOrNull()

    private fun render(seed: String, topic: String): ByteArray {
        val accent = topicPalette(topic, dark = true).accent.toArgb()
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = SIZE.toFloat()
        val rnd = Random(seed.hashCode())

        // Deep wash from a darkened accent to near-black.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, s, s, blend(accent, 0xFF000000.toInt(), 0.62f), 0xFF0E0C0B.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, s, s, paint)

        // Two soft glows.
        repeat(2) {
            val cx = s * (0.2f + rnd.nextFloat() * 0.6f)
            val cy = s * (0.15f + rnd.nextFloat() * 0.5f)
            val r = s * (0.45f + rnd.nextFloat() * 0.2f)
            paint.shader = RadialGradient(cx, cy, r, withAlpha(accent, 0.55f), withAlpha(accent, 0f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, paint)
        }
        paint.shader = null

        // The app mark: sun over a horizon with two ripples, centred low.
        val cx = s / 2
        val horizon = s * 0.6f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = s * 0.03f
            color = 0xFFFFFFFF.toInt()
        }
        listOf(0.30f to 0.35f, 0.22f to 0.65f).forEach { (r, a) ->
            stroke.alpha = (a * 255).toInt()
            val rr = s * r
            c.drawArc(RectF(cx - rr, horizon - rr, cx + rr, horizon + rr), 180f, 180f, false, stroke)
        }
        paint.color = 0xFFFFFFFF.toInt()
        val sun = s * 0.14f
        c.drawArc(RectF(cx - sun, horizon - sun, cx + sun, horizon + sun), 180f, 180f, true, paint)
        stroke.alpha = 255
        c.drawLine(cx - s * 0.26f, horizon + s * 0.06f, cx + s * 0.26f, horizon + s * 0.06f, stroke)
        stroke.alpha = 150
        c.drawLine(cx - s * 0.13f, horizon + s * 0.13f, cx + s * 0.13f, horizon + s * 0.13f, stroke)

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
        bmp.recycle()
        return out.toByteArray()
    }

    private fun withAlpha(color: Int, alpha: Float): Int = (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt() and 0xFF
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
