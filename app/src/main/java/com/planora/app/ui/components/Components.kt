package com.planora.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.horizontalScroll
import coil.compose.AsyncImage
import com.planora.app.data.CountdownStyle
import com.planora.app.data.Defaults
import com.planora.app.data.ImageStore
import com.planora.app.data.RepeatRule
import com.planora.app.data.TagEntity
import com.planora.app.domain.DateLogic
import com.planora.app.widget.PatternPainter
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// ---------------------------------------------------------------------------------------------
// Allgemeine Bausteine
// ---------------------------------------------------------------------------------------------

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.padding(top = 14.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
    )
}

@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) { Column(Modifier.padding(14.dp)) { content() } }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (v, label) ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label) })
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------
// Emoji-Auswahl
// ---------------------------------------------------------------------------------------------

@Composable
fun EmojiButton(emoji: String, size: Dp = 52.dp, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { open = true },
        contentAlignment = Alignment.Center,
    ) { Text(emoji.ifBlank { "➕" }, fontSize = (size.value * 0.5f).sp) }
    if (open) EmojiPickerDialog(onDismiss = { open = false }, onPick = { onPick(it); open = false })
}

@Composable
fun EmojiPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { if (custom.isNotBlank()) onPick(custom.trim()) else onDismiss() }) {
                Text(if (custom.isNotBlank()) "Übernehmen" else "Schließen")
            }
        },
        title = { Text("Emoji auswählen") },
        text = {
            Column {
                OutlinedTextField(
                    value = custom, onValueChange = { if (it.length <= 12) custom = it },
                    label = { Text("Eigenes Emoji / Symbol eingeben") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                    EmojiData.categories.forEachIndexed { i, (name, list) ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text("${list.first()} $name") })
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(44.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(top = 8.dp),
                ) {
                    items(EmojiData.categories[tab].second) { e ->
                        Box(Modifier.size(44.dp).clip(CircleShape).clickable { onPick(e) }, contentAlignment = Alignment.Center) {
                            Text(e, fontSize = 24.sp)
                        }
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Farbauswahl inkl. eigener Farben
// ---------------------------------------------------------------------------------------------

@Composable
fun ColorPalette(selected: Int?, onSelect: (Int) -> Unit, extra: List<Int> = emptyList()) {
    var custom by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                .clickable { custom = true },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Palette, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        (extra + Defaults.vivid).distinct().forEach { c ->
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                    .border(if (selected == c) 3.dp else 1.dp, if (selected == c) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.1f), CircleShape)
                    .clickable { onSelect(c) },
            )
        }
    }
    if (custom) ColorPickerDialog(initial = selected ?: Defaults.vivid[0], onDismiss = { custom = false }, onPick = { onSelect(it); custom = false })
}

@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    var hex by remember { mutableStateOf("") }
    val color = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(color) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Eigene Farbe") },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(color)))
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(
                        Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }),
                    ),
                )
                LabeledSlider("Farbton", h, 0f..360f, "${h.toInt()}°") { h = it }
                LabeledSlider("Sättigung", s, 0f..1f, "${(s * 100).toInt()} %") { s = it }
                LabeledSlider("Helligkeit", v, 0f..1f, "${(v * 100).toInt()} %") { v = it }
                OutlinedTextField(
                    value = hex, onValueChange = { t ->
                        hex = t
                        runCatching { android.graphics.Color.parseColor(if (t.startsWith("#")) t else "#$t") }.getOrNull()?.let {
                            val arr = FloatArray(3); android.graphics.Color.colorToHSV(it, arr); h = arr[0]; s = arr[1]; v = arr[2]
                        }
                    },
                    label = { Text("Hex, z. B. #FF4F7B") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Tags
// ---------------------------------------------------------------------------------------------

@Composable
fun TagChip(tag: TagEntity, small: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color(tag.color).copy(alpha = 0.18f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = if (small) 6.dp else 10.dp, vertical = if (small) 2.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tag.emoji, fontSize = if (small) 10.sp else 13.sp)
        Spacer(Modifier.width(3.dp))
        Text(tag.name, fontSize = if (small) 10.sp else 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(tagIds: List<Long>, tags: List<TagEntity>, small: Boolean = true) {
    val map = tags.associateBy { it.id }
    val list = tagIds.mapNotNull { map[it] }
    if (list.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        list.forEach { TagChip(it, small) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelector(
    allTags: List<TagEntity>,
    selected: List<Long>,
    onChange: (List<Long>) -> Unit,
    onCreate: (TagEntity, (Long) -> Unit) -> Unit,
) {
    var create by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        allTags.forEach { t ->
            val on = t.id in selected
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) selected - t.id else selected + t.id) },
                label = { Text("${t.emoji} ${t.name}") },
            )
        }
        AssistChip(onClick = { create = true }, label = { Text("Neuer Tag") }, leadingIcon = { Icon(Icons.Default.Add, null) })
    }
    if (create) {
        TagEditDialog(null, onDismiss = { create = false }) { t ->
            onCreate(t) { id -> onChange(selected + id) }
            create = false
        }
    }
}

@Composable
fun TagEditDialog(tag: TagEntity?, onDismiss: () -> Unit, onSave: (TagEntity) -> Unit) {
    var name by remember { mutableStateOf(tag?.name ?: "") }
    var emoji by remember { mutableStateOf(tag?.emoji ?: "🏷️") }
    var color by remember { mutableIntStateOf(tag?.color ?: Defaults.vivid.random()) }
    var desc by remember { mutableStateOf(tag?.description ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave((tag ?: TagEntity(name = "")).copy(name = name.trim(), emoji = emoji, color = color, description = desc.trim()))
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text(if (tag == null) "Neuer Tag" else "Tag bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiButton(emoji) { emoji = it }
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                }
                ColorPalette(color, { color = it })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Beschreibung (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Datum, Uhrzeit, Erinnerungen, Wiederholungen
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogM(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogM(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        text = { TimePicker(state = state) },
    )
}

@Composable
fun DateTimeRow(label: String, date: LocalDate, time: LocalTime?, onDate: (LocalDate) -> Unit, onTime: ((LocalTime) -> Unit)?) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) { Text(DateLogic.formatDay(date)) }
        if (time != null && onTime != null) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { pickTime = true }) { Text("%02d:%02d".format(time.hour, time.minute)) }
        }
    }
    if (pickDate) DatePickerDialogM(date, { pickDate = false }) { onDate(it); pickDate = false }
    if (pickTime && time != null && onTime != null) TimePickerDialogM(time, { pickTime = false }) { onTime(it); pickTime = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditor(reminders: List<Long>, presets: List<Long>, onChange: (List<Long>) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        presets.forEach { m ->
            val on = m in reminders
            FilterChip(selected = on, onClick = { onChange(if (on) reminders - m else (reminders + m).sorted()) },
                label = { Text(DateLogic.describeOffset(m)) })
        }
        reminders.filter { it !in presets }.forEach { m ->
            InputChip(selected = true, onClick = { onChange(reminders - m) }, label = { Text(DateLogic.describeOffset(m)) },
                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
        }
        AssistChip(onClick = { custom = true }, label = { Text("Eigene Zeit") }, leadingIcon = { Icon(Icons.Default.Add, null) })
    }
    if (custom) {
        var amount by remember { mutableStateOf("2") }
        var unit by remember { mutableIntStateOf(1) }
        val units = listOf("Minuten" to 1L, "Stunden" to 60L, "Tage" to 1440L, "Wochen" to 10080L)
        AlertDialog(
            onDismissRequest = { custom = false },
            confirmButton = {
                TextButton(onClick = {
                    amount.toLongOrNull()?.takeIf { it >= 0 }?.let { a -> onChange((reminders + a * units[unit].second).distinct().sorted()) }
                    custom = false
                }) { Text("Hinzufügen") }
            },
            dismissButton = { TextButton(onClick = { custom = false }) { Text("Abbrechen") } },
            title = { Text("Eigene Erinnerung") },
            text = {
                Column {
                    OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(4) }, label = { Text("Anzahl") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    ChoiceChips(units.indices.map { it to units[it].first }, unit) { unit = it }
                    Text("vorher", style = MaterialTheme.typography.bodySmall)
                }
            },
        )
    }
}

@Composable
fun RepeatEditor(rule: RepeatRule, onChange: (RepeatRule) -> Unit, allowDaily: Boolean = true) {
    var pickUntil by remember { mutableStateOf(false) }
    val custom = rule.isRepeating && rule.interval > 1
    val options = buildList {
        add(0 to "Keine")
        if (allowDaily) add(1 to "Täglich")
        add(2 to "Wöchentlich"); add(3 to "Monatlich"); add(4 to "Jährlich"); add(5 to "Benutzerdefiniert")
    }
    val selected = if (custom) 5 else rule.type
    Column {
        ChoiceChips(options, selected) { v ->
            onChange(
                when (v) {
                    5 -> RepeatRule(if (rule.isRepeating) rule.type else RepeatRule.WEEKLY, maxOf(2, rule.interval), rule.until)
                    else -> RepeatRule(v, 1, if (v == 0) null else rule.until)
                },
            )
        }
        if (custom) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("Alle ")
                OutlinedTextField(
                    value = rule.interval.toString(),
                    onValueChange = { s -> s.filter(Char::isDigit).take(3).toIntOrNull()?.let { onChange(rule.copy(interval = it.coerceAtLeast(1))) } },
                    modifier = Modifier.width(72.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.width(8.dp))
            }
            ChoiceChips(
                (if (allowDaily) listOf(1 to "Tage") else emptyList()) + listOf(2 to "Wochen", 3 to "Monate", 4 to "Jahre"),
                rule.type,
            ) { onChange(rule.copy(type = it)) }
        }
        if (rule.isRepeating) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Endet: ", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { pickUntil = true }) {
                    Text(rule.until?.let { DateLogic.formatDay(LocalDate.parse(it)) } ?: "Nie")
                }
                if (rule.until != null) TextButton(onClick = { onChange(rule.copy(until = null)) }) { Text("Entfernen") }
            }
        }
    }
    if (pickUntil) {
        DatePickerDialogM(rule.until?.let { LocalDate.parse(it) } ?: LocalDate.now().plusMonths(6), { pickUntil = false }) {
            onChange(rule.copy(until = it.toString())); pickUntil = false
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bilder & Hintergründe
// ---------------------------------------------------------------------------------------------

@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
fun imageFile(name: String?): File? {
    val ctx = LocalContext.current
    return remember(name) { ImageStore.file(ctx, name) }
}

@Composable
fun ImagePickRow(imageName: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    val file = imageFile(imageName)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (file != null) {
            AsyncImage(model = file, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(10.dp))
        }
        OutlinedButton(onClick = onPick) {
            Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp)); Text(if (file == null) "Bild wählen" else "Ändern")
        }
        if (file != null) TextButton(onClick = onRemove) { Text("Entfernen") }
    }
}

