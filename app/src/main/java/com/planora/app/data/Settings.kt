package com.planora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class AppSettings(
    // Design
    /** 0 = System, 1 = Hell, 2 = Dunkel */
    val darkMode: Int = 0,
    val themePreset: Int = 0,
    val accentColor: Int? = null,
    val customBackground: Int? = null,
    val customSurface: Int? = null,
    /** 0 = Standard, 1 = Serif, 2 = Monospace, 3 = Handschrift, 4 = Schmal */
    val fontFamily: Int = 0,
    val fontScale: Float = 1f,
    val cornerScale: Float = 1f,
    /** 0 = kompakt, 1 = normal, 2 = großzügig */
    val spacing: Int = 1,
    /** 0 = gefüllt, 1 = Umriss, 2 = abgerundet */
    val iconStyle: Int = 0,
    /** 0 = gefüllt, 1 = erhöht, 2 = Umriss, 3 = Glas */
    val cardStyle: Int = 0,
    val appBackground: String? = null,
    val appBackgroundAlpha: Float = 0.25f,
    val animations: Boolean = true,

    // Navigation & Übersicht
    val startScreen: String = "dashboard",
    val dashShowDate: Boolean = true,
    val dashShowEvents: Boolean = true,
    val dashShowCountdowns: Boolean = true,
    val dashShowTasks: Boolean = true,
    val dashShowNotes: Boolean = true,
    val dashShowFavorites: Boolean = true,

    // Sortierungen
    /** 0 = nächste zuerst, 1 = Erstellung, 2 = alphabetisch, 3 = manuell, 4 = Tags */
    val countdownSort: Int = 0,
    /** 0 = zuletzt bearbeitet, 1 = erstellt, 2 = Kategorie, 3 = Titel */
    val notesSort: Int = 0,
    val notesGrid: Boolean = true,
    /** 0 = Monat, 1 = Woche, 2 = Tag, 3 = Liste */
    val calendarView: Int = 0,
    val weekStartsMonday: Boolean = true,
    val calendarFontScale: Float = 1f,
    /** 0 = Emojis, 1 = Punkte, 2 = Balken */
    val calendarStyle: Int = 0,

    // Benachrichtigungen
    val notificationsEnabled: Boolean = true,
    val notifyEvents: Boolean = true,
    val notifyCountdowns: Boolean = true,
    val notifyTasks: Boolean = true,
    /** null = Standardton, "" = lautlos, sonst Ton-URI */
    val soundUri: String? = null,
    val vibration: Boolean = true,
    val showPreview: Boolean = true,
    val quietEnabled: Boolean = false,
    val quietStart: Int = 22 * 60,
    val quietEnd: Int = 7 * 60,
    val dailyTaskSummary: Boolean = false,
    val dailyTaskSummaryTime: Int = 8 * 60,
    val defaultEventReminders: List<Long> = listOf(15),
    val defaultCountdownReminders: List<Long> = listOf(1440, 0),

    // Widgets
    val widgetAlpha: Float = 1f,
    val widgetFontScale: Float = 1f,
    val widgetCorner: Int = 20,

    // Sicherung
    val autoBackup: Boolean = true,
    val lastAutoBackup: Long = 0,
    val seeded: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val key = stringPreferencesKey("app_settings_json")

    val flow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { AppJson.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
    }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val cur = prefs[key]?.let { runCatching { AppJson.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
            prefs[key] = AppJson.encodeToString(transform(cur))
        }
    }

    suspend fun replace(settings: AppSettings) = update { settings }
}
