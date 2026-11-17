package com.planora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.data.CountdownEntity
import com.planora.app.data.CountdownStyle
import com.planora.app.data.Defaults
import com.planora.app.data.RepeatRule
import com.planora.app.data.TimeUnits
import com.planora.app.domain.CountdownLogic
import com.planora.app.domain.DateLogic
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.components.ChoiceChips
import com.planora.app.ui.components.ColorPalette
import com.planora.app.ui.components.CountdownIcons
import com.planora.app.ui.components.DateTimeRow
import com.planora.app.ui.components.EmojiButton
import com.planora.app.ui.components.ImagePickRow
import com.planora.app.ui.components.LabeledSlider
import com.planora.app.ui.components.PatternLayer
import com.planora.app.ui.components.ReminderEditor
import com.planora.app.ui.components.RepeatEditor
import com.planora.app.ui.components.SectionTitle
import com.planora.app.ui.components.SettingsCard
import com.planora.app.ui.components.SwitchRow
import com.planora.app.ui.components.TagSelector
import com.planora.app.ui.components.gradientBrush
import com.planora.app.ui.components.rememberImagePicker
import com.planora.app.ui.components.rememberNow
import com.planora.app.ui.theme.FontFamilies
import com.planora.app.widget.PatternPainter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Vorgefertigte Designs zum schnellen Anwenden. */
private data class DesignPreset(val name: String, val emoji: String, val style: (CountdownStyle) -> CountdownStyle)