@Composable
fun PatternLayer(patternId: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawIntoCanvas { c -> PatternPainter.draw(c.nativeCanvas, size.width, size.height, patternId) }
    }
}

fun gradientBrush(c1: Int, c2: Int, direction: Int): Brush = when (direction) {
    0 -> Brush.horizontalGradient(listOf(Color(c1), Color(c2)))
    1 -> Brush.verticalGradient(listOf(Color(c1), Color(c2)))
    else -> Brush.linearGradient(listOf(Color(c1), Color(c2)))
}

/** Vollständiger Kartenhintergrund nach CountdownStyle (Farbe, Verlauf, Bild, Muster, Filter, Transparenzverlauf). */
@Composable
fun StyledBackground(style: CountdownStyle, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val file = imageFile(style.imageName)
    Box(modifier) {
        Box(Modifier.matchParentSize().alpha(style.alpha)) {
            when (style.bgType) {
                1, 3 -> {
                    Box(Modifier.fillMaxSize().background(gradientBrush(style.color, style.color2, style.gradientDirection)))
                    if (style.bgType == 3) PatternLayer(style.patternId, Modifier.fillMaxSize())
                }
                2 -> {
                    Box(Modifier.fillMaxSize().background(Color(style.color)))
                    if (file != null) {
                        val b = style.brightness
                        AsyncImage(
                            model = file, contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = style.zoom; scaleY = style.zoom },
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(style.focusX, style.focusY),
                            colorFilter = if (b != 1f) ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(b, b, b, 1f) }) else null,
                        )
                    }
                }
                else -> Box(Modifier.fillMaxSize().background(Color(style.color)))
            }
            if (style.tintAlpha > 0f) Box(Modifier.fillMaxSize().background(Color(style.tintColor).copy(alpha = style.tintAlpha)))
            if (style.overlayEnabled) {
                val oc = Color(style.overlayColor).copy(alpha = style.overlayIntensity)
                val tr = oc.copy(alpha = 0f)
                val brush = when (style.overlayDirection) {
                    1 -> Brush.verticalGradient(listOf(oc, tr))
                    2 -> Brush.horizontalGradient(listOf(tr, oc))
                    3 -> Brush.horizontalGradient(listOf(oc, tr))
                    else -> Brush.verticalGradient(listOf(tr, oc))
                }
                Box(Modifier.fillMaxSize().background(brush))
            }
        }
        content()
    }
}

fun Color.argb(): Int = toArgb()

@Composable
fun ColorDot(color: Int, size: Dp = 12.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(color)))
}

@Composable
fun OutlinedPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}
