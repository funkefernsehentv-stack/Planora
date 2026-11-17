package com.planora.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.planora.app.data.DecorEntity
import com.planora.app.data.Defaults
import com.planora.app.data.EventEntity
import com.planora.app.data.EventStyle
import com.planora.app.data.TagEntity
import com.planora.app.domain.DateLogic
import com.planora.app.domain.EventLogic
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.Routes
import com.planora.app.ui.TopBarActions
import com.planora.app.ui.components.ChoiceChips
import com.planora.app.ui.components.ColorPalette
import com.planora.app.ui.components.DateTimeRow
import com.planora.app.ui.components.EmojiButton
import com.planora.app.ui.components.EmptyState
import com.planora.app.ui.components.ImagePickRow
import com.planora.app.ui.components.LabeledSlider
import com.planora.app.ui.components.PatternLayer
import com.planora.app.ui.components.ReminderEditor
import com.planora.app.ui.components.RepeatEditor
import com.planora.app.ui.components.SectionTitle
import com.planora.app.ui.components.SettingsCard
import com.planora.app.ui.components.SwitchRow
import com.planora.app.ui.components.TagRow
import com.planora.app.ui.components.TagSelector
import com.planora.app.ui.components.imageFile
import com.planora.app.ui.components.rememberImagePicker
import com.planora.app.ui.theme.Spacing
import com.planora.app.ui.theme.onColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle

private fun dayKey(d: LocalDate) = "d:$d"
private fun monthKey(m: YearMonth) = "m:$m"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: AppViewModel, nav: Nav, outer: PaddingValues) {
    val events by vm.events.collectAsStateWithLifecycle()
    val decor by vm.decor.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var selectedEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    fun select(d: LocalDate) { selectedEpoch = d.toEpochDay() }
    val view = settings.calendarView
    var decorKey by remember { mutableStateOf<String?>(null) }
    val decorMap = decor.associateBy { it.key }
    val firstDow = if (settings.weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text("Kalender", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { select(LocalDate.now()) }) { Icon(Icons.Default.Today, "Heute") }
                    TopBarActions(nav)
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.go(Routes.eventEdit(0, selected.toString())) }) { Icon(Icons.Default.Add, "Neuer Termin") }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen())) {
                listOf("Monat", "Woche", "Tag", "Liste").forEachIndexed { i, l ->
                    SegmentedButton(
                        selected = view == i, onClick = { vm.updateSettings { it.copy(calendarView = i) } },
                        shape = SegmentedButtonDefaults.itemShape(i, 4),
                    ) { Text(l, maxLines = 1) }
                }
            }
            // Navigationsleiste (Monat / Woche / Tag)
            if (view != 3) {
                val label = when (view) {
                    0 -> DateLogic.formatMonth(selected)
                    1 -> {
                        var s = selected; while (s.dayOfWeek != firstDow) s = s.minusDays(1)
                        "${DateLogic.formatDayShort(s)} – ${DateLogic.formatDayShort(s.plusDays(6))}"
                    }
                    else -> DateLogic.formatLongDay(selected)
                }
                fun shift(dir: Long) = select(when (view) { 0 -> selected.plusMonths(dir); 1 -> selected.plusWeeks(dir); else -> selected.plusDays(dir) })
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Zurück") }
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { shift(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Weiter") }
                    IconButton(onClick = { decorKey = if (view == 0) monthKey(YearMonth.from(selected)) else dayKey(selected) }) {
                        Icon(Icons.Default.Brush, "Gestalten")
                    }
                }
            }
            AnimatedContent(targetState = view, label = "calendarView") { v ->
                when (v) {
                    0 -> MonthView(events, decorMap, tags, selected, firstDow, settings.calendarStyle, settings.calendarFontScale, nav, vm,
                        onSelect = ::select, onSwipe = { dir -> select(selected.plusMonths(dir)) }, onDecor = { decorKey = dayKey(it) })
                    1 -> WeekView(events, decorMap, tags, selected, firstDow, nav, vm, ::select)
                    2 -> DayView(events, decorMap, tags, selected, nav, vm, onSwipe = { dir -> select(selected.plusDays(dir)) })
                    else -> ListView(events, tags, nav, vm)
                }
            }
        }
    }
    decorKey?.let { key -> DecorDialog(key, decorMap[key], vm) { decorKey = null } }
}

