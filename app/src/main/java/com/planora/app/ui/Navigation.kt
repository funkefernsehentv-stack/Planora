package com.planora.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.planora.app.ui.components.imageFile
import com.planora.app.ui.screens.CalendarScreen
import com.planora.app.ui.screens.ChecklistDetailScreen
import com.planora.app.ui.screens.ChecklistsScreen
import com.planora.app.ui.screens.CountdownDetailScreen
import com.planora.app.ui.screens.CountdownEditorScreen
import com.planora.app.ui.screens.CountdownsScreen
import com.planora.app.ui.screens.DashboardScreen
import com.planora.app.ui.screens.EventEditorScreen
import com.planora.app.ui.screens.FavoritesScreen
import com.planora.app.ui.screens.NoteEditorScreen
import com.planora.app.ui.screens.NotesScreen
import com.planora.app.ui.screens.SearchScreen
import com.planora.app.ui.screens.SettingsScreen
import com.planora.app.ui.screens.TagDetailScreen
import com.planora.app.ui.screens.TagsScreen
import com.planora.app.ui.theme.AppIcons
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val DASHBOARD = "dashboard"
    const val CALENDAR = "calendar"
    const val COUNTDOWNS = "countdowns"
    const val FAVORITES = "favorites"
    const val CHECKLISTS = "checklists"
    const val NOTES = "notes"
    const val TAGS = "tags"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun countdown(id: Long) = "countdown/$id"
    fun countdownEdit(id: Long) = "countdown_edit/$id"
    fun eventEdit(id: Long, date: String = "") = "event_edit/$id?date=$date"
    fun checklist(id: Long) = "checklist/$id"
    fun note(id: Long) = "note/$id"
    fun tag(id: Long) = "tag/$id"

    val topLevel = listOf(CALENDAR, COUNTDOWNS, FAVORITES, CHECKLISTS, NOTES)
}

/** Navigation innerhalb der App (wird an alle Screens weitergereicht). */
class Nav(val controller: NavHostController) {
    fun go(route: String) = controller.navigate(route) { launchSingleTop = true }
    fun back() { if (!controller.popBackStack()) controller.navigate(Routes.DASHBOARD) }
    fun tab(route: String) = controller.navigate(route) {
        popUpTo(controller.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Eintrag, der beim Start über Benachrichtigung oder Widget geöffnet werden soll. */
data class OpenRequest(val type: String, val id: Long, val nonce: Long = System.nanoTime())

@Composable
fun TopBarActions(nav: Nav) {
    IconButton(onClick = { nav.go(Routes.DASHBOARD) }) { Icon(AppIcons.dashboard(), "Übersicht") }
    IconButton(onClick = { nav.go(Routes.SEARCH) }) { Icon(AppIcons.search(), "Suche") }
    IconButton(onClick = { nav.go(Routes.TAGS) }) { Icon(Icons.AutoMirrored.Filled.Label, "Tags") }
    IconButton(onClick = { nav.go(Routes.SETTINGS) }) { Icon(AppIcons.settings(), "Einstellungen") }
}

@Composable
fun PlanoraRoot(vm: AppViewModel, startRoute: String, openRequests: MutableStateFlow<OpenRequest?>) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val controller = rememberNavController()
    val nav = Nav(controller)
    val backStack by controller.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val open by openRequests.collectAsStateWithLifecycle()

    LaunchedEffect(open) {
        val r = open ?: return@LaunchedEffect
        when (r.type) {
            "countdown" -> if (r.id > 0) nav.go(Routes.countdown(r.id)) else nav.tab(Routes.COUNTDOWNS)
            "countdowns" -> nav.tab(Routes.COUNTDOWNS)
            "event" -> nav.go(Routes.eventEdit(r.id))
            "calendar" -> nav.tab(Routes.CALENDAR)
            "task" -> {
                val listId = vm.tasks.value.firstOrNull { it.id == r.id }?.checklistId
                if (listId != null) nav.go(Routes.checklist(listId)) else nav.tab(Routes.CHECKLISTS)
            }
            "checklist" -> if (r.id > 0) nav.go(Routes.checklist(r.id)) else nav.tab(Routes.CHECKLISTS)
            "checklists" -> nav.tab(Routes.CHECKLISTS)
            "note" -> if (r.id > 0) nav.go(Routes.note(r.id)) else nav.tab(Routes.NOTES)
            "new_note" -> nav.go(Routes.note(0))
        }
        openRequests.value = null
    }

    val anim = settings.animations
    val bgFile = imageFile(settings.appBackground)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (bgFile != null) {
            AsyncImage(
                model = bgFile, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(settings.appBackgroundAlpha),
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (route in Routes.topLevel || route == Routes.DASHBOARD) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f)) {
                        val items: List<Triple<String, String, ImageVector>> = listOf(
                            Triple(Routes.CALENDAR, "Kalender", AppIcons.calendar()),
                            Triple(Routes.COUNTDOWNS, "Countdown", AppIcons.countdown()),
                            Triple(Routes.FAVORITES, "Favoriten", AppIcons.favorites()),
                            Triple(Routes.CHECKLISTS, "Checklisten", AppIcons.checklist()),
                            Triple(Routes.NOTES, "Notizen", AppIcons.notes()),
                        )
                        items.forEach { (r, label, icon) ->
                            NavigationBarItem(
                                selected = route == r,
                                onClick = { nav.tab(r) },
                                icon = { Icon(icon, label) },
                                label = { Text(label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                    }
                }
            },
        ) { pad ->
            NavHost(
                navController = controller,
                startDestination = startRoute.takeIf { it in Routes.topLevel || it == Routes.DASHBOARD } ?: Routes.DASHBOARD,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { if (anim) fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f) else EnterTransition.None },
                exitTransition = { if (anim) fadeOut(tween(150)) else ExitTransition.None },
                popEnterTransition = { if (anim) fadeIn(tween(220)) else EnterTransition.None },
                popExitTransition = { if (anim) fadeOut(tween(150)) else ExitTransition.None },
            ) {
                composable(Routes.DASHBOARD) { DashboardScreen(vm, nav, pad) }
                composable(Routes.CALENDAR) { CalendarScreen(vm, nav, pad) }
                composable(Routes.COUNTDOWNS) { CountdownsScreen(vm, nav, pad) }
                composable(Routes.FAVORITES) { FavoritesScreen(vm, nav, pad) }
                composable(Routes.CHECKLISTS) { ChecklistsScreen(vm, nav, pad) }
                composable(Routes.NOTES) { NotesScreen(vm, nav, pad) }
                composable(Routes.TAGS) { TagsScreen(vm, nav) }
                composable(Routes.SEARCH) { SearchScreen(vm, nav) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
                composable("countdown/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    CountdownDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
                composable("countdown_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    CountdownEditorScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
                composable(
                    "event_edit/{id}?date={date}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("date") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { EventEditorScreen(vm, nav, it.arguments?.getLong("id") ?: 0, it.arguments?.getString("date") ?: "") }
                composable("checklist/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    ChecklistDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
                composable("note/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    NoteEditorScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
                composable("tag/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    TagDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
            }
        }
    }
}
