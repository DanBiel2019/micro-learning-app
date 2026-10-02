package com.example.aigeneratedandroid.microlearning.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.aigeneratedandroid.microlearning.ui.theme.LocalDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    var openSegment by rememberSaveable { mutableStateOf(false) }
    var segmentIndex by rememberSaveable { mutableIntStateOf(0) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }

    // Android 13+ needs permission for the playback notification / lock-screen controls.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun withNotificationPermission(action: () -> Unit) {
        if (!askedNotifications && Build.VERSION.SDK_INT >= 33 && ui.episode?.hasAudio == true) {
            askedNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        action()
    }

    val episode = ui.episode
    if (episode == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text("Fetching today's episode…", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    BackHandler(enabled = openSegment) { openSegment = false }

    // Light status-bar icons over the dark cover art; dark icons on the light segment pages.
    val view = LocalView.current
    val dark = LocalDarkTheme.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = openSegment && !dark
        }
    }

    val showPlayer = player.episodeId == episode.id
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showPlayer) {
                MiniPlayer(
                    episode = episode,
                    state = player,
                    onOpen = { segmentIndex = player.segmentIndex; openSegment = true },
                    onToggle = vm.player::toggle,
                    onBack15 = { vm.player.seekBy(-15_000) },
                    onForward30 = { vm.player.seekBy(30_000) },
                    onNext = { vm.player.skipToSegment(player.segmentIndex + 1) },
                    onSpeed = vm.player::cycleSpeed
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = openSegment,
            transitionSpec = {
                if (targetState) (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                else fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            },
            label = "screen"
        ) { segment ->
            if (segment) {
                SegmentScreen(
                    episode = episode,
                    startIndex = segmentIndex,
                    player = player,
                    reactions = ui.reactions,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = padding.calculateBottomPadding()),
                    keyOf = { vm.key(episode, it) },
                    related = vm::related,
                    onBack = { openSegment = false },
                    onPageSettled = { page ->
                        segmentIndex = page
                        if (player.episodeId == episode.id && player.isPlaying && player.segmentIndex != page) {
                            vm.player.skipToSegment(page)
                        }
                    },
                    onPlaySegment = { i ->
                        withNotificationPermission {
                            if (player.episodeId == episode.id && player.segmentIndex == i) vm.player.toggle() else vm.play(i)
                        }
                    },
                    onSeek = vm.player::seekTo,
                    onReact = vm::react
                )
            } else {
                HomeScreen(
                    ui = ui,
                    player = player,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = padding.calculateBottomPadding()),
                    onPlayEpisode = { withNotificationPermission(vm::playOrToggle) },
                    onOpenSegment = { segmentIndex = it; openSegment = true },
                    onOpenPrevious = vm::openEpisode,
                    onOpenLibrary = vm::openLibrarySet,
                    onDismissNotice = vm::dismissNotice,
                    completedKey = vm::key
                )
            }
        }
    }
}
