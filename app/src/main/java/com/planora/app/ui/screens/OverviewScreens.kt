package com.planora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.data.ChecklistEntity
import com.planora.app.data.CountdownEntity
import com.planora.app.data.EventEntity
import com.planora.app.data.NoteEntity
import com.planora.app.data.TagEntity
import com.planora.app.data.TaskEntity
import com.planora.app.domain.CountdownLogic
import com.planora.app.domain.DateLogic
import com.planora.app.domain.EventLogic
import com.planora.app.domain.TaskLogic
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.Routes
import com.planora.app.ui.TopBarActions
import com.planora.app.ui.components.EmptyState
import com.planora.app.ui.components.SectionTitle
import com.planora.app.ui.components.SwitchRow
import com.planora.app.ui.components.TagChip
import com.planora.app.ui.components.TagEditDialog
import com.planora.app.ui.components.rememberNow
import com.planora.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalDateTime

// ---------------------------------------------------------------------------------------------
// Bereichsübergreifende Ergebnisliste (Favoriten, Tag-Filter, Suche)
// ---------------------------------------------------------------------------------------------

fun nextOccurrenceOf(e: EventEntity, now: LocalDateTime): EventLogic.Occurrence {
    val s = DateLogic.parse(e.start) ?: now
    val d = EventLogic.duration(e)
    val next = DateLogic.nextOccurrence(s, e.repeat, now.minus(d)) ?: DateLogic.previousOccurrence(s, e.repeat, now) ?: s
    return EventLogic.Occurrence(e, next, next.plus(d))
}

