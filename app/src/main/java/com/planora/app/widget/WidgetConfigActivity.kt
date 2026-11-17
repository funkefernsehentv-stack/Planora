package com.planora.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.planora.app.data.AppSettings
import com.planora.app.data.Defaults
import com.planora.app.data.Graph
import com.planora.app.ui.theme.PlanoraTheme
import kotlinx.coroutines.launch

/** Konfiguration aller Widget-Typen (Auswahl des Eintrags und Gestaltung). */
class WidgetConfigActivity : ComponentActivity() {

    private data class Option(val id: Long, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className ?: ""
        val kind = when {
            provider.endsWith("CountdownListWidgetReceiver") -> "countdown_list"
            provider.endsWith("CountdownWidgetReceiver") -> "countdown"
            provider.endsWith("CalendarWidgetReceiver") -> "calendar"
            provider.endsWith("ChecklistWidgetReceiver") -> "checklist"
            provider.endsWith("NoteWidgetReceiver") -> "note"
            else -> "other"
        }
        val widget: GlanceAppWidget = when (kind) {
            "countdown_list" -> CountdownListWidget()
            "countdown" -> CountdownWidget()
            "calendar" -> CalendarWidget()
            "checklist" -> ChecklistWidget()
            "note" -> NoteWidget()
            else -> QuickNoteWidget()
        }

        setContent {
            val settings by Graph.settings.flow.collectAsState(initial = AppSettings())
            PlanoraTheme(settings) {
                ConfigScreen(kind, settings, appWidgetId, widget)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun ConfigScreen(kind: String, s: AppSettings, appWidgetId: Int, widget: GlanceAppWidget) {
        val scope = rememberCoroutineScope()
        var options by remember { mutableStateOf<List<Option>>(emptyList()) }
        var itemId by remember { mutableLongStateOf(0L) }
        var alpha by remember { mutableFloatStateOf(s.widgetAlpha) }
        var font by remember { mutableFloatStateOf(s.widgetFontScale) }
        var corner by remember { mutableIntStateOf(s.widgetCorner) }
        var units by remember { mutableIntStateOf(0) }
        var bgColor by remember { mutableIntStateOf(0) }
        var count by remember { mutableIntStateOf(6) }
        var mode by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            options = when (kind) {
                "countdown" -> Graph.db.countdowns().all().map { Option(it.id, "${it.emoji} ${it.title}") }
                "checklist" -> Graph.db.checklists().all().sortedBy { it.sortIndex }.map { Option(it.id, "${it.emoji} ${it.title}") }
                "note" -> Graph.db.notes().all().sortedByDescending { it.updatedAt }.map { Option(it.id, it.title.ifBlank { "Ohne Titel" }) }
                else -> emptyList()
            }
            // vorhandene Einstellungen laden (Neukonfiguration)
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                prefs[WidgetKeys.ITEM_ID]?.let { itemId = it }
                prefs[WidgetKeys.ALPHA]?.let { alpha = it }
                prefs[WidgetKeys.FONT]?.let { font = it }
                prefs[WidgetKeys.CORNER]?.let { corner = it }
                prefs[WidgetKeys.UNITS]?.let { units = it }
                prefs[WidgetKeys.BG_COLOR]?.let { bgColor = it }
                prefs[WidgetKeys.COUNT]?.let { count = it }
                prefs[WidgetKeys.MODE]?.let { mode = it }
            }
            if (itemId == 0L) options.firstOrNull()?.let { itemId = it.id }
        }

        fun save() {
            scope.launch {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { p ->
                    if (itemId != 0L) p[WidgetKeys.ITEM_ID] = itemId
                    p[WidgetKeys.ALPHA] = alpha
                    p[WidgetKeys.FONT] = font
                    p[WidgetKeys.CORNER] = corner
                    p[WidgetKeys.UNITS] = units
                    p[WidgetKeys.BG_COLOR] = bgColor
                    p[WidgetKeys.COUNT] = count
                    p[WidgetKeys.MODE] = mode
                }
                widget.update(this@WidgetConfigActivity, glanceId)
                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                finish()
            }
        }

        val title = when (kind) {
            "countdown" -> "Countdown-Widget"
            "countdown_list" -> "Countdown-Liste"
            "calendar" -> "Kalender-Widget"
            "checklist" -> "Checklisten-Widget"
            "note" -> "Notiz-Widget"
            else -> "Widget"
        }

        Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { pad ->
            LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (options.isNotEmpty()) {
                    item { Text("Eintrag auswählen", style = MaterialTheme.typography.titleMedium) }
                    items(options, key = { it.id }) { o ->
                        Card(
                            Modifier.fillMaxWidth().clickable { itemId = o.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (o.id == itemId) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) { Text(o.label, Modifier.padding(14.dp), fontWeight = if (o.id == itemId) FontWeight.Bold else FontWeight.Normal) }
                    }
                } else if (kind in listOf("countdown", "checklist", "note")) {
                    item { Text("Noch keine Einträge vorhanden – das Widget zeigt automatisch den ersten Eintrag, sobald einer existiert.") }
                }
                if (kind == "countdown") {
                    item {
                        Text("Angezeigte Zeiteinheiten", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Wie Countdown", "Tage", "Tage + Std", "Wochen + Tage", "T + Std + Min").forEachIndexed { i, l ->
                                FilterChip(selected = units == i, onClick = { units = i }, label = { Text(l) })
                            }
                        }
                        Text("Hinweis: Android aktualisiert Widgets etwa alle 15 Minuten, daher ohne Sekunden.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (kind == "countdown_list") {
                    item {
                        Text("Anzahl Countdowns: $count", style = MaterialTheme.typography.titleMedium)
                        Slider(value = count.toFloat(), onValueChange = { count = it.toInt() }, valueRange = 2f..12f, steps = 9)
                    }
                }
                if (kind == "calendar") {
                    item {
                        Text("Ansicht", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Monat", "Heute", "Nächste Termine", "Woche").forEachIndexed { i, l ->
                                FilterChip(selected = mode == i, onClick = { mode = i }, label = { Text(l) })
                            }
                        }
                    }
                }
                item {
                    Text("Hintergrundfarbe", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { bgColor = 0 },
                            contentAlignment = Alignment.Center,
                        ) { Text(if (bgColor == 0) "✓" else "A") }
                        (Defaults.vivid + listOf(0xFF1E1B2E.toInt())).forEach { c ->
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Color(c)).clickable { bgColor = c },
                                contentAlignment = Alignment.Center,
                            ) { if (bgColor == c) Text("✓", color = if (c == 0xFFFFFFFF.toInt()) Color.Black else Color.White) }
                        }
                    }
                    Text("„A“ = automatisch (Design des Eintrags)", style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text("Deckkraft: ${(alpha * 100).toInt()} %", style = MaterialTheme.typography.titleMedium)
                    Slider(value = alpha, onValueChange = { alpha = it }, valueRange = 0.15f..1f)
                    Text("Schriftgröße: ${(font * 100).toInt()} %", style = MaterialTheme.typography.titleMedium)
                    Slider(value = font, onValueChange = { font = it }, valueRange = 0.7f..1.5f)
                    Text("Ecken: $corner dp", style = MaterialTheme.typography.titleMedium)
                    Slider(value = corner.toFloat(), onValueChange = { corner = it.toInt() }, valueRange = 0f..36f)
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("Widget speichern")
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
