package com.example.aigeneratedandroid

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.ContentBank
import com.example.aigeneratedandroid.microlearning.data.FeedCache
import com.example.aigeneratedandroid.microlearning.data.Reaction
import com.example.aigeneratedandroid.microlearning.data.SharedPrefsFeedbackStore
import com.example.aigeneratedandroid.microlearning.model.DailyFeed
import com.example.aigeneratedandroid.microlearning.model.IdeaCard
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import com.example.aigeneratedandroid.microlearning.narration.NarrationChunk
import com.example.aigeneratedandroid.microlearning.narration.NarrationFormatter
import com.example.aigeneratedandroid.microlearning.narration.NarrationPlayer
import com.example.aigeneratedandroid.microlearning.ui.CardPagerAdapter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Today's feed: swipe between cards, listen podcast-style with play/pause, and react.
 * Finishing a card's narration auto-advances, so the whole set plays hands-free.
 */
class MainActivity : AppCompatActivity(), CardPagerAdapter.Listener {

    private val profile = UserProfile.default()
    private lateinit var feedback: SharedPrefsFeedbackStore
    private lateinit var cache: FeedCache
    private lateinit var engine: ContentCurationEngine
    private lateinit var player: NarrationPlayer
    private lateinit var feed: DailyFeed
    private lateinit var chunks: List<NarrationChunk>

    private lateinit var pager: ViewPager2
    private lateinit var playButton: Button
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private val adapter = CardPagerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feedback = SharedPrefsFeedbackStore(this)
        cache = FeedCache(this)
        engine = ContentCurationEngine(ContentBank.all, feedback)

        pager = findViewById(R.id.pager)
        playButton = findViewById(R.id.playButton)
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)
        pager.adapter = adapter

        val today = LocalDate.now()
        feed = cache.load(today.toString()) ?: engine.buildFeed(profile, today).also {
            cache.save(it)
            cache.position = 0
            feedback.markShown(it.cards.map(IdeaCard::id), today.toEpochDay())
        }
        showFeed(today)

        player = NarrationPlayer(this).apply {
            onStateChanged = ::renderPlayState
            onChunkFinished = ::onChunkFinished
            onReadyChanged = { ready ->
                if (!ready) Toast.makeText(this@MainActivity, R.string.voice_unavailable, Toast.LENGTH_LONG).show()
            }
        }

        pager.setCurrentItem(cache.position.coerceIn(0, feed.cards.lastIndex), false)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                cache.position = position
                renderProgress(position)
                val chunk = chunks[position]
                if (player.isPlaying) player.play(chunk) else player.cue(chunk)
            }
        })
        player.cue(chunks[pager.currentItem])
        renderProgress(pager.currentItem)

        playButton.setOnClickListener { if (player.isPlaying) player.pause() else player.resume() }
        findViewById<Button>(R.id.prevButton).setOnClickListener { go(pager.currentItem - 1) }
        findViewById<Button>(R.id.nextButton).setOnClickListener { go(pager.currentItem + 1) }
    }

    override fun onStop() {
        super.onStop()
        // Keep it a deliberate listen: leaving the app pauses rather than talking in your pocket.
        if (::player.isInitialized) player.pause()
    }

    override fun onDestroy() {
        if (::player.isInitialized) player.shutdown()
        super.onDestroy()
    }

    // ---- CardPagerAdapter.Listener ----

    override fun onLike(card: IdeaCard) {
        feedback.record(card.id, Reaction.LIKED)
        Toast.makeText(this, R.string.liked, Toast.LENGTH_SHORT).show()
    }

    override fun onNotForMe(card: IdeaCard) {
        feedback.record(card.id, Reaction.SKIPPED)
        Toast.makeText(this, R.string.skipped, Toast.LENGTH_SHORT).show()
        go(pager.currentItem + 1)
    }

    override fun onGoDeeper(card: IdeaCard) {
        val related = engine.related(card, exclude = feed.cards.map(IdeaCard::id))
        if (related.isEmpty()) {
            Toast.makeText(this, R.string.deeper_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = related.map { "${it.title}\n${it.author}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.deeper_title, card.title))
            .setItems(labels) { _, which -> queueNext(card, related[which]) }
            .show()
    }

    // ---- internals ----

    /** Inserts a related card right after the current one, then moves to it. */
    private fun queueNext(from: IdeaCard, extra: IdeaCard) {
        feedback.record(from.id, Reaction.DEEP_DIVED)
        val at = pager.currentItem + 1
        val cards = feed.cards.toMutableList().apply { add(at, extra) }
        feed = feed.copy(cards = cards)
        cache.save(feed)
        feedback.markShown(listOf(extra.id), LocalDate.now().toEpochDay())
        chunks = NarrationFormatter.format(feed)
        adapter.submit(feed.cards)
        Toast.makeText(this, getString(R.string.deeper_added, extra.title), Toast.LENGTH_SHORT).show()
        go(at)
    }

    private fun showFeed(today: LocalDate) {
        chunks = NarrationFormatter.format(feed)
        adapter.submit(feed.cards)
        findViewById<TextView>(R.id.dateText).text =
            today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
        findViewById<TextView>(R.id.themeText).text = feed.theme
    }

    private fun onChunkFinished(cardId: String) {
        feedback.record(cardId, Reaction.COMPLETED)
        if (pager.currentItem < feed.cards.lastIndex) {
            pager.currentItem = pager.currentItem + 1
            player.play(chunks[pager.currentItem])
        }
    }

    private fun go(position: Int) {
        if (position in feed.cards.indices) pager.currentItem = position
    }

    private fun renderProgress(position: Int) {
        val minutes = (chunks.sumOf { it.estimatedSeconds } + 59) / 60
        progressText.text = getString(R.string.progress, position + 1, feed.cards.size, minutes)
        progressBar.max = feed.cards.size
        progressBar.progress = position + 1
    }

    private fun renderPlayState(playing: Boolean) {
        playButton.text = if (playing) "❚❚" else "▶"
        playButton.contentDescription = getString(if (playing) R.string.pause else R.string.play)
    }
}
