package com.planora.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Aktuelle Uhrzeit, die sich im angegebenen Intervall aktualisiert (für Live-Countdowns). */
@Composable
fun rememberNow(periodMs: Long = 1000L): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs - System.currentTimeMillis() % periodMs + 5)
            now = LocalDateTime.now()
        }
    }
    return now
}

/** Vorgefertigte Symbole für Countdowns. */
object CountdownIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "cake" to Icons.Filled.Cake,
        "gift" to Icons.Filled.CardGiftcard,
        "celebration" to Icons.Filled.Celebration,
        "flight" to Icons.Filled.Flight,
        "beach" to Icons.Filled.BeachAccess,
        "school" to Icons.Filled.School,
        "fitness" to Icons.Filled.FitnessCenter,
        "sports" to Icons.Filled.SportsSoccer,
        "favorite" to Icons.Filled.Favorite,
        "music" to Icons.Filled.MusicNote,
        "work" to Icons.Filled.Work,
        "home" to Icons.Filled.Home,
        "car" to Icons.Filled.DirectionsCar,
        "pets" to Icons.Filled.Pets,
        "event" to Icons.Filled.Event,
        "star" to Icons.Filled.Star,
    )

    fun byName(name: String?): ImageVector? = all.firstOrNull { it.first == name }?.second
}
