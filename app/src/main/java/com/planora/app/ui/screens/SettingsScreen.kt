package com.planora.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.data.AppSettings
import com.planora.app.domain.DateLogic
import com.planora.app.notify.NotificationHelper
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.Routes
import com.planora.app.ui.components.ChoiceChips
import com.planora.app.ui.components.ColorPalette
import com.planora.app.ui.components.ImagePickRow
import com.planora.app.ui.components.LabeledSlider
import com.planora.app.ui.components.ReminderEditor
import com.planora.app.ui.components.SectionTitle
import com.planora.app.ui.components.SettingsCard
import com.planora.app.ui.components.SwitchRow
import com.planora.app.ui.components.TimePickerDialogM
import com.planora.app.ui.components.rememberImagePicker
import com.planora.app.ui.theme.FontFamilies
import com.planora.app.ui.theme.ThemePresets
import com.planora.app.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: Nav) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun upd(f: (AppSettings) -> AppSettings) = vm.updateSettings(f)
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    var timeDialog by remember { mutableIntStateOf(0) } // 1 = Ruhe Start, 2 = Ruhe Ende, 3 = Zusammenfassung
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var restoreFile by remember { mutableStateOf<File?>(null) }
    var backups by remember { mutableStateOf(vm.localBackups()) }

    val pickBg = rememberImagePicker { uri -> vm.importImage(uri) { n -> if (n != null) upd { it.copy(appBackground = n) } } }
    val ringtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            upd { it.copy(soundUri = uri?.toString() ?: "") }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        toast(if (ok) "Benachrichtigungen erlaubt ✅" else "Benachrichtigungen wurden abgelehnt")
        vm.rescheduleReminders()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(uri) { toast(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> importUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // ---------------- Design ----------------
            item {
                SectionTitle("🎨 Design & Themes")
                SettingsCard {
                    Text("Vorgefertigte Themes", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemePresets.forEachIndexed { i, p ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { upd { it.copy(themePreset = i, accentColor = null) } }) {
                                Box(
                                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(Color(p.primary), Color(p.secondary), Color(p.tertiary))))
                                        .then(if (s.themePreset == i) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp)) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) { if (s.themePreset == i) Text("✓", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                                Text(p.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Text("Modus", fontWeight = FontWeight.Bold)
                    ChoiceChips(listOf(0 to "System", 1 to "Hell", 2 to "Dunkel"), s.darkMode) { v -> upd { it.copy(darkMode = v) } }
                    Spacer(Modifier.height(8.dp))
                    Text("Eigene Akzentfarbe", fontWeight = FontWeight.Bold)
                    ColorPalette(s.accentColor, { c -> upd { it.copy(accentColor = c) } })
                    if (s.accentColor != null) TextButton(onClick = { upd { it.copy(accentColor = null) } }) { Text("Akzentfarbe zurücksetzen") }
                    Text("Eigene Hintergrundfarbe der App", fontWeight = FontWeight.Bold)
                    ColorPalette(s.customBackground, { c -> upd { it.copy(customBackground = c) } })
                    if (s.customBackground != null) TextButton(onClick = { upd { it.copy(customBackground = null) } }) { Text("Zurücksetzen") }
                    Text("Eigene Kartenfarbe", fontWeight = FontWeight.Bold)
                    ColorPalette(s.customSurface, { c -> upd { it.copy(customSurface = c) } })
                    if (s.customSurface != null) TextButton(onClick = { upd { it.copy(customSurface = null) } }) { Text("Zurücksetzen") }
                }
            }
            item {
                SettingsCard {
                    Text("Schriftart", fontWeight = FontWeight.Bold)
                    ChoiceChips(FontFamilies.indices.map { it to FontFamilies[it].first }, s.fontFamily) { v -> upd { it.copy(fontFamily = v) } }
                    LabeledSlider("Schriftgröße", s.fontScale, 0.8f..1.4f, "${(s.fontScale * 100).toInt()} %") { v -> upd { it.copy(fontScale = v) } }
                    LabeledSlider("Rundungen", s.cornerScale, 0f..2f, "${(s.cornerScale * 100).toInt()} %") { v -> upd { it.copy(cornerScale = v) } }
                    Text("Abstände", fontWeight = FontWeight.Bold)
                    ChoiceChips(listOf(0 to "Kompakt", 1 to "Normal", 2 to "Großzügig"), s.spacing) { v -> upd { it.copy(spacing = v) } }
                    Text("Icon-Stil", fontWeight = FontWeight.Bold)
                    ChoiceChips(listOf(0 to "Gefüllt", 1 to "Umriss", 2 to "Abgerundet"), s.iconStyle) { v -> upd { it.copy(iconStyle = v) } }
                    SwitchRow("Animationen", s.animations) { v -> upd { it.copy(animations = v) } }
                }
            }
            item {
                SettingsCard {
                    Text("App-Hintergrundbild", fontWeight = FontWeight.Bold)
                    ImagePickRow(s.appBackground, onPick = pickBg, onRemove = { upd { it.copy(appBackground = null) } })
                    if (s.appBackground != null) {
                        LabeledSlider("Sichtbarkeit", s.appBackgroundAlpha, 0.05f..0.8f, "${(s.appBackgroundAlpha * 100).toInt()} %") { v -> upd { it.copy(appBackgroundAlpha = v) } }
                    }
                }
            }
            // ---------------- Start & Kalender ----------------
            item {
                SectionTitle("🧭 Start & Kalender")
                SettingsCard {
                    Text("Startbildschirm (ab dem nächsten App-Start)", fontWeight = FontWeight.Bold)
                    ChoiceChips(
                        listOf(Routes.DASHBOARD to "Übersicht", Routes.CALENDAR to "Kalender", Routes.COUNTDOWNS to "Countdowns",
                            Routes.FAVORITES to "Favoriten", Routes.CHECKLISTS to "Checklisten", Routes.NOTES to "Notizen"),
                        s.startScreen,
                    ) { v -> upd { it.copy(startScreen = v) } }
                    SwitchRow("Woche beginnt am Montag", s.weekStartsMonday) { v -> upd { it.copy(weekStartsMonday = v) } }
                    Text("Darstellung im Monatskalender", fontWeight = FontWeight.Bold)
                    ChoiceChips(listOf(0 to "Emojis", 1 to "Farbpunkte", 2 to "Titelbalken"), s.calendarStyle) { v -> upd { it.copy(calendarStyle = v) } }
                    LabeledSlider("Schriftgröße im Kalender", s.calendarFontScale, 0.8f..1.3f, "${(s.calendarFontScale * 100).toInt()} %") { v -> upd { it.copy(calendarFontScale = v) } }
                    Text("Tipp: Tag im Monatskalender lange drücken, um ihm ein eigenes Hintergrundbild, eine Farbe oder ein Emoji zu geben. Über 🖌️ gestaltest du den ganzen Monat.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // ---------------- Benachrichtigungen ----------------
            item {
                SectionTitle("🔔 Benachrichtigungen")
                SettingsCard {
                    val canPost = NotificationHelper.canPost(context)
                    if (!canPost) {
                        Text("⚠️ Benachrichtigungen sind für Planora nicht erlaubt.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        }) { Text("Erlauben") }
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        val am = context.getSystemService(AlarmManager::class.java)
                        if (am != null && !am.canScheduleExactAlarms()) {
                            Text("⚠️ Exakte Wecker sind nicht erlaubt – Erinnerungen können sich verspäten.", color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                            }) { Text("Exakte Wecker erlauben") }
                        }
                    }
                    SwitchRow("Alle Benachrichtigungen", s.notificationsEnabled) { v -> upd { it.copy(notificationsEnabled = v) } }
                    if (s.notificationsEnabled) {
                        SwitchRow("📅 Kalender-Erinnerungen", s.notifyEvents) { v -> upd { it.copy(notifyEvents = v) } }
                        SwitchRow("⏳ Countdown-Erinnerungen", s.notifyCountdowns) { v -> upd { it.copy(notifyCountdowns = v) } }
                        SwitchRow("✅ Checklisten-Erinnerungen", s.notifyTasks) { v -> upd { it.copy(notifyTasks = v) } }
                    }
                }
            }
            item {
                SettingsCard {
                    Text("Ton", fontWeight = FontWeight.Bold)
                    val soundLabel = when {
                        s.soundUri == null -> "Standard"
                        s.soundUri == "" -> "Lautlos"
                        else -> runCatching { RingtoneManager.getRingtone(context, Uri.parse(s.soundUri))?.getTitle(context) }.getOrNull() ?: "Eigener Ton"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(soundLabel, Modifier.weight(1f))
                        TextButton(onClick = {
                            ringtone.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Erinnerungston")
                                s.soundUri?.takeIf { it.isNotEmpty() }?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                            })
                        }) { Text("Ändern") }
                        if (s.soundUri != null) TextButton(onClick = { upd { it.copy(soundUri = null) } }) { Text("Standard") }
                    }
                    SwitchRow("Vibration", s.vibration) { v -> upd { it.copy(vibration = v) } }
                    SwitchRow("Inhalt in der Vorschau anzeigen", s.showPreview, subtitle = "Aus: nur „Planora-Erinnerung“ wird angezeigt") { v -> upd { it.copy(showPreview = v) } }
                    SwitchRow("Ruhezeiten", s.quietEnabled, subtitle = "Erinnerungen kommen lautlos") { v -> upd { it.copy(quietEnabled = v) } }
                    if (s.quietEnabled) {
                        Row {
                            OutlinedButton(onClick = { timeDialog = 1 }) { Text("Von %02d:%02d".format(s.quietStart / 60, s.quietStart % 60)) }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { timeDialog = 2 }) { Text("Bis %02d:%02d".format(s.quietEnd / 60, s.quietEnd % 60)) }
                        }
                    }
                    SwitchRow("Tägliche Erinnerung an offene Aufgaben", s.dailyTaskSummary) { v -> upd { it.copy(dailyTaskSummary = v) } }
                    if (s.dailyTaskSummary) {
                        OutlinedButton(onClick = { timeDialog = 3 }) { Text("Um %02d:%02d Uhr".format(s.dailyTaskSummaryTime / 60, s.dailyTaskSummaryTime % 60)) }
                    }
                }
            }
            item {
                SettingsCard {
                    Text("Standard-Erinnerungen für neue Termine", fontWeight = FontWeight.Bold)
                    ReminderEditor(s.defaultEventReminders, listOf(0L, 5L, 15L, 30L, 60L, 1440L, 10080L)) { v -> upd { it.copy(defaultEventReminders = v) } }
                    Spacer(Modifier.height(8.dp))
                    Text("Standard-Erinnerungen für neue Countdowns", fontWeight = FontWeight.Bold)
                    ReminderEditor(s.defaultCountdownReminders, listOf(43200L, 20160L, 10080L, 4320L, 1440L, 0L)) { v -> upd { it.copy(defaultCountdownReminders = v) } }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            NotificationHelper.show(context, s, 999_999, "🔔 Testbenachrichtigung", "So sehen deine Planora-Erinnerungen aus.", "dashboard", 0)
                        }) { Text("Test senden") }
                        OutlinedButton(onClick = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        }) { Text("Energiesparen") }
                    }
                    Text("Tipp: Nimm Planora von der Akku-Optimierung aus, damit Erinnerungen auch im Energiesparmodus pünktlich kommen. Erinnerungen werden nach einem Neustart automatisch wiederhergestellt.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // ---------------- Widgets ----------------
            item {
                SectionTitle("📲 Homescreen-Widgets")
                SettingsCard {
                    Text("Verfügbare Widgets: Countdown, Nächste Countdowns, Kalender (Monat/Woche/Heute/Nächste), Checkliste (mit Abhaken), Notiz und Schnellnotiz.")
                    Text("Hinzufügen: Homescreen lange drücken → Widgets → Planora. Jedes Widget lässt sich beim Platzieren (und später durch langes Drücken → Neu konfigurieren) individuell anpassen.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Standardwerte für neue Widgets", fontWeight = FontWeight.Bold)
                    LabeledSlider("Deckkraft", s.widgetAlpha, 0.15f..1f, "${(s.widgetAlpha * 100).toInt()} %") { v -> upd { it.copy(widgetAlpha = v) } }
                    LabeledSlider("Schriftgröße", s.widgetFontScale, 0.7f..1.5f, "${(s.widgetFontScale * 100).toInt()} %") { v -> upd { it.copy(widgetFontScale = v) } }
                    LabeledSlider("Ecken", s.widgetCorner.toFloat(), 0f..36f, "${s.widgetCorner} dp") { v -> upd { it.copy(widgetCorner = v.toInt()) } }
                    OutlinedButton(onClick = { scope.launch { WidgetUpdater.updateAll(context); toast("Widgets aktualisiert") } }) { Text("Widgets jetzt aktualisieren") }
                }
            }
            // ---------------- Datensicherung ----------------
            item {
                SectionTitle("💾 Datensicherung")
                SettingsCard {
                    Text("Alle Daten (inkl. Bilder, Tags und Einstellungen) als JSON-Datei exportieren – z. B. für einen Gerätewechsel.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        Button(onClick = {
                            exportLauncher.launch("planora-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())}.json")
                        }) { Text("Exportieren") }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Importieren") }
                    }
                    SwitchRow("Automatische lokale Sicherung (täglich)", s.autoBackup) { v -> upd { it.copy(autoBackup = v) } }
                    if (s.lastAutoBackup > 0) Text("Letzte Sicherung: ${DateLogic.formatDateTime(DateLogic.fromEpochMillis(s.lastAutoBackup))}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { vm.backupNow { toast(it); backups = vm.localBackups() } }) { Text("Jetzt lokal sichern") }
                    if (backups.isNotEmpty()) {
                        Text("Lokale Sicherungen", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        backups.take(7).forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.name.removePrefix("auto_").removeSuffix(".json").replace("_", " "), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { restoreFile = f }) { Text("Wiederherstellen") }
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("ℹ️ Über")
                SettingsCard {
                    Text("Planora 1.0", fontWeight = FontWeight.Bold)
                    Text("Kalender · Countdowns · Checklisten · Notizen – komplett offline, ohne Anmeldung. Alle Daten bleiben auf deinem Gerät.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (timeDialog != 0) {
        val initial = when (timeDialog) { 1 -> s.quietStart; 2 -> s.quietEnd; else -> s.dailyTaskSummaryTime }
        TimePickerDialogM(LocalTime.of(initial / 60, initial % 60), { timeDialog = 0 }) { t ->
            val m = t.hour * 60 + t.minute
            when (timeDialog) {
                1 -> upd { it.copy(quietStart = m) }
                2 -> upd { it.copy(quietEnd = m) }
                else -> upd { it.copy(dailyTaskSummaryTime = m) }
            }
            timeDialog = 0
        }
    }
    importUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text("Daten importieren?") },
            text = { Text("Alle aktuellen Daten werden durch die Sicherung ersetzt.") },
            confirmButton = { TextButton(onClick = { vm.import(uri) { toast(it) }; importUri = null }) { Text("Importieren") } },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("Abbrechen") } },
        )
    }
    restoreFile?.let { f ->
        AlertDialog(
            onDismissRequest = { restoreFile = null },
            title = { Text("Sicherung wiederherstellen?") },
            text = { Text("Alle aktuellen Daten werden durch den Stand vom ${f.name.removePrefix("auto_").removeSuffix(".json")} ersetzt.") },
            confirmButton = { TextButton(onClick = { vm.restoreLocal(f) { toast(it) }; restoreFile = null }) { Text("Wiederherstellen") } },
            dismissButton = { TextButton(onClick = { restoreFile = null }) { Text("Abbrechen") } },
        )
    }
}

