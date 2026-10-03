package com.example.aigeneratedandroid.microlearning.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.aigeneratedandroid.microlearning.data.EpisodeRepository
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.narration.NarrationFormatter
import com.example.aigeneratedandroid.microlearning.narration.NarrationPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val episodeId: String? = null,
    val segmentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /** Index into the segment's transcript lines currently being spoken, or -1. */
    val lineIndex: Int = -1,
    val speed: Float = 1f,
    /** True when the episode has no studio audio and the device voice is reading it. */
    val deviceVoice: Boolean = false
)

/**
 * One playback surface for the UI. Studio episodes stream (or play downloaded) MP3s through
 * the Media3 session in [PlaybackService]; offline episodes fall back to device TTS.
 */
class EpisodePlayer(
    private val context: Context,
    private val repo: EpisodeRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Called when a segment plays to its end (not when skipped). */
    var onSegmentCompleted: ((episode: Episode, index: Int) -> Unit)? = null

    private var episode: Episode? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private val tts by lazy { NarrationPlayer(context).also(::wireTts) }
    private var ttsChunks = emptyList<com.example.aigeneratedandroid.microlearning.narration.NarrationChunk>()

    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, token).buildAsync().await().also { c ->
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startTicker()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val ep = episode ?: return
                    val previous = _state.value.segmentIndex
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) onSegmentCompleted?.invoke(ep, previous)
                    _state.update { it.copy(segmentIndex = c.currentMediaItemIndex, lineIndex = -1, positionMs = 0) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val ep = episode ?: return
                    if (playbackState == Player.STATE_ENDED) onSegmentCompleted?.invoke(ep, c.currentMediaItemIndex)
                }
            })
            // Re-attach to an episode that kept playing while the activity was gone.
            if (c.mediaItemCount > 0) {
                val id = c.getMediaItemAt(0).mediaId.substringBefore('/')
                _state.update { it.copy(episodeId = id, segmentIndex = c.currentMediaItemIndex, isPlaying = c.isPlaying) }
                startTicker()
            }
        }
    }

    /** Loads [ep] (if it isn't already) and positions at [index]; starts playing when [play]. */
    fun load(ep: Episode, index: Int, play: Boolean) {
        val sameEpisode = episode?.id == ep.id
        episode = ep
        if (ep.hasAudio) {
            tts.pause()
            val c = controller ?: return
            val loaded = c.mediaItemCount == ep.segments.size &&
                c.getMediaItemAt(0).mediaId == "${ep.id}/${ep.segments[0].id}"
            if (!loaded) {
                c.setMediaItems(ep.segments.indices.map { mediaItem(ep, it) }, index, 0)
                c.prepare()
            } else if (c.currentMediaItemIndex != index) {
                c.seekTo(index, 0)
            }
            _state.update { it.copy(episodeId = ep.id, segmentIndex = index, deviceVoice = false, lineIndex = -1) }
            if (play) c.play()
        } else {
            controller?.pause()
            if (!sameEpisode || ttsChunks.size != ep.segments.size) ttsChunks = NarrationFormatter.format(ep)
            _state.update { it.copy(episodeId = ep.id, segmentIndex = index, deviceVoice = true, lineIndex = -1, positionMs = 0, durationMs = 0) }
            if (play) tts.play(ttsChunks[index]) else tts.cue(ttsChunks[index])
        }
    }

    /** Binds [ep] to whatever the session is already playing, without changing playback. */
    fun attach(ep: Episode) {
        if (_state.value.episodeId == ep.id) episode = ep
    }

    fun toggle() {
        val ep = episode ?: return
        if (ep.hasAudio) {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        } else {
            if (tts.isPlaying) tts.pause() else tts.resume()
        }
    }

    fun pause() {
        controller?.pause()
        if (episode?.hasAudio == false) tts.pause()
    }

    fun skipToSegment(index: Int) {
        val ep = episode ?: return
        if (index !in ep.segments.indices) return
        load(ep, index, play = _state.value.isPlaying)
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        if (episode?.hasAudio != true) return
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, c.duration.coerceAtLeast(0)))
    }

    /** Sets an exact speed (one of [SPEEDS]); used by the Now Playing speed picker. */
    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
        _state.update { it.copy(speed = speed) }
    }

    fun seekTo(positionMs: Long) {
        if (episode?.hasAudio == true) controller?.seekTo(positionMs)
    }

    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(_state.value.speed) + 1) % SPEEDS.size]
        controller?.playbackParameters = PlaybackParameters(next)
        _state.update { it.copy(speed = next) }
    }

    fun release() {
        ticker?.cancel()
        controller?.release()
        controller = null
        tts.shutdown()
    }

    private fun mediaItem(ep: Episode, index: Int): MediaItem {
        val seg = ep.segments[index]
        return MediaItem.Builder()
            .setMediaId("${ep.id}/${seg.id}")
            .setUri(Uri.parse(repo.playableUri(ep, index)))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(seg.title)
                    .setArtist("${ep.hostName("host")} & ${ep.hostName("cohost")}")
                    .setAlbumTitle(ep.title)
                    .setTrackNumber(index + 1)
                    .setTotalTrackCount(ep.segments.size)
                    .apply { Artwork.forSegment(ep, index)?.let { setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) } }
                    .build()
            )
            .build()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val c = controller ?: break
                val ep = episode
                val index = c.currentMediaItemIndex
                val pos = c.currentPosition
                val lines = ep?.segments?.getOrNull(index)?.lines.orEmpty()
                val line = lines.indexOfLast { it.startMs <= pos }
                _state.update {
                    it.copy(
                        segmentIndex = if (ep?.hasAudio == true) index else it.segmentIndex,
                        positionMs = pos,
                        durationMs = c.duration.coerceAtLeast(0),
                        lineIndex = line,
                        isPlaying = c.isPlaying
                    )
                }
                if (!c.isPlaying) break
                delay(250)
            }
        }
    }

    private fun wireTts(player: NarrationPlayer) {
        player.onStateChanged = { playing -> _state.update { it.copy(isPlaying = playing) } }
        player.onSentence = { i -> _state.update { it.copy(lineIndex = i) } }
        player.onChunkFinished = {
            val ep = episode
            val index = _state.value.segmentIndex
            if (ep != null) {
                onSegmentCompleted?.invoke(ep, index)
                if (index < ep.segments.lastIndex) load(ep, index + 1, play = true)
            }
        }
    }

    companion object {
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 0.85f)
    }
}
