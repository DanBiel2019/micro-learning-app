package com.example.aigeneratedandroid.microlearning.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * The app's map.
 *
 * Three top-level tabs in a Material 3 navigation bar (Today / Library / You), each its own
 * nested graph so every tab keeps its back stack and scroll position when you switch away.
 * Detail screens live inside their tab's graph; the full-screen player sits on top of all tabs.
 *
 * Adding a screen: add a route constant to [Routes], register a `composable(...)` inside the
 * right tab graph in AppNavHost (App.kt), and link to it (e.g. a [YouEntry] on the You tab).
 */
object Routes {
    const val TODAY_GRAPH = "today_tab"
    const val LIBRARY_GRAPH = "library_tab"
    const val YOU_GRAPH = "you_tab"

    const val TODAY = "today"
    const val SEGMENT = "segment/{index}"
    const val LIBRARY = "library"
    const val YOU = "you"
    const val PLAYER = "player"

    fun segment(index: Int) = "segment/$index"
}

enum class TopLevel(val graph: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Today(Routes.TODAY_GRAPH, "Today", Icons.Outlined.WbSunny, Icons.Rounded.WbSunny),
    Library(Routes.LIBRARY_GRAPH, "Library", Icons.Outlined.CollectionsBookmark, Icons.Rounded.CollectionsBookmark),
    You(Routes.YOU_GRAPH, "You", Icons.Outlined.Person, Icons.Rounded.Person)
}

/** Standard tab switch: one copy of each tab, state saved and restored, back goes to Today. */
fun NavController.switchTab(tab: TopLevel) {
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Back to the root of Today (it is always at the bottom of the back stack), closing any detail
 * screen or the player. Used after picking a different episode, so Today never restores a
 * segment page that belonged to the previous one.
 */
fun NavController.goToTodayRoot() {
    if (!popBackStack(Routes.TODAY, inclusive = false)) switchTab(TopLevel.Today)
}