@Composable
private fun DecorBackground(d: DecorEntity?, modifier: Modifier) {
    if (d == null) return
    val file = imageFile(d.imageName)
    Box(modifier) {
        d.color?.let { Box(Modifier.fillMaxSize().background(Color(it).copy(alpha = d.alpha))) }
        if (file != null) AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(d.alpha))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthView(
    events: List<EventEntity>, decor: Map<String, DecorEntity>, tags: List<TagEntity>, selected: LocalDate, firstDow: DayOfWeek,
    style: Int, fontScale: Float, nav: Nav, vm: AppViewModel,
    onSelect: (LocalDate) -> Unit, onSwipe: (Long) -> Unit, onDecor: (LocalDate) -> Unit,
) {
    val month = YearMonth.from(selected)
    var gridStart = month.atDay(1)
    while (gridStart.dayOfWeek != firstDow) gridStart = gridStart.minusDays(1)
    val gridEnd = gridStart.plusDays(41)
    val occ = remember(events, gridStart) { EventLogic.occurrences(events, gridStart, gridEnd) }
    val byDay = remember(occ) { EventLogic.byDay(occ, gridStart, gridEnd) }
    val today = LocalDate.now()
    var drag by remember { mutableFloatStateOf(0f) }
    val selectedOcc = byDay[selected].orEmpty()

    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Box(
                Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(20.dp))
                    .pointerInput(month) {
                        detectHorizontalDragGestures(
                            onDragEnd = { if (drag > 120) onSwipe(-1) else if (drag < -120) onSwipe(1); drag = 0f },
                            onHorizontalDrag = { _, d -> drag += d },
                        )
                    },
            ) {
                DecorBackground(decor[monthKey(month)], Modifier.matchParentSize())
                Column(Modifier.padding(4.dp)) {
                    Row {
                        for (i in 0 until 7) {
                            Text(firstDow.plus(i.toLong()).getDisplayName(JTextStyle.SHORT, DateLogic.locale),
                                Modifier.weight(1f).padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    for (w in 0 until 6) {
                        Row {
                            for (i in 0 until 7) {
                                val day = gridStart.plusDays((w * 7 + i).toLong())
                                val inMonth = day.month == month.month
                                val isSel = day == selected
                                val dayEvents = byDay[day].orEmpty()
                                Box(
                                    Modifier.weight(1f).aspectRatio(0.82f).padding(2.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                                        .combinedClickable(onClick = { onSelect(day) }, onLongClick = { onSelect(day); onDecor(day) }),
                                ) {
                                    DecorBackground(decor[dayKey(day)], Modifier.matchParentSize())
                                    Column(Modifier.fillMaxSize().padding(3.dp).alpha(if (inMonth) 1f else 0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            Modifier.size((24 * fontScale).dp).clip(CircleShape)
                                                .background(if (day == today) MaterialTheme.colorScheme.primary else Color.Transparent),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(day.dayOfMonth.toString(), fontSize = (13 * fontScale).sp,
                                                fontWeight = if (day == today || isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (day == today) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                        decor[dayKey(day)]?.emoji?.let { Text(it, fontSize = 11.sp) }
                                        when (style) {
                                            1 -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                                                dayEvents.take(4).forEach { o -> Box(Modifier.size(6.dp).clip(CircleShape).background(Color(o.event.color))) }
                                            }
                                            2 -> Column(Modifier.fillMaxWidth().padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                dayEvents.take(3).forEach { o ->
                                                    Text(o.event.title, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Clip,
                                                        color = Color(o.event.color).onColor(),
                                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(Color(o.event.color)).padding(horizontal = 2.dp))
                                                }
                                            }
                                            else -> Text(dayEvents.take(3).joinToString("") { it.event.emoji }, fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(DateLogic.formatLongDay(selected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onDecor(selected) }) { Text("🎨 Tag gestalten") }
            }
        }
        if (selectedOcc.isEmpty()) item {
            Text("Keine Termine an diesem Tag", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(selectedOcc, key = { "${it.event.id}_${it.start}" }) { o ->
            EventCard(o, tags, nav, vm, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun WeekView(
    events: List<EventEntity>, decor: Map<String, DecorEntity>, tags: List<TagEntity>, selected: LocalDate, firstDow: DayOfWeek,
    nav: Nav, vm: AppViewModel, onSelect: (LocalDate) -> Unit,
) {
    var start = selected
    while (start.dayOfWeek != firstDow) start = start.minusDays(1)
    val end = start.plusDays(6)
    val byDay = remember(events, start) { EventLogic.byDay(EventLogic.occurrences(events, start, end), start, end) }
    val today = LocalDate.now()
    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items((0..6).map { start.plusDays(it.toLong()) }, key = { it.toEpochDay() }) { day ->
            val evs = byDay[day].orEmpty()
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { onSelect(day) }) {
                DecorBackground(decor[dayKey(day)], Modifier.matchParentSize())
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.dayOfWeek.getDisplayName(JTextStyle.FULL, DateLogic.locale), fontWeight = FontWeight.Bold,
                            color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text(DateLogic.formatDayShort(day), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        decor[dayKey(day)]?.emoji?.let { Text("  $it") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { nav.go(Routes.eventEdit(0, day.toString())) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, "Termin") }
                    }
                    if (evs.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    evs.forEach { o -> EventCard(o, tags, nav, vm, Modifier.padding(top = 6.dp), compact = true) }
                }
            }
        }
    }
}

@Composable
private fun DayView(
    events: List<EventEntity>, decor: Map<String, DecorEntity>, tags: List<TagEntity>, selected: LocalDate,
    nav: Nav, vm: AppViewModel, onSwipe: (Long) -> Unit,
) {
    val occ = remember(events, selected) { EventLogic.byDay(EventLogic.occurrences(events, selected, selected), selected, selected)[selected].orEmpty() }
    val d = decor[dayKey(selected)]
    var drag by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().pointerInput(selected) {
            detectHorizontalDragGestures(
                onDragEnd = { if (drag > 120) onSwipe(-1) else if (drag < -120) onSwipe(1); drag = 0f },
                onHorizontalDrag = { _, x -> drag += x },
            )
        },
    ) {
        // Eigene Hintergrundgestaltung für den geöffneten Tag
        DecorBackground(d?.let { it.copy(alpha = it.alpha * 0.8f) }, Modifier.matchParentSize())
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)) {
            d?.emoji?.let { e -> item { Text(e, fontSize = 48.sp, modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
            val allDay = occ.filter { it.event.allDay || it.start.toLocalDate() != selected }
            if (allDay.isNotEmpty()) {
                item { SectionTitle("Ganztägig") }
                items(allDay, key = { "ad_${it.event.id}_${it.start}" }) { o -> EventCard(o, tags, nav, vm, Modifier.padding(vertical = 3.dp)) }
            }
            val timed = occ - allDay.toSet()
            items((0..23).toList(), key = { "h$it" }) { h ->
                val atHour = timed.filter { it.start.hour == h }
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                    Text("%02d:00".format(h), Modifier.width(52.dp).padding(top = 4.dp), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(
                        Modifier.weight(1f).padding(vertical = 2.dp)
                            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                            .clickable { nav.go(Routes.eventEdit(0, selected.atTime(h, 0).toString())) }
                            .padding(4.dp),
                    ) {
                        if (atHour.isEmpty()) Spacer(Modifier.height(30.dp))
                        atHour.forEach { o -> EventCard(o, tags, nav, vm, Modifier.padding(vertical = 2.dp), compact = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListView(events: List<EventEntity>, tags: List<TagEntity>, nav: Nav, vm: AppViewModel) {
    var past by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val from = if (past) today.minusDays(180) else today
    val to = if (past) today.minusDays(1) else today.plusDays(180)
    val occ = remember(events, past) {
        EventLogic.occurrences(events, from, to).let { if (past) it.sortedByDescending { o -> o.start } else it.sortedBy { o -> o.start } }
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
        item {
            ChoiceChips(listOf(false to "Kommende", true to "Vergangene"), past) { past = it }
        }
        if (occ.isEmpty()) item { EmptyState("📅", if (past) "Keine vergangenen Termine" else "Keine kommenden Termine", "Lege mit + einen neuen Termin an.") }
        var lastDay: LocalDate? = null
        occ.forEach { o ->
            val day = o.start.toLocalDate()
            if (day != lastDay) {
                lastDay = day
                item(key = "day_$day") { SectionTitle(DateLogic.formatLongDay(day)) }
            }
            item(key = "o_${o.event.id}_${o.start}") { EventCard(o, tags, nav, vm, Modifier.padding(vertical = 3.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(o: EventLogic.Occurrence, tags: List<TagEntity>, nav: Nav, vm: AppViewModel, modifier: Modifier = Modifier, compact: Boolean = false) {
    val e = o.event
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val color = Color(e.color)
    val img = imageFile(e.style.imageName)
    val shape = RoundedCornerShape(16.dp)
    val filled = e.style.design != 1
    val fg = if (filled) color.onColor() else MaterialTheme.colorScheme.onSurface
    Box(modifier) {
        Box(
            Modifier.fillMaxWidth().clip(shape)
                .then(
                    when (e.style.design) {
                        1 -> Modifier.border(2.dp, color, shape).background(MaterialTheme.colorScheme.surface)
                        2 -> Modifier.background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f))))
                        else -> Modifier.background(color)
                    },
                )
                .combinedClickable(onClick = { nav.go(Routes.eventEdit(e.id)) }, onLongClick = { menu = true }),
        ) {
            if (e.style.design == 3 && img != null) {
                AsyncImage(model = img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))))
            }
            if (e.style.patternId >= 0) PatternLayer(e.style.patternId, Modifier.matchParentSize())
            Row(Modifier.padding(if (compact) 8.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e.emoji, fontSize = if (compact) 20.sp else 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.title, fontWeight = FontWeight.SemiBold, color = if (e.style.design == 3 && img != null) Color.White else fg,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = if (e.allDay) "Ganztägig" else "${DateLogic.formatTime(o.start)} – ${DateLogic.formatTime(o.end)}" +
                        if (o.end.toLocalDate() != o.start.toLocalDate()) " (${DateLogic.formatDayShort(o.end.toLocalDate())})" else ""
                    Text(sub, fontSize = 12.sp, color = (if (e.style.design == 3 && img != null) Color.White else fg).copy(alpha = 0.85f))
                    if (!compact && e.description.isNotBlank()) {
                        Text(e.description, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = fg.copy(alpha = 0.85f))
                    }
                    if (!compact) TagRow(e.tags, tags)
                }
                if (e.repeat.isRepeating) Icon(Icons.Default.Repeat, null, tint = fg.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                if (e.reminders.isNotEmpty()) Icon(Icons.Default.Notifications, null, tint = fg.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                if (e.favorite) Icon(Icons.Filled.Star, null, tint = fg, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Bearbeiten") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; nav.go(Routes.eventEdit(e.id)) })
            DropdownMenuItem(text = { Text("Duplizieren") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { menu = false; vm.duplicateEvent(e) })
            DropdownMenuItem(text = { Text(if (e.favorite) "Aus Favoriten entfernen" else "Zu Favoriten") },
                leadingIcon = { Icon(if (e.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
                onClick = { menu = false; vm.saveEvent(e.copy(favorite = !e.favorite)) })
            DropdownMenuItem(text = { Text("Löschen") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; confirm = true })
        }
    }
    if (confirm) ConfirmDeleteDialog("„${e.title}“ löschen?", { confirm = false }) { confirm = false; vm.deleteEvent(e.id) }
}

@Composable
private fun DecorDialog(key: String, existing: DecorEntity?, vm: AppViewModel, onDismiss: () -> Unit) {
    var d by remember { mutableStateOf(existing ?: DecorEntity(key = key, alpha = 0.45f)) }
    val pick = rememberImagePicker { uri -> vm.importImage(uri) { n -> if (n != null) d = d.copy(imageName = n) } }
    val isMonth = key.startsWith("m:")
    val label = if (isMonth) DateLogic.formatMonth(YearMonth.parse(key.drop(2)).atDay(1)) else DateLogic.formatLongDay(LocalDate.parse(key.drop(2)))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isMonth) "Monat gestalten" else "Tag gestalten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, fontWeight = FontWeight.Bold)
                Text("Hintergrundfarbe")
                ColorPalette(d.color, { d = d.copy(color = it) })
                Text("Hintergrundbild")
                ImagePickRow(d.imageName, onPick = pick, onRemove = { d = d.copy(imageName = null) })
                LabeledSlider("Deckkraft", d.alpha, 0.1f..1f, "${(d.alpha * 100).toInt()} %") { d = d.copy(alpha = it) }
                if (!isMonth) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Symbol für den Tag: ")
                    EmojiButton(d.emoji ?: "", 40.dp) { d = d.copy(emoji = it) }
                    if (d.emoji != null) TextButton(onClick = { d = d.copy(emoji = null) }) { Text("Entfernen") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.saveDecor(d); onDismiss() }) { Text("Speichern") } },
        dismissButton = {
            Row {
                if (existing != null) TextButton(onClick = { vm.deleteDecor(key); onDismiss() }) { Text("Zurücksetzen") }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Termin-Editor
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorScreen(vm: AppViewModel, nav: Nav, id: Long, date: String) {
    val all by vm.events.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val initial = remember(id) {
        all.firstOrNull { it.id == id } ?: run {
            val startDt = DateLogic.parse(date)?.let { if (date.length > 10) it else it.withHour((LocalTime.now().hour + 1).coerceAtMost(23)) }
                ?: LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1)
            EventEntity(
                title = "", start = DateLogic.format(startDt), end = DateLogic.format(startDt.plusHours(1)),
                reminders = settings.defaultEventReminders, color = Defaults.vivid[3],
            )
        }
    }
    var e by remember(id) { mutableStateOf(initial) }
    var confirm by remember { mutableStateOf(false) }
    val start = DateLogic.parse(e.start) ?: LocalDateTime.now()
    val end = DateLogic.parse(e.end) ?: start.plusHours(1)
    val pick = rememberImagePicker { uri -> vm.importImage(uri) { n -> if (n != null) e = e.copy(style = e.style.copy(imageName = n, design = 3)) } }

    fun setStart(s: LocalDateTime) {
        val dur = java.time.Duration.between(start, end).let { if (it.isNegative) java.time.Duration.ofHours(1) else it }
        e = e.copy(start = DateLogic.format(s), end = DateLogic.format(s.plus(dur)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Neuer Termin" else "Termin bearbeiten") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = {
                    if (id != 0L) {
                        IconButton(onClick = { vm.duplicateEvent(e); nav.back() }) { Icon(Icons.Default.ContentCopy, "Duplizieren") }
                        IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, "Löschen") }
                    }
                    TextButton(onClick = {
                        val fixedEnd = if (end.isBefore(start)) start.plusHours(1) else end
                        vm.saveEvent(e.copy(title = e.title.ifBlank { "Termin" }, end = DateLogic.format(fixedEnd)))
                        nav.back()
                    }) { Icon(Icons.Default.Check, null); Text(" Speichern") }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiButton(e.emoji) { e = e.copy(emoji = it) }
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(e.title, { e = e.copy(title = it) }, label = { Text("Titel") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(e.description, { e = e.copy(description = it) }, label = { Text("Beschreibung") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(e.category, { e = e.copy(category = it) }, label = { Text("Kategorie (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                SectionTitle("Zeit")
                SettingsCard {
                    SwitchRow("Ganztägig", e.allDay) { v ->
                        e = if (v) e.copy(allDay = true, start = DateLogic.format(start.toLocalDate().atStartOfDay()),
                            end = DateLogic.format(maxOf(end.toLocalDate(), start.toLocalDate()).plusDays(1).atStartOfDay()))
                        else e.copy(allDay = false, start = DateLogic.format(start.toLocalDate().atTime(9, 0)), end = DateLogic.format(start.toLocalDate().atTime(10, 0)))
                    }
                    DateTimeRow("Beginn", start.toLocalDate(), if (e.allDay) null else start.toLocalTime(),
                        onDate = { d -> setStart(d.atTime(start.toLocalTime())) }, onTime = { t -> setStart(start.toLocalDate().atTime(t)) })
                    val shownEnd = if (e.allDay) end.toLocalDate().minusDays(1) else end.toLocalDate()
                    DateTimeRow("Ende", maxOf(shownEnd, start.toLocalDate()), if (e.allDay) null else end.toLocalTime(),
                        onDate = { d -> e = e.copy(end = DateLogic.format(if (e.allDay) d.plusDays(1).atStartOfDay() else d.atTime(end.toLocalTime()))) },
                        onTime = { t -> e = e.copy(end = DateLogic.format(end.toLocalDate().atTime(t))) })
                    if (end.isBefore(start)) Text("⚠️ Ende liegt vor dem Beginn", color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                SectionTitle("Wiederholung")
                SettingsCard { RepeatEditor(e.repeat, { e = e.copy(repeat = it) }) }
            }
            item {
                SectionTitle("Erinnerungen")
                SettingsCard {
                    ReminderEditor(e.reminders, listOf(0L, 5L, 15L, 30L, 60L, 1440L, 10080L)) { e = e.copy(reminders = it) }
                    Text("Mehrere Erinnerungen pro Termin sind möglich.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SectionTitle("Gestaltung")
                SettingsCard {
                    Text("Farbe")
                    ColorPalette(e.color, { e = e.copy(color = it) })
                    Text("Design", modifier = Modifier.padding(top = 6.dp))
                    ChoiceChips(listOf(0 to "Gefüllt", 1 to "Umriss", 2 to "Verlauf", 3 to "Bild"), e.style.design) { v -> e = e.copy(style = e.style.copy(design = v)) }
                    if (e.style.design == 3) ImagePickRow(e.style.imageName, onPick = pick, onRemove = { e = e.copy(style = e.style.copy(imageName = null, design = 0)) })
                    Text("Muster", modifier = Modifier.padding(top = 6.dp))
                    ChoiceChips(listOf(-1 to "Keins") + com.planora.app.widget.PatternPainter.names.mapIndexed { i, n -> i to n }, e.style.patternId) { v ->
                        e = e.copy(style = e.style.copy(patternId = v))
                    }
                    Spacer(Modifier.height(8.dp))
                    EventCard(EventLogic.Occurrence(e.copy(title = e.title.ifBlank { "Vorschau" }), start, end), tags, nav, vm)
                }
            }
            item {
                SectionTitle("Tags")
                SettingsCard { TagSelector(tags, e.tags, { e = e.copy(tags = it) }) { t, done -> vm.saveTag(t, done) } }
            }
            item {
                SettingsCard { SwitchRow("⭐ Favorit", e.favorite) { e = e.copy(favorite = it) } }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
    if (confirm) ConfirmDeleteDialog("Termin löschen?", { confirm = false }) { confirm = false; vm.deleteEvent(id); nav.back() }
}
