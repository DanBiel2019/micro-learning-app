package com.example.aigeneratedandroid.microlearning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.EpisodeRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val episode: Episode? = null,
    /** Other episodes the listener can go back to, newest first. */
    val history: List<PreviousEpisode> = emptyList(),
    val notice: String? = null,
    /** feedback key -> +1 liked, -1 skipped. */
    val reactions: Map<String, Int> = emptyMap(),
    val completed: Set<String> = emptySet()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val profile = UserProfile.default()
    private val repo = EpisodeRepository(app)
    private val feedback = SharedPrefsFeedbackStore(app)
    private val engine by lazy { ContentCurationEngine(repo.library, feedback) }

    val player = EpisodePlayer(app, repo, viewModelScope)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        player.onSegmentCompleted = { ep, index ->
            ep.segments.getOrNull(index)?.let { seg ->
                feedback.record(key(ep, seg), Reaction.COMPLETED)
                refreshFeedback()
            }
        }
        viewModelScope.launch { runCatching { player.connect() } }
        PrefetchWorker.schedule(app)
        load()
    }

    fun load() {
        viewModelScope.launch {
            val cached = repo.cachedEpisodes().firstOrNull()
            if (cached != null) show(cached, refreshing = true)
            else _ui.update { it.copy(loading = true) }

            val latest = repo.refreshLatest()
            when {
                latest != null -> {
                    if (_ui.value.episode?.id == cached?.id || _ui.value.episode == null) show(latest)
                    launch { runCatching { repo.downloadAudio(latest) } }
                }
                cached != null -> _ui.update {
                    it.copy(refreshing = false, notice = "Offline. Showing your most recent downloaded episode.")
                }
                else -> show(
                    engine.buildEpisode(profile, LocalDate.now()),
                    notice = "Couldn't reach today's studio episode, so here's a set from your built-in library, read by your phone's voice."
                )
            }
        }
    }

    fun openEpisode(previous: PreviousEpisode) {
        viewModelScope.launch {
            val ep = repo.cachedEpisode(previous.id) ?: runCatching { repo.fetchEpisode(previous.feedUrl) }.getOrNull()
            if (ep == null) {
                _ui.update { it.copy(notice = "That episode needs a connection to download.") }
                return@launch
            }
            show(ep)
            launch { runCatching { repo.downloadAudio(ep) } }
        }
    }

    fun openLibrarySet() = show(
        engine.buildEpisode(profile, LocalDate.now()),
        notice = "A set from your built-in library, read by your phone's voice."
    )

    fun play(index: Int) {
        val ep = _ui.value.episode ?: return
        player.load(ep, index, play = true)
    }

    fun playOrToggle() {
        val ep = _ui.value.episode ?: return
        val s = player.state.value
        if (s.episodeId == ep.id) player.toggle() else player.load(ep, 0, play = true)
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

    private fun show(ep: Episode, refreshing: Boolean = false, notice: String? = null) {
        player.attach(ep)
        val history = (ep.previous + repo.cachedEpisodes().filter { it.id != ep.id }.map {
            PreviousEpisode(it.id, it.date, it.title, "")
        }).distinctBy { it.id }.filter { it.id != ep.id }.sortedByDescending { it.date }.take(14)
        if (!ep.offline) feedback.markShown(ep.segments.map { key(ep, it) }, LocalDate.now().toEpochDay())
        else feedback.markShown(ep.segments.map { it.id }, LocalDate.now().toEpochDay())
        _ui.update {
            it.copy(loading = false, refreshing = refreshing, episode = ep, history = history, notice = notice ?: it.notice)
        }
        refreshFeedback()
    }

    private fun refreshFeedback() {
        val stats = feedback.allStats()
        _ui.update { s ->
            s.copy(
                reactions = stats.mapValues { (_, v) -> if (v.likes > v.skips) 1 else if (v.skips > v.likes) -1 else 0 }
                    .filterValues { it != 0 },
                completed = stats.filterValues { it.completions > 0 }.keys
            )
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
