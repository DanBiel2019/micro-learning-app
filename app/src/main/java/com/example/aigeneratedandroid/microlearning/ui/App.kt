package com.example.aigeneratedandroid.microlearning.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val inPlayer = destination?.route == Routes.PLAYER
    val haptics = LocalHapticFeedback.current

    // Android 13+ needs permission for the playback notification / lock-screen controls.
    // Asked in context, the first time the listener presses play (not at launch).
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun withNotificationPermission(action: () -> Unit) {
        if (!askedNotifications && Build.VERSION.SDK_INT >= 33 && ui.episode?.hasAudio == true) {
            askedNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        action()
    }

    val playingEpisode = nowPlaying?.takeIf { player.episodeId == it.id }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Screens handle the status bar themselves (the Today hero draws behind it).
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = !inPlayer,
                enter = expandVertically(tween(250)) + fadeIn(),
                exit = shrinkVertically(tween(200)) + fadeOut()
            ) {
                Column {
                    if (playingEpisode != null) {
                        MiniPlayer(
                            episode = playingEpisode,
                            state = player,
                            onOpen = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } },
                            onToggle = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.player.toggle()
                            },
                            onForward30 = { vm.player.seekBy(30_000) },
                            onNext = { vm.skipTo(player.segmentIndex + 1) }
                        )
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        TopLevel.entries.forEach { tab ->
                            val selected = destination?.hierarchy?.any { it.route == tab.graph } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (selected) nav.popBackStack(startRouteOf(tab), inclusive = false)
                                    else nav.switchTab(tab)
                                },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        AppNavHost(
            nav = nav,
            vm = vm,
            ui = ui,
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
            withNotificationPermission = ::withNotificationPermission
        )
    }
}

private fun startRouteOf(tab: TopLevel) = when (tab) {
    TopLevel.Today -> Routes.TODAY
    TopLevel.Library -> Routes.LIBRARY
    TopLevel.You -> Routes.YOU
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    vm: MainViewModel,
    ui: UiState,
    contentPadding: PaddingValues,
    withNotificationPermission: (() -> Unit) -> Unit
) {
    val player by vm.player.state.collectAsStateWithLifecycle()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()

    // Material motion: fade-through between tabs, shared-axis X into details, the player slides up.
    NavHost(
        navController = nav,
        startDestination = Routes.TODAY_GRAPH,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(tween(220, delayMillis = 60)) },
        exitTransition = { fadeOut(tween(90)) },
        popEnterTransition = { fadeIn(tween(220, delayMillis = 60)) },
        popExitTransition = { fadeOut(tween(90)) }
    ) {
        navigation(route = Routes.TODAY_GRAPH, startDestination = Routes.TODAY) {
            composable(Routes.TODAY) {
                HomeScreen(
                    ui = ui,
                    player = player,
                    contentPadding = contentPadding,
                    onPlayEpisode = { withNotificationPermission(vm::playOrToggle) },
                    onOpenSegment = { nav.navigate(Routes.segment(it)) },
                    onRefresh = vm::refresh,
                    onBackToToday = vm::backToToday,
                    onDismissNotice = vm::dismissNotice,
                    completedKey = vm::key
                )
            }
            composable(
                Routes.SEGMENT,
                arguments = listOf(navArgument("index") { type = NavType.IntType }),
                enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { slideOutHorizontally(tween(250)) { it / 4 } + fadeOut(tween(200)) }
            ) { entry ->
                val episode = ui.episode ?: return@composable
                val start = (entry.arguments?.getInt("index") ?: 0).coerceIn(0, (episode.segments.size - 1).coerceAtLeast(0))
                SegmentScreen(
                    episode = episode,
                    startIndex = start,
                    player = player,
                    reactions = ui.reactions,
                    contentPadding = contentPadding,
                    keyOf = { vm.key(episode, it) },
                    related = vm::related,
                    onBack = { nav.popBackStack() },
                    onPageSettled = { page ->
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
            }
        }

        navigation(route = Routes.LIBRARY_GRAPH, startDestination = Routes.LIBRARY) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    ui = ui,
                    contentPadding = contentPadding,
                    onOpenEpisode = { prev ->
                        vm.openEpisode(prev)
                        nav.goToTodayRoot()
                    },
                    onOpenToday = {
                        vm.backToToday()
                        nav.goToTodayRoot()
                    },
                    onOpenClassics = {
                        vm.openLibrarySet()
                        nav.goToTodayRoot()
                    }
                )
            }
        }

        navigation(route = Routes.YOU_GRAPH, startDestination = Routes.YOU) {
            composable(Routes.YOU) {
                YouScreen(
                    stats = ui.stats,
                    speed = player.speed,
                    feedRepo = vm.feedRepo,
                    appVersion = vm.appVersion,
                    contentPadding = contentPadding,
                    onSpeed = vm.player::setSpeed,
                    // Extension point: add entries here (e.g. an infographic gallery) and
                    // register their routes in this graph.
                    entries = emptyList()
                )
            }
        }

        composable(
            Routes.PLAYER,
            enterTransition = { slideInVertically(tween(320)) { it } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { slideOutVertically(tween(280)) { it } }
        ) {
            val episode = nowPlaying?.takeIf { player.episodeId == it.id }
            if (episode == null) {
                // Nothing loaded any more (e.g. the session was stopped): close the player.
                LaunchedEffect(Unit) { nav.popBackStack() }
                return@composable
            }
            NowPlayingScreen(
                episode = episode,
                state = player,
                onClose = { nav.popBackStack() },
                onToggle = vm.player::toggle,
                onSeek = vm.player::seekTo,
                onSeekBy = vm.player::seekBy,
                onSkipTo = vm::skipTo,
                onSpeed = vm.player::setSpeed,
                onOpenTranscript = { index ->
                    // The transcript lives on the segment page of the episode on Today.
                    if (ui.episode?.id != episode.id) vm.showEpisode(episode)
                    nav.goToTodayRoot()
                    nav.navigate(Routes.segment(index))
                }
            )
        }
    }
}
