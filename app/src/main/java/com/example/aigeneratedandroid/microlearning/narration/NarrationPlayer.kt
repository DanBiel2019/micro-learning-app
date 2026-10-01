package com.example.aigeneratedandroid.microlearning.narration

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Thin wrapper over Android TextToSpeech that plays one [NarrationChunk] at a time,
 * a sentence per utterance, so pause/resume continues from the sentence it stopped on.
 * All callbacks are delivered on the main thread.
 */
class NarrationPlayer(context: Context) {

    var onChunkFinished: ((cardId: String) -> Unit)? = null
    var onStateChanged: ((playing: Boolean) -> Unit)? = null
    var onReadyChanged: ((ready: Boolean) -> Unit)? = null

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
            onReadyChanged?.invoke(ready)
            if (ready && pendingPlay) {
                pendingPlay = false
                speakFrom(position)
            }
        }
    }

    private fun configure() {
        val lang = tts.setLanguage(Locale.getDefault())
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }
        tts.setSpeechRate(SPEECH_RATE)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                main.post { handleDone(utteranceId) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                main.post { handleDone(utteranceId) }
            }
        })
    }

    /** Loads [next] and starts speaking it from the first sentence. */
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

    /** Swaps the loaded chunk without speaking, e.g. when the user swipes while paused. */
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
            val index = start + offset
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "${c.cardId}#$index")
        }
    }

    private fun handleDone(utteranceId: String) {
        val c = chunk ?: return
        val (cardId, indexText) = utteranceId.split("#").let { it[0] to it.getOrNull(1) }
        if (cardId != c.cardId || !isPlaying) return
        val index = indexText?.toIntOrNull() ?: return
        position = index + 1
        if (position >= c.segments.size) {
            position = 0
            setPlaying(false)
            onChunkFinished?.invoke(cardId)
        }
    }

    private fun setPlaying(playing: Boolean) {
        if (isPlaying == playing) return
        isPlaying = playing
        onStateChanged?.invoke(playing)
    }

    companion object {
        /** Slightly under 1.0 suits a calm, podcast-like morning listen. */
        const val SPEECH_RATE = 0.95f
    }
}