fun LazyListScope.mixedResults(
    events: List<EventEntity>, countdowns: List<CountdownEntity>, checklists: List<ChecklistEntity>,
    tasks: List<TaskEntity>, notes: List<NoteEntity>, allTasks: List<TaskEntity>, allLists: List<ChecklistEntity>,
    tags: List<TagEntity>, vm: AppViewModel, nav: Nav, now: LocalDateTime,
) {
    if (countdowns.isNotEmpty()) {
        item(key = "h_cd") { SectionTitle("⏳ Countdowns (${countdowns.size})") }
        items(CountdownLogic.sort(countdowns, 0, now, tags), key = { "cd_${it.id}" }) { c ->
            CountdownCard(c, now, tags, onClick = { nav.go(Routes.countdown(c.id)) })
        }
    }
    if (events.isNotEmpty()) {
        item(key = "h_ev") { SectionTitle("📅 Termine (${events.size})") }
        items(events.map { nextOccurrenceOf(it, now) }.sortedBy { it.start }, key = { "ev_${it.event.id}" }) { o ->
            Column {
                Text(DateLogic.formatDay(o.start.toLocalDate()), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                EventCard(o, tags, nav, vm)
            }
        }
    }
    if (checklists.isNotEmpty()) {
        item(key = "h_cl") { SectionTitle("✅ Checklisten (${checklists.size})") }
        items(checklists, key = { "cl_${it.id}" }) { c ->
            ChecklistCard(c, allTasks.filter { it.checklistId == c.id }, tags, onClick = { nav.go(Routes.checklist(c.id)) })
        }
    }
    if (tasks.isNotEmpty()) {
        item(key = "h_tk") { SectionTitle("☑️ Aufgaben (${tasks.size})") }
        items(tasks, key = { "tk_${it.id}" }) { t -> MiniTaskRow(t, allLists, vm, nav) }
    }
    if (notes.isNotEmpty()) {
        item(key = "h_nt") { SectionTitle("📝 Notizen (${notes.size})") }
        items(notes, key = { "nt_${it.id}" }) { n -> NoteCard(n, tags, maxLines = 4, onClick = { nav.go(Routes.note(n.id)) }) }
    }
}

@Composable
fun MiniTaskRow(t: TaskEntity, lists: List<ChecklistEntity>, vm: AppViewModel, nav: Nav) {
    val list = lists.firstOrNull { it.id == t.checklistId }
    val due = DateLogic.parse(t.due)
    val overdue = TaskLogic.isOverdue(t, LocalDateTime.now())
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { nav.go(Routes.checklist(t.checklistId)) }.padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = t.done, onCheckedChange = { vm.toggleTask(t.id) })
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(t.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(list?.let { "${it.emoji} ${it.title}" }, due?.let { (if (overdue) "⚠️ " else "") + DateLogic.formatDateTime(it) }).joinToString(" · "),
                fontSize = 11.sp, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dashboard
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: AppViewModel, nav: Nav, outer: PaddingValues) {
    val events by vm.events.collectAsStateWithLifecycle()
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val now = rememberNow(1000)
    var customize by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val upcoming = remember(events, today) {
        EventLogic.occurrences(events, today, today.plusDays(14)).filter { !it.end.isBefore(LocalDateTime.now()) }.take(5)
    }
    val minute = now.withSecond(0).withNano(0)
    val nextCountdowns = remember(countdowns, minute) { CountdownLogic.sort(countdowns.filter { !CountdownLogic.state(it, minute).expired }, 0, minute, tags).take(4) }
    val dueTasks = tasks.filter { !it.done && (DateLogic.parse(it.due)?.let { d -> !d.toLocalDate().isAfter(today.plusDays(2)) } ?: false) }
        .sortedBy { it.due }.take(6)
    val recentNotes = notes.sortedByDescending { it.updatedAt }.take(6)
    val favCount = events.count { it.favorite } + countdowns.count { it.favorite } + lists.count { it.favorite } + notes.count { it.favorite }
    val greeting = when (now.hour) { in 5..10 -> "Guten Morgen"; in 11..17 -> "Guten Tag"; in 18..22 -> "Guten Abend"; else -> "Gute Nacht" }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text("Übersicht", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { customize = true }) { Icon(Icons.Default.Tune, "Anpassen") }
                    TopBarActions(nav)
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen(), end = Spacing.screen(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap()),
        ) {
            if (s.dashShowDate) item {
                val primary = MaterialTheme.colorScheme.primary
                val secondary = MaterialTheme.colorScheme.secondary
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(primary, secondary))).padding(20.dp),
                ) {
                    Text("$greeting 👋", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(DateLogic.formatLongDay(today), color = Color.White.copy(alpha = 0.9f))
                    Text("%02d:%02d".format(now.hour, now.minute), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${upcoming.count { it.start.toLocalDate() == today }} Termine heute · ${dueTasks.size} Aufgaben fällig",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp,
                    )
                }
            }
            if (s.dashShowCountdowns) {
                item { DashHeader("⏳ Aktive Countdowns") { nav.tab(Routes.COUNTDOWNS) } }
                if (nextCountdowns.isEmpty()) item { Text("Keine aktiven Countdowns", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(nextCountdowns, key = { "c${it.id}" }) { c -> CountdownCard(c, now, tags, onClick = { nav.go(Routes.countdown(c.id)) }) }
            }
            if (s.dashShowEvents) {
                item { DashHeader("📅 Nächste Termine") { nav.tab(Routes.CALENDAR) } }
                if (upcoming.isEmpty()) item { Text("Keine Termine in den nächsten 14 Tagen", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(upcoming, key = { "e${it.event.id}_${it.start}" }) { o ->
                    Column {
                        Text(if (o.start.toLocalDate() == today) "Heute" else DateLogic.formatDay(o.start.toLocalDate()),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        EventCard(o, tags, nav, vm, compact = true)
                    }
                }
            }
            if (s.dashShowTasks) {
                item { DashHeader("☑️ Anstehende Aufgaben") { nav.tab(Routes.CHECKLISTS) } }
                if (dueTasks.isEmpty()) item { Text("Nichts fällig – genieß den Tag! ☀️", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(dueTasks, key = { "t${it.id}" }) { t -> MiniTaskRow(t, lists, vm, nav) }
            }
            if (s.dashShowNotes) {
                item { DashHeader("📝 Letzte Notizen") { nav.tab(Routes.NOTES) } }
                item {
                    if (recentNotes.isEmpty()) Text("Noch keine Notizen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentNotes, key = { it.id }) { n ->
                            NoteCard(n, tags, Modifier.width(200.dp), maxLines = 5, onClick = { nav.go(Routes.note(n.id)) })
                        }
                    }
                }
            }
            if (s.dashShowFavorites) item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { nav.tab(Routes.FAVORITES) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⭐", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Favoriten", fontWeight = FontWeight.Bold)
                        Text("$favCount markierte Einträge", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("›", fontSize = 26.sp)
                }
            }
        }
    }
    if (customize) {
        AlertDialog(
            onDismissRequest = { customize = false },
            title = { Text("Übersicht anpassen") },
            text = {
                Column {
                    SwitchRow("Datum & Begrüßung", s.dashShowDate) { v -> vm.updateSettings { it.copy(dashShowDate = v) } }
                    SwitchRow("Aktive Countdowns", s.dashShowCountdowns) { v -> vm.updateSettings { it.copy(dashShowCountdowns = v) } }
                    SwitchRow("Nächste Termine", s.dashShowEvents) { v -> vm.updateSettings { it.copy(dashShowEvents = v) } }
                    SwitchRow("Anstehende Aufgaben", s.dashShowTasks) { v -> vm.updateSettings { it.copy(dashShowTasks = v) } }
                    SwitchRow("Letzte Notizen", s.dashShowNotes) { v -> vm.updateSettings { it.copy(dashShowNotes = v) } }
                    SwitchRow("Favoriten", s.dashShowFavorites) { v -> vm.updateSettings { it.copy(dashShowFavorites = v) } }
                }
            },
            confirmButton = { TextButton(onClick = { customize = false }) { Text("Fertig") } },
        )
    }
}

@Composable
private fun DashHeader(title: String, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onMore) { Text("Alle") }
    }
}

