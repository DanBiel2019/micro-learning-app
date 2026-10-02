package com.example.aigeneratedandroid.microlearning.narration

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/**
 * Fallback narrator for offline episodes (no studio audio): Android TextToSpeech, one
 * sentence per utterance so pause/resume continues where it stopped. Picks the highest
 * quality voice the device offers rather than the engine default.
 * All callbacks are delivered on the main thread.
 */
class NarrationPlayer(context: Context) {

    var onChunkFinished: ((segmentId: String) -> Unit)? = null
    var onStateChanged: ((playing: Boolean) -> Unit)? = null
    var onSentence: ((index: Int) -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var chunk: NarrationChunk? = null
    private var position = 0
    private var pendingPlay = false

    var isPlaying = false
        private set

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        main.post {
            ready = status == TextToSpeech.SUCCESS
            if (ready) configure()
            if (ready && pendingPlay) {
                pendingPlay = false
                speakFrom(position)
            }
        }
    }

    private fun configure() {
        val best = bestVoice()
        if (best != null) {
            tts.voice = best
        } else {
            val lang = tts.setLanguage(Locale.getDefault())
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }
        }
        tts.setSpeechRate(SPEECH_RATE)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                main.post { handleStart(utteranceId) }
            }

            override fun onDone(utteranceId: String) {
                main.post { handleDone(utteranceId) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                main.post { handleDone(utteranceId) }
            }
        })
    }

    /** English voice with the best quality rating, preferring on-device ones at equal quality. */
    private fun bestVoice(): Voice? = runCatching {
        tts.voices.orEmpty()
            .filter { it.locale.language == "en" && "notinstalled" !in it.features.orEmpty() }
            .maxWithOrNull(
                compareBy<Voice>({ it.quality }, { it.locale.country == Locale.getDefault().country }, { !it.isNetworkConnectionRequired })
            )
    }.getOrNull()

    fun play(next: NarrationChunk) {
        chunk = next
        position = 0
        resume()
    }

    fun resume() {
        if (chunk == null) return
        setPlaying(true)
        if (ready) speakFrom(position) else pendingPlay = true
    }

    fun pause() {
        pendingPlay = false
        setPlaying(false)
        if (ready) tts.stop()
    }

    fun cue(next: NarrationChunk) {
        pause()
        chunk = next
        position = 0
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun speakFrom(start: Int) {
        val c = chunk ?: return
        tts.stop()
        c.segments.drop(start).forEachIndexed { offset, text ->
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "${c.segmentId}#${start + offset}")
        }
    }

    private fun parse(utteranceId: String): Pair<String, Int>? {
        val parts = utteranceId.split("#")
        return parts.getOrNull(1)?.toIntOrNull()?.let { parts[0] to it }
    }

    private fun handleStart(utteranceId: String) {
        val (id, index) = parse(utteranceId) ?: return
        if (id == chunk?.segmentId && isPlaying) onSentence?.invoke(index)
    }

    private fun handleDone(utteranceId: String) {
        val c = chunk ?: return
        val (id, index) = parse(utteranceId) ?: return
        if (id != c.segmentId || !isPlaying) return
        position = index + 1
        if (position >= c.segments.size) {
            position = 0
            setPlaying(false)
            onChunkFinished?.invoke(id)
        }
    }

    private fun setPlaying(playing: Boolean) {
        if (isPlaying == playing) return
        isPlaying = playing
        onStateChanged?.invoke(playing)
    }

    companion object {
        const val SPEECH_RATE = 0.95f
    }
}
