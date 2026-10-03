package com.example.aigeneratedandroid.microlearning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aigeneratedandroid.BuildConfig
import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.EpisodeRepository
import com.example.aigeneratedandroid.microlearning.data.ListeningLog
import com.example.aigeneratedandroid.microlearning.data.ListeningStats
import com.example.aigeneratedandroid.microlearning.data.PrefetchWorker
import com.example.aigeneratedandroid.microlearning.data.Reaction
import com.example.aigeneratedandroid.microlearning.data.SharedPrefsFeedbackStore
import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.PreviousEpisode
import com.example.aigeneratedandroid.microlearning.model.Segment
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import com.example.aigeneratedandroid.microlearning.playback.EpisodePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A dismissible message on the Today screen; [canRetry] adds a "Try again" action. */
data class Notice(val text: String, val canRetry: Boolean = false)

data class UiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    /** The episode shown on the Today tab (today's, or one picked from the Library). */
    val episode: Episode? = null,
    /** Id of the newest episode we know about, so Today can offer "Back to today's episode". */
    val todayId: String? = null,
    /** Other episodes the listener can go back to, newest first. */
    val history: List<PreviousEpisode> = emptyList(),
    /** Ids of episodes whose feed is stored on the device (open without a connection). */
    val downloaded: Set<String> = emptySet(),
    val notice: Notice? = null,
    /** feedback key -> +1 liked, -1 skipped. */
    val reactions: Map<String, Int> = emptyMap(),
    val completed: Set<String> = emptySet(),
    val stats: ListeningStats = ListeningStats()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val profile = UserProfile.default()
    private val repo = EpisodeRepository(app)
    private val feedback = SharedPrefsFeedbackStore(app)
    private val log = ListeningLog(app)
    private val engine by lazy { ContentCurationEngine(repo.library, feedback) }

    val player = EpisodePlayer(app, repo, viewModelScope)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** The episode loaded in the player (may differ from the one on screen). */
    private val _nowPlaying = MutableStateFlow<Episode?>(null)
    val nowPlaying: StateFlow<Episode?> = _nowPlaying.asStateFlow()

    private var today: Episode? = null

    val feedRepo: String = BuildConfig.FEED_REPO
    val appVersion: String = BuildConfig.VERSION_NAME

    init {
        player.onSegmentCompleted = { ep, index ->
            ep.segments.getOrNull(index)?.let { seg ->
                feedback.record(key(ep, seg), Reaction.COMPLETED)
                log.recordCompletion(seg.durationMs)
                refreshFeedback()
            }
        }
        viewModelScope.launch { runCatching { player.connect() } }
        // Keep the mini player bound to whatever episode the session is playing, even after the
        // listener has browsed to a different one (or the activity was recreated mid-episode).
        viewModelScope.launch {
            player.state.map { it.episodeId }.distinctUntilChanged().collect { id ->
                _nowPlaying.value = id?.let(::resolveEpisode)?.also(player::attach)
            }
        }
        PrefetchWorker.schedule(app)
        load()
    }

    fun load() {
        viewModelScope.launch {
            val cached = repo.cachedEpisodes().firstOrNull()
            if (cached != null) {
                today = cached
                show(cached, refreshing = true)
            } else {
                _ui.update { it.copy(loading = true) }
            }

            val latest = repo.refreshLatest()
            when {
                latest != null -> {
                    today = latest
                    if (_ui.value.episode?.id == cached?.id || _ui.value.episode == null) show(latest)
                    else _ui.update { it.copy(refreshing = false, todayId = latest.id) }
                    launch { runCatching { repo.downloadAudio(latest) } }
                }
                cached != null -> _ui.update {
                    it.copy(
                        refreshing = false,
                        notice = Notice("You're offline. Showing your most recent downloaded episode.", canRetry = true)
                    )
                }
                else -> show(
                    engine.buildEpisode(profile, LocalDate.now()).also { today = it },
                    notice = Notice(
                        "Couldn't reach today's studio episode, so here's a set from your built-in library, read by your phone's voice.",
                        canRetry = true
                    )
                )
            }
        }
    }

    /** Pull-to-refresh / "Try again": fetch the newest episode and show it. */
    fun refresh() {
        if (_ui.value.refreshing) return
        _ui.update { it.copy(refreshing = true, notice = null) }
        viewModelScope.launch {
            val latest = repo.refreshLatest()
            if (latest != null) {
                today = latest
                show(latest)
                launch { runCatching { repo.downloadAudio(latest) } }
            } else {
                _ui.update {
                    it.copy(refreshing = false, notice = Notice("Still offline. Your downloaded episodes are in the Library.", canRetry = true))
                }
            }
        }
    }

    fun backToToday() {
        val t = today ?: return load()
        show(t)
    }

    fun openEpisode(previous: PreviousEpisode) {
        viewModelScope.launch {
            val ep = repo.cachedEpisode(previous.id) ?: runCatching { repo.fetchEpisode(previous.feedUrl) }.getOrNull()
            if (ep == null) {
                _ui.update { it.copy(notice = Notice("That episode needs a connection to download.", canRetry = false)) }
                return@launch
            }
            show(ep)
            launch { runCatching { repo.downloadAudio(ep) } }
        }
    }

    /** Shows an episode we already hold (e.g. the one playing) on the Today tab. */
    fun showEpisode(ep: Episode) = show(ep)

    fun openLibrarySet() = show(
        engine.buildEpisode(profile, LocalDate.now()),
        notice = Notice("A set from your built-in library, read by your phone's voice.")
    )

    fun play(index: Int) {
        val ep = _ui.value.episode ?: return
        _nowPlaying.value = ep
        player.load(ep, index, play = true)
    }

    fun playOrToggle() {
        val ep = _ui.value.episode ?: return
        val s = player.state.value
        if (s.episodeId == ep.id) player.toggle() else {
            _nowPlaying.value = ep
            player.load(ep, 0, play = true)
        }
    }

    /** Skip within the episode that's playing (Now Playing / mini player). */
    fun skipTo(index: Int) {
        val ep = _nowPlaying.value ?: return
        if (index !in ep.segments.indices) return
        player.attach(ep)
        player.load(ep, index, play = true)
    }

    fun react(segment: Segment, reaction: Reaction) {
        val ep = _ui.value.episode ?: return
        feedback.record(key(ep, segment), reaction)
        refreshFeedback()
    }

    fun related(segment: Segment): List<Segment> {
        val ep = _ui.value.episode ?: return emptyList()
        return engine.related(segment, exclude = ep.segments.map { it.id }, limit = 2)
    }

    /** Feedback is keyed per episode for studio segments (their ids repeat daily). */
    fun key(ep: Episode, seg: Segment): String = if (ep.offline) seg.id else "${ep.id}/${seg.id}"

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    private fun resolveEpisode(id: String): Episode? =
        _nowPlaying.value?.takeIf { it.id == id }
            ?: _ui.value.episode?.takeIf { it.id == id }
            ?: today?.takeIf { it.id == id }
            ?: repo.cachedEpisode(id)

    private fun show(ep: Episode, refreshing: Boolean = false, notice: Notice? = null) {
        player.attach(ep)
        val cached = repo.cachedEpisodes()
        val history = (ep.previous + cached.filter { it.id != ep.id }.map {
            PreviousEpisode(it.id, it.date, it.title, "")
        }).distinctBy { it.id }.filter { it.id != ep.id }.sortedByDescending { it.date }.take(14)
        if (!ep.offline) feedback.markShown(ep.segments.map { key(ep, it) }, LocalDate.now().toEpochDay())
        else feedback.markShown(ep.segments.map { it.id }, LocalDate.now().toEpochDay())
        _ui.update {
            it.copy(
                loading = false,
                refreshing = refreshing,
                episode = ep,
                todayId = today?.id ?: ep.id,
                history = history,
                downloaded = cached.map { c -> c.id }.toSet(),
                notice = notice ?: it.notice
            )
        }
        refreshFeedback()
    }

    private fun refreshFeedback() {
        val stats = feedback.allStats()
        _ui.update { s ->
            s.copy(
                reactions = stats.mapValues { (_, v) -> if (v.likes > v.skips) 1 else if (v.skips > v.likes) -1 else 0 }
                    .filterValues { it != 0 },
                completed = stats.filterValues { it.completions > 0 }.keys,
                stats = log.stats()
            )
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