private val designPresets = listOf(
    DesignPreset("Klassisch", "🎂") { it.copy(bgType = 0, color = 0xFFE63757.toInt(), textColor = 0xFFFFFFFF.toInt(), layout = 0, showPanel = true) },
    DesignPreset("Geburtstag", "🎈") { it.copy(bgType = 3, patternId = 0, color = 0xFFF7208F.toInt(), color2 = 0xFFFF8A65.toInt(), textColor = 0xFFFFFFFF.toInt()) },
    DesignPreset("Urlaub", "🏝️") { it.copy(bgType = 3, patternId = 2, color = 0xFF1CB5E0.toInt(), color2 = 0xFF00897B.toInt(), textColor = 0xFFFFFFFF.toInt()) },
    DesignPreset("Winter", "❄️") { it.copy(bgType = 3, patternId = 7, color = 0xFF3949AB.toInt(), color2 = 0xFF90CAF9.toInt(), gradientDirection = 1) },
    DesignPreset("Weihnachten", "🎄") { it.copy(bgType = 3, patternId = 3, color = 0xFFC62828.toInt(), color2 = 0xFF2E7D32.toInt()) },
    DesignPreset("Halloween", "🎃") { it.copy(bgType = 1, color = 0xFFFF6F00.toInt(), color2 = 0xFF212121.toInt(), textColor = 0xFFFFFFFF.toInt()) },
    DesignPreset("Liebe", "💖") { it.copy(bgType = 3, patternId = 6, color = 0xFFF48FB1.toInt(), color2 = 0xFFE91E63.toInt()) },
    DesignPreset("Nacht", "🌙") { it.copy(bgType = 3, patternId = 3, color = 0xFF1A237E.toInt(), color2 = 0xFF000000.toInt(), gradientDirection = 1) },
    DesignPreset("Minimal", "◽") { it.copy(bgType = 0, color = 0xFFFFFFFF.toInt(), textColor = 0xFF1F1B24.toInt(), showPanel = true, shadow = true) },
    DesignPreset("Pastell", "🌸") { it.copy(bgType = 1, color = 0xFFFFD1DC.toInt(), color2 = 0xFFC8E7FF.toInt(), textColor = 0xFF3A3A55.toInt()) },
    DesignPreset("Groß", "🖼️") { it.copy(layout = 1, height = 150, autoUnit = false, units = TimeUnits.DAYS or TimeUnits.HOURS or TimeUnits.MINUTES or TimeUnits.SECONDS) },
    DesignPreset("Kompakt", "➖") { it.copy(layout = 2, height = 52) },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CountdownEditorScreen(vm: AppViewModel, nav: Nav, id: Long) {
    val all by vm.countdowns.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val existing = remember(id) { all.firstOrNull { it.id == id } }
    var c by remember(id) {
        mutableStateOf(
            existing ?: CountdownEntity(
                title = "",
                target = DateLogic.format(LocalDate.now().plusDays(7).atTime(18, 0)),
                reminders = settings.defaultCountdownReminders,
                style = CountdownStyle(color = Defaults.vivid[(all.size) % 8]),
            ),
        )
    }
    var tab by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val now = rememberNow(1000)
    val target = DateLogic.parse(c.target) ?: LocalDateTime.now()
    val type = when {
        c.mode == CountdownLogic.MODE_UP -> 1
        c.repeat.isRepeating -> 2
        else -> 0
    }
    fun st(f: (CountdownStyle) -> CountdownStyle) { c = c.copy(style = f(c.style)) }
    val pickImage = rememberImagePicker { uri -> vm.importImage(uri) { name -> if (name != null) st { it.copy(bgType = 2, imageName = name) } } }
    val pickThumb = rememberImagePicker { uri -> vm.importImage(uri) { name -> if (name != null) c = c.copy(thumbnail = name) } }

    fun save() {
        vm.saveCountdown(c.copy(title = c.title.ifBlank { "Neuer Countdown" }))
        nav.back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Neuer Countdown" else "Countdown bearbeiten") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = {
                    if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Löschen") }
                    TextButton(onClick = { save() }) { Icon(Icons.Default.Check, null); Text(" Speichern") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
            // Live-Vorschau
            Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)) {
                CountdownCard(c.copy(title = c.title.ifBlank { "Vorschau" }), now, tags)
            }
            TabRow(selectedTabIndex = tab) {
                listOf("Allgemein", "Hintergrund", "Gestaltung").forEachIndexed { i, l ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> {
                        item {
                            SettingsCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EmojiButton(c.emoji) { e -> c = c.copy(emoji = e, icon = null) }
                                    Spacer(Modifier.width(12.dp))
                                    OutlinedTextField(c.title, { c = c.copy(title = it) }, label = { Text("Titel") },
                                        singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    c.description, { if (it.length <= 200) c = c.copy(description = it) },
                                    label = { Text("Beschreibung") }, supportingText = { Text("${c.description.length}/200") },
                                    modifier = Modifier.fillMaxWidth(), maxLines = 4,
                                )
                                OutlinedTextField(c.category, { c = c.copy(category = it) }, label = { Text("Kategorie (optional)") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        item {
                            SectionTitle("Symbol & Vorschaubild")
                            SettingsCard {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(selected = c.icon == null, onClick = { c = c.copy(icon = null) }, label = { Text("Emoji ${c.emoji}") })
                                    CountdownIcons.all.forEach { (name, icon) ->
                                        FilterChip(selected = c.icon == name, onClick = { c = c.copy(icon = name) }, label = { Icon(icon, name) })
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Eigenes Vorschaubild (ersetzt Emoji/Symbol)", style = MaterialTheme.typography.bodySmall)
                                ImagePickRow(c.thumbnail, onPick = pickThumb, onRemove = { c = c.copy(thumbnail = null) })
                            }
                        }
                        item {
                            SectionTitle("Typ & Zeitpunkt")
                            SettingsCard {
                                ChoiceChips(listOf(0 to "⏳ Countdown", 1 to "↗️ Hochzählen seit", 2 to "🔁 Wiederkehrend"), type) { t ->
                                    c = when (t) {
                                        1 -> c.copy(mode = CountdownLogic.MODE_UP, repeat = RepeatRule())
                                        2 -> c.copy(mode = CountdownLogic.MODE_DOWN, repeat = if (c.repeat.isRepeating) c.repeat else RepeatRule(RepeatRule.YEARLY))
                                        else -> c.copy(mode = CountdownLogic.MODE_DOWN, repeat = RepeatRule())
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                DateTimeRow(
                                    if (type == 1) "Seit" else "Datum", target.toLocalDate(), target.toLocalTime(),
                                    onDate = { d -> c = c.copy(target = DateLogic.format(d.atTime(target.toLocalTime()))) },
                                    onTime = { t -> c = c.copy(target = DateLogic.format(target.toLocalDate().atTime(t))) },
                                )
                                TextButton(onClick = { c = c.copy(target = DateLogic.format(target.toLocalDate().atTime(LocalTime.MIDNIGHT))) }) {
                                    Text("Ohne Uhrzeit (ganzer Tag)")
                                }
                                if (type != 1) {
                                    val end = DateLogic.parse(c.endDate)
                                    if (end == null) {
                                        TextButton(onClick = { c = c.copy(endDate = DateLogic.format(target.plusDays(3))) }) { Text("+ Enddatum (z. B. für Urlaub)") }
                                    } else {
                                        DateTimeRow("Bis", end.toLocalDate(), null,
                                            onDate = { d -> c = c.copy(endDate = DateLogic.format(d.atTime(end.toLocalTime()))) }, onTime = null)
                                        TextButton(onClick = { c = c.copy(endDate = null) }) { Text("Enddatum entfernen") }
                                    }
                                }
                                if (type == 2) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Wiederholung", style = MaterialTheme.typography.titleSmall)
                                    RepeatEditor(c.repeat, { r -> c = c.copy(repeat = if (r.isRepeating) r else RepeatRule(RepeatRule.YEARLY)) })
                                    Text("Monatslängen und Schaltjahre werden automatisch berücksichtigt (z. B. 31. → 30./28./29.).",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (type != 1) item {
                            SectionTitle("Erinnerungen")
                            SettingsCard {
                                ReminderEditor(c.reminders, listOf(43200L, 20160L, 10080L, 4320L, 1440L, 60L, 0L)) { c = c.copy(reminders = it) }
                            }
                        }
                        item {
                            SectionTitle("Tags")
                            SettingsCard {
                                TagSelector(tags, c.tags, { c = c.copy(tags = it) }) { t, done -> vm.saveTag(t, done) }
                            }
                        }
                        item {
                            SettingsCard {
                                SwitchRow("⭐ Favorit", c.favorite) { c = c.copy(favorite = it) }
                                SwitchRow("📌 Oben anheften", c.pinned) { c = c.copy(pinned = it) }
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                    1 -> {
                        item {
                            SectionTitle("Vorgefertigte Designs")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                designPresets.forEach { p ->
                                    val ps = p.style(c.style)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { c = c.copy(style = ps) }) {
                                        Box(
                                            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                                                .background(if (ps.bgType == 0) androidx.compose.ui.graphics.SolidColor(Color(ps.color)) else gradientBrush(ps.color, ps.color2, ps.gradientDirection)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (ps.bgType == 3) PatternLayer(ps.patternId, Modifier.fillMaxSize())
                                            Text(p.emoji, style = MaterialTheme.typography.headlineSmall)
                                        }
                                        Text(p.name, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        item {
                            SectionTitle("Hintergrundart")
                            SettingsCard {
                                ChoiceChips(listOf(0 to "Farbe", 1 to "Farbverlauf", 2 to "Eigenes Bild", 3 to "Muster"), c.style.bgType) { v -> st { it.copy(bgType = v) } }
                                Spacer(Modifier.height(8.dp))
                                Text(if (c.style.bgType == 2) "Grundfarbe" else "Farbe 1", style = MaterialTheme.typography.bodyMedium)
                                ColorPalette(c.style.color, { v -> st { it.copy(color = v) } })
                                if (c.style.bgType == 1 || c.style.bgType == 3) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Farbe 2", style = MaterialTheme.typography.bodyMedium)
                                    ColorPalette(c.style.color2, { v -> st { it.copy(color2 = v) } })
                                    Text("Richtung", style = MaterialTheme.typography.bodyMedium)
                                    ChoiceChips(listOf(0 to "↔ Horizontal", 1 to "↕ Vertikal", 2 to "↘ Diagonal"), c.style.gradientDirection) { v -> st { it.copy(gradientDirection = v) } }
                                }
                                if (c.style.bgType == 3) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Muster", style = MaterialTheme.typography.bodyMedium)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PatternPainter.names.forEachIndexed { i, n ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { st { it.copy(patternId = i) } }) {
                                                Box(
                                                    Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                                        .background(gradientBrush(c.style.color, c.style.color2, c.style.gradientDirection))
                                                        .then(if (c.style.patternId == i) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier),
                                                ) { PatternLayer(i, Modifier.fillMaxSize()) }
                                                Text(n, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                if (c.style.bgType == 2) {
                                    Spacer(Modifier.height(6.dp))
                                    ImagePickRow(c.style.imageName, onPick = pickImage, onRemove = { st { it.copy(imageName = null, bgType = 0) } })
                                    if (c.style.imageName != null) {
                                        Text("Zuschneiden & Positionieren", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                                        LabeledSlider("Zoom", c.style.zoom, 1f..3f, "${(c.style.zoom * 100).toInt()} %") { v -> st { it.copy(zoom = v) } }
                                        LabeledSlider("Horizontal", c.style.focusX, -1f..1f, "%.1f".format(c.style.focusX)) { v -> st { it.copy(focusX = v) } }
                                        LabeledSlider("Vertikal", c.style.focusY, -1f..1f, "%.1f".format(c.style.focusY)) { v -> st { it.copy(focusY = v) } }
                                        LabeledSlider("Helligkeit", c.style.brightness, 0.3f..1.6f, "${(c.style.brightness * 100).toInt()} %") { v -> st { it.copy(brightness = v) } }
                                    }
                                }
                            }
                        }
                        item {
                            SectionTitle("Transparenz & Filter")
                            SettingsCard {
                                LabeledSlider("Deckkraft der Karte", c.style.alpha, 0.2f..1f, "${(c.style.alpha * 100).toInt()} %") { v -> st { it.copy(alpha = v) } }
                                Text("Farbfilter", style = MaterialTheme.typography.bodyMedium)
                                ColorPalette(c.style.tintColor, { v -> st { it.copy(tintColor = v, tintAlpha = if (it.tintAlpha == 0f) 0.3f else it.tintAlpha) } })
                                LabeledSlider("Filterstärke", c.style.tintAlpha, 0f..0.9f, "${(c.style.tintAlpha * 100).toInt()} %") { v -> st { it.copy(tintAlpha = v) } }
                            }
                        }
                        item {
                            SectionTitle("Transparenter Verlauf (für Lesbarkeit)")
                            SettingsCard {
                                SwitchRow("Verlauf über den Hintergrund legen", c.style.overlayEnabled) { v -> st { it.copy(overlayEnabled = v) } }
                                if (c.style.overlayEnabled) {
                                    ChoiceChips(listOf(0 to "Nach unten dunkler", 1 to "Nach oben", 2 to "Nach rechts", 3 to "Nach links"), c.style.overlayDirection) { v -> st { it.copy(overlayDirection = v) } }
                                    LabeledSlider("Intensität", c.style.overlayIntensity, 0.1f..1f, "${(c.style.overlayIntensity * 100).toInt()} %") { v -> st { it.copy(overlayIntensity = v) } }
                                    Text("Verlaufsfarbe", style = MaterialTheme.typography.bodyMedium)
                                    ColorPalette(c.style.overlayColor, { v -> st { it.copy(overlayColor = v) } }, extra = listOf(0xFF000000.toInt()))
                                }
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                    else -> {
                        item {
                            SectionTitle("Zeiteinheiten")
                            SettingsCard {
                                SwitchRow("Automatisch (wie im Referenzdesign)", c.style.autoUnit,
                                    subtitle = "Zeigt Tage, bei weniger als einem Tag Stunden, dann Minuten") { v -> st { it.copy(autoUnit = v) } }
                                if (!c.style.autoUnit) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TimeUnits.ALL.forEach { u ->
                                            val on = c.style.units and u != 0
                                            FilterChip(selected = on, onClick = {
                                                st { s -> val n = if (on) s.units and u.inv() else s.units or u; s.copy(units = if (n == 0) TimeUnits.DAYS else n) }
                                            }, label = { Text(TimeUnits.name(u)) })
                                        }
                                    }
                                    Text("Beispiele: Monate + Wochen + Tage, nur Wochen + Tage, Tage + Std + Min + Sek …",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item {
                            SectionTitle("Layout")
                            SettingsCard {
                                ChoiceChips(listOf(0 to "Klassisch", 1 to "Groß", 2 to "Kompakt", 3 to "Zentriert"), c.style.layout) { v ->
                                    st { it.copy(layout = v, height = when (v) { 1 -> maxOf(it.height, 140); 2 -> 52; else -> if (it.height < 70) 80 else it.height }) }
                                }
                                Text("Kartenform", style = MaterialTheme.typography.bodyMedium)
                                ChoiceChips(listOf(0 to "Abgerundet", 1 to "Abgeschrägt", 2 to "Pille", 3 to "Eckig"), c.style.cardShape) { v -> st { it.copy(cardShape = v) } }
                                SwitchRow("Emoji rechts", c.style.emojiRight) { v -> st { it.copy(emojiRight = v) } }
                                Text("Position des Countdowns", style = MaterialTheme.typography.bodyMedium)
                                ChoiceChips(listOf(0 to "Rechts", 1 to "Unter dem Titel"), c.style.timePosition) { v -> st { it.copy(timePosition = v) } }
                                Text("Position des Titels", style = MaterialTheme.typography.bodyMedium)
                                ChoiceChips(listOf(0 to "Links", 1 to "Zentriert"), c.style.titleAlign) { v -> st { it.copy(titleAlign = v) } }
                                SwitchRow("Abgesetztes Zeitfeld rechts", c.style.showPanel) { v -> st { it.copy(showPanel = v) } }
                                SwitchRow("Datum anzeigen", c.style.showDate) { v -> st { it.copy(showDate = v) } }
                                SwitchRow("Beschreibung anzeigen", c.style.showDescription) { v -> st { it.copy(showDescription = v) } }
                                SwitchRow("Schatten", c.style.shadow) { v -> st { it.copy(shadow = v) } }
                            }
                        }
                        item {
                            SectionTitle("Größe & Abstände")
                            SettingsCard {
                                LabeledSlider("Kartenhöhe", c.style.height.toFloat(), 44f..260f, "${c.style.height} dp") { v -> st { it.copy(height = v.toInt()) } }
                                LabeledSlider("Kartenbreite", c.style.widthFraction, 0.5f..1f, "${(c.style.widthFraction * 100).toInt()} %") { v -> st { it.copy(widthFraction = v) } }
                                LabeledSlider("Eckenradius", c.style.cornerRadius.toFloat(), 0f..40f, "${c.style.cornerRadius} dp") { v -> st { it.copy(cornerRadius = v.toInt()) } }
                                LabeledSlider("Innenabstand", c.style.padding.toFloat(), 4f..28f, "${c.style.padding} dp") { v -> st { it.copy(padding = v.toInt()) } }
                            }
                        }
                        item {
                            SectionTitle("Schrift")
                            SettingsCard {
                                Text("Schriftfarbe", style = MaterialTheme.typography.bodyMedium)
                                ColorPalette(c.style.textColor, { v -> st { it.copy(textColor = v) } }, extra = listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt()))
                                Text("Schriftart", style = MaterialTheme.typography.bodyMedium)
                                ChoiceChips(FontFamilies.indices.map { it to FontFamilies[it].first }, c.style.fontFamily) { v -> st { it.copy(fontFamily = v) } }
                                LabeledSlider("Titelgröße", c.style.titleSize.toFloat(), 12f..30f, "${c.style.titleSize} sp") { v -> st { it.copy(titleSize = v.toInt()) } }
                                LabeledSlider("Zahlengröße", c.style.numberSize.toFloat(), 16f..56f, "${c.style.numberSize} sp") { v -> st { it.copy(numberSize = v.toInt()) } }
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog("Countdown löschen?", onDismiss = { confirmDelete = false }) {
            vm.deleteCountdown(id); confirmDelete = false
            if (!nav.controller.popBackStack(com.planora.app.ui.Routes.COUNTDOWNS, false)) nav.back()
        }
    }
}