// ---------------------------------------------------------------------------------------------
// Favoriten-Tab
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: AppViewModel, nav: Nav, outer: PaddingValues) {
    val events by vm.events.collectAsStateWithLifecycle()
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val now = rememberNow(1000)
    var filter by remember { mutableStateOf(0) }
    val fe = if (filter == 0 || filter == 2) events.filter { it.favorite } else emptyList()
    val fc = if (filter == 0 || filter == 1) countdowns.filter { it.favorite } else emptyList()
    val fl = if (filter == 0 || filter == 3) lists.filter { it.favorite } else emptyList()
    val fn = if (filter == 0 || filter == 4) notes.filter { it.favorite } else emptyList()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text("Favoriten ⭐", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = { TopBarActions(nav) },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen(), end = Spacing.screen(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap()),
        ) {
            item {
                com.planora.app.ui.components.ChoiceChips(
                    listOf(0 to "Alle", 1 to "⏳ Countdowns", 2 to "📅 Termine", 3 to "✅ Checklisten", 4 to "📝 Notizen"), filter,
                ) { filter = it }
            }
            if (fe.isEmpty() && fc.isEmpty() && fl.isEmpty() && fn.isEmpty()) item {
                EmptyState("⭐", "Noch keine Favoriten", "Halte einen Eintrag gedrückt oder tippe auf den Stern, um ihn hier zu sammeln.")
            }
            mixedResults(fe, fc, fl, emptyList(), fn, tasks, lists, tags, vm, nav, now)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tags: Verwaltung und globale Filteransicht
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(vm: AppViewModel, nav: Nav) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<TagEntity?>(null) }
    var create by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<TagEntity?>(null) }
    fun count(id: Long) = events.count { id in it.tags } + countdowns.count { id in it.tags } + lists.count { id in it.tags } +
        tasks.count { id in it.tags } + notes.count { id in it.tags }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags 🏷️") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { create = true }) { Icon(Icons.Default.Add, "Neuer Tag") } },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(query, { query = it }, placeholder = { Text("Tags durchsuchen …") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50))
                Text("Tippe auf einen Tag, um alle Termine, Countdowns, Checklisten und Notizen mit diesem Tag zu sehen.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            val shown = tags.filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) }
            if (shown.isEmpty()) item { EmptyState("🏷️", "Keine Tags", "Erstelle Tags, um Inhalte bereichsübergreifend zu ordnen.") }
            items(shown, key = { it.id }) { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(t.color).copy(alpha = 0.16f))
                        .clickable { nav.go(Routes.tag(t.id)) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(t.color)).padding(8.dp)) { Text(t.emoji, fontSize = 22.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold)
                        Text("${count(t.id)} Einträge" + if (t.description.isNotBlank()) " · ${t.description}" else "",
                            style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { edit = t }) { Icon(Icons.Default.Edit, "Bearbeiten") }
                    IconButton(onClick = { confirm = t }) { Icon(Icons.Default.Delete, "Löschen") }
                }
            }
        }
    }
    if (create) TagEditDialog(null, { create = false }) { vm.saveTag(it); create = false }
    edit?.let { t -> TagEditDialog(t, { edit = null }) { vm.saveTag(it); edit = null } }
    confirm?.let { t ->
        ConfirmDeleteDialog("Tag „${t.name}“ löschen?", { confirm = null }) { vm.deleteTag(t); confirm = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(vm: AppViewModel, nav: Nav, id: Long) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val now = rememberNow(1000)
    val tag = tags.firstOrNull { it.id == id }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag?.let { "${it.emoji} ${it.name}" } ?: "Tag") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tag?.let { Color(it.color).copy(alpha = 0.25f) } ?: MaterialTheme.colorScheme.surface),
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
    ) { pad ->
        val e = events.filter { id in it.tags }
        val c = countdowns.filter { id in it.tags }
        val l = lists.filter { id in it.tags }
        val t = tasks.filter { id in it.tags }
        val n = notes.filter { id in it.tags }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tag != null && tag.description.isNotBlank()) item { Text(tag.description, style = MaterialTheme.typography.bodyMedium) }
            if (e.isEmpty() && c.isEmpty() && l.isEmpty() && t.isEmpty() && n.isEmpty()) item {
                EmptyState("🔍", "Noch nichts mit diesem Tag", "Weise den Tag in Terminen, Countdowns, Checklisten oder Notizen zu.")
            }
            mixedResults(e, c, l, t, n, tasks, lists, tags, vm, nav, now)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Globale Suche
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: AppViewModel, nav: Nav) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val now = rememberNow(1000)
    var q by remember { mutableStateOf("") }
    val query = q.trim()
    val matchingTags = if (query.isEmpty()) emptyList() else tags.filter { it.name.contains(query, true) || it.description.contains(query, true) }
    val tagIds = matchingTags.map { it.id }.toSet()
    fun hit(text: String) = query.isNotEmpty() && text.contains(query, true)
    fun tagHit(ids: List<Long>) = ids.any { it in tagIds }
    val e = events.filter { hit(it.title) || hit(it.description) || hit(it.category) || tagHit(it.tags) }
    val c = countdowns.filter { hit(it.title) || hit(it.description) || hit(it.category) || tagHit(it.tags) }
    val l = lists.filter { hit(it.title) || tagHit(it.tags) }
    val t = tasks.filter { hit(it.text) || hit(it.note) || tagHit(it.tags) }
    val n = notes.filter { hit(it.title) || hit(Markup.toPlain(it.content)) || hit(it.category) || tagHit(it.tags) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(q, { q = it }, placeholder = { Text("Alles durchsuchen …") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp), shape = RoundedCornerShape(50))
                },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (query.isEmpty()) {
                item { EmptyState("🔎", "Globale Suche", "Durchsucht Termine, Countdowns, Checklisten, Aufgaben, Notizen und Tags.") }
            } else {
                if (matchingTags.isNotEmpty()) {
                    item {
                        SectionTitle("🏷️ Tags")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            matchingTags.take(6).forEach { tag -> TagChip(tag) { nav.go(Routes.tag(tag.id)) } }
                        }
                    }
                }
                if (e.isEmpty() && c.isEmpty() && l.isEmpty() && t.isEmpty() && n.isEmpty() && matchingTags.isEmpty()) {
                    item { EmptyState("🤷", "Keine Treffer", "Versuche einen anderen Suchbegriff.") }
                }
                mixedResults(e, c, l, t, n, tasks, lists, tags, vm, nav, now)
            }
        }
    }
}
