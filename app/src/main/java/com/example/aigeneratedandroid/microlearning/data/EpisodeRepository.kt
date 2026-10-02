package com.example.aigeneratedandroid.microlearning.data

import android.content.Context
import com.example.aigeneratedandroid.BuildConfig
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.Segment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where episodes come from. A GitHub Actions job publishes a new studio episode every morning
 * as a GitHub Release (feed.json + one MP3 per segment); this downloads and caches them so the
 * app works offline, and keeps the bundled library as a last resort.
 */
class EpisodeRepository(private val context: Context) {

    private val episodesDir = File(context.filesDir, "episodes").apply { mkdirs() }
    private val audioDir = File(context.filesDir, "audio").apply { mkdirs() }

    val library: List<Segment> by lazy {
        context.assets.open("library.json").bufferedReader().use {
            json.decodeFromString(ListSerializer(Segment.serializer()), it.readText())
        }
    }

    /** Fetches the newest published episode and caches it. Returns null when offline. */
    suspend fun refreshLatest(): Episode? = withContext(Dispatchers.IO) {
        runCatching { fetchEpisode(LATEST_FEED_URL) }.getOrNull()
    }

    /** Fetches a specific past episode (from [Episode.previous]) and caches it. */
    suspend fun fetchEpisode(feedUrl: String): Episode = withContext(Dispatchers.IO) {
        val episode = json.decodeFromString(Episode.serializer(), download(feedUrl).decodeToString())
        File(episodesDir, "${episode.id}.json").writeText(json.encodeToString(Episode.serializer(), episode))
        episode
    }

    /** Studio episodes already on the device, newest first. */
    fun cachedEpisodes(): List<Episode> =
        episodesDir.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { f -> runCatching { json.decodeFromString(Episode.serializer(), f.readText()) }.getOrNull() }
            .sortedByDescending { it.date }

    fun cachedEpisode(id: String): Episode? =
        File(episodesDir, "$id.json").takeIf { it.exists() }
            ?.let { runCatching { json.decodeFromString(Episode.serializer(), it.readText()) }.getOrNull() }

    /** Local copy of a segment's audio if it has been downloaded. */
    fun localAudio(episode: Episode, index: Int): File? =
        File(File(audioDir, episode.id), "seg-${index + 1}.mp3").takeIf { it.exists() && it.length() > 0 }

    /** Best URI to play: the downloaded file when present, otherwise stream it. */
    fun playableUri(episode: Episode, index: Int): String? =
        localAudio(episode, index)?.toURI()?.toString() ?: episode.segments[index].audioUrl

    /** Downloads every segment's audio so the episode plays offline. Skips what's already there. */
    suspend fun downloadAudio(episode: Episode) = withContext(Dispatchers.IO) {
        val dir = File(audioDir, episode.id).apply { mkdirs() }
        episode.segments.forEachIndexed { i, seg ->
            val url = seg.audioUrl ?: return@forEachIndexed
            val target = File(dir, "seg-${i + 1}.mp3")
            if (target.exists() && target.length() > 0) return@forEachIndexed
            val tmp = File(dir, "seg-${i + 1}.part")
            tmp.writeBytes(download(url))
            tmp.renameTo(target)
        }
    }

    /** Keeps the newest [keep] episodes' audio; feeds (a few KB each) are kept for history. */
    fun pruneAudio(keep: Int = 10) {
        val keepIds = cachedEpisodes().take(keep).map { it.id }.toSet()
        audioDir.listFiles().orEmpty().filter { it.name !in keepIds }.forEach { it.deleteRecursively() }
    }

    private fun download(url: String): ByteArray {
        var current = URL(url)
        repeat(5) {
            val conn = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "MicroLearning/${BuildConfig.VERSION_NAME}")
            }
            try {
                when (val code = conn.responseCode) {
                    in 200..299 -> return conn.inputStream.use { it.readBytes() }
                    in 300..399 -> current = URL(current, conn.getHeaderField("Location"))
                    else -> throw IOException("HTTP $code for $current")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects for $url")
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        val LATEST_FEED_URL = "https://github.com/${BuildConfig.FEED_REPO}/releases/latest/download/feed.json"
    }
}
