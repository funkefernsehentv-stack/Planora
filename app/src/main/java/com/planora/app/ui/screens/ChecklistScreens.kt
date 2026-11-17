package com.planora.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.data.ChecklistEntity
import com.planora.app.data.Defaults
import com.planora.app.data.TagEntity
import com.planora.app.data.TaskEntity
import com.planora.app.domain.DateLogic
import com.planora.app.domain.TaskLogic
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.Routes
import com.planora.app.ui.TopBarActions
import com.planora.app.ui.components.ChoiceChips
import com.planora.app.ui.components.ColorPalette
import com.planora.app.ui.components.DateTimeRow
import com.planora.app.ui.components.EmojiButton
import com.planora.app.ui.components.EmptyState
import com.planora.app.ui.components.RepeatEditor
import com.planora.app.ui.components.SwitchRow
import com.planora.app.ui.components.TagRow
import com.planora.app.ui.components.TagSelector
import com.planora.app.ui.theme.Spacing
import com.planora.app.ui.theme.onColor
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val priorityColors = listOf(Color.Transparent, Color(0xFF42A5F5), Color(0xFFFFA726), Color(0xFFE53935))
private val priorityLabels = listOf("Keine", "Niedrig", "Mittel", "Hoch")

fun checklistBackground(c: ChecklistEntity): Modifier = when (c.design) {
    1 -> Modifier.border(2.dp, Color(c.color), RoundedCornerShape(20.dp))
    2 -> Modifier.background(Brush.linearGradient(listOf(Color(c.color), Color(c.color).copy(alpha = 0.6f))))
    3 -> Modifier.background(Color(0xFFFFF8E1))
    else -> Modifier.background(Color(c.color))
}

fun checklistText(c: ChecklistEntity, fallback: Color): Color = when (c.design) {
    1 -> fallback
    3 -> Color(0xFF3E2723)
    else -> Color(c.textColor)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistCard(c: ChecklistEntity, tasks: List<TaskEntity>, tags: List<TagEntity>, modifier: Modifier = Modifier,
                  onClick: () -> Unit, onLongClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val done = tasks.count { it.done }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val animated by animateFloatAsState(progress, label = "progress")
    val fg = checklistText(c, MaterialTheme.colorScheme.onSurface)
    val now = LocalDateTime.now()
    val overdue = tasks.count { TaskLogic.isOverdue(it, now) }
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).then(checklistBackground(c))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.title, color = fg, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${tasks.size - done} offen · $done erledigt" + if (overdue > 0) " · ⚠️ $overdue überfällig" else "",
                        color = fg.copy(alpha = 0.85f), fontSize = 12.sp,
                    )
                }
                if (c.pinned) Icon(Icons.Filled.PushPin, null, tint = fg, modifier = Modifier.size(16.dp))
                if (c.favorite) Icon(Icons.Filled.Star, null, tint = fg, modifier = Modifier.size(16.dp))
                Text("${(progress * 100).toInt()} %", color = fg, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                trailing?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (c.design == 1 || c.design == 3) Color(c.color) else fg,
                trackColor = (if (c.design == 1 || c.design == 3) Color(c.color) else fg).copy(alpha = 0.25f),
                drawStopIndicator = {},
            )
            if (c.tags.isNotEmpty()) { Spacer(Modifier.height(6.dp)); TagRow(c.tags, tags) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistsScreen(vm: AppViewModel, nav: Nav, outer: PaddingValues) {
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<ChecklistEntity?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    var confirm by remember { mutableStateOf<ChecklistEntity?>(null) }
    val sorted = remember(lists) { lists.sortedWith(compareByDescending<ChecklistEntity> { it.pinned }.thenBy { it.sortIndex }) }
    var items by remember(sorted) { mutableStateOf(sorted) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val f = items.indexOfFirst { it.id == from.key }; val t = items.indexOfFirst { it.id == to.key }
        if (f >= 0 && t >= 0) items = items.toMutableList().apply { add(t, removeAt(f)) }
    }
    val byList = tasks.groupBy { it.checklistId }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text("Checklisten", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = { TopBarActions(nav) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { edit = ChecklistEntity(title = "", color = Defaults.vivid[lists.size % 8]) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Checkliste") })
        },
    ) { pad ->
        LazyColumn(
            state = listState, modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen(), end = Spacing.screen(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.gap()),
        ) {
            if (items.isEmpty()) item { EmptyState("✅", "Noch keine Checklisten", "Erstelle z. B. eine Einkaufsliste oder deine Tagesroutine.") }
            items(items, key = { it.id }) { c ->
                ReorderableItem(reorder, key = c.id) { _ ->
                    Box {
                        ChecklistCard(
                            c, byList[c.id].orEmpty(), tags,
                            onClick = { nav.go(Routes.checklist(c.id)) }, onLongClick = { menuFor = c.id },
                            trailing = {
                                IconButton(onClick = {}, modifier = Modifier.draggableHandle(onDragStopped = { vm.reorderChecklists(items.map { it.id }) })) {
                                    Icon(Icons.Default.DragHandle, "Verschieben", tint = checklistText(c, MaterialTheme.colorScheme.onSurface))
                                }
                            },
                        )
                        DropdownMenu(expanded = menuFor == c.id, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(text = { Text("Bearbeiten") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menuFor = null; edit = c })
                            DropdownMenuItem(text = { Text("Duplizieren") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { menuFor = null; vm.duplicateChecklist(c) })
                            DropdownMenuItem(text = { Text(if (c.favorite) "Aus Favoriten entfernen" else "Zu Favoriten") },
                                leadingIcon = { Icon(if (c.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
                                onClick = { menuFor = null; vm.saveChecklist(c.copy(favorite = !c.favorite)) })
                            DropdownMenuItem(text = { Text(if (c.pinned) "Lösen" else "Oben anheften") }, leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                                onClick = { menuFor = null; vm.saveChecklist(c.copy(pinned = !c.pinned)) })
                            DropdownMenuItem(text = { Text("Löschen") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuFor = null; confirm = c })
                        }
                    }
                }
            }
        }
    }
    edit?.let { c ->
        ChecklistEditDialog(c, tags, vm, onDismiss = { edit = null }) { saved ->
            vm.saveChecklist(saved) { newId -> if (c.id == 0L) nav.go(Routes.checklist(newId)) }
            edit = null
        }
    }
    confirm?.let { c -> ConfirmDeleteDialog("„${c.title}“ mit allen Aufgaben löschen?", { confirm = null }) { vm.deleteChecklist(c.id); confirm = null } }
}

@Composable
fun ChecklistEditDialog(c: ChecklistEntity, tags: List<TagEntity>, vm: AppViewModel, onDismiss: () -> Unit, onSave: (ChecklistEntity) -> Unit) {
    var s by remember { mutableStateOf(c) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (c.id == 0L) "Neue Checkliste" else "Checkliste bearbeiten") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiButton(s.emoji) { s = s.copy(emoji = it) }
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(s.title, { s = s.copy(title = it) }, label = { Text("Name") }, singleLine = true)
                }
                Text("Farbe"); ColorPalette(s.color, { s = s.copy(color = it) })
                Text("Schriftfarbe"); ColorPalette(s.textColor, { s = s.copy(textColor = it) }, extra = listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt()))
                Text("Design"); ChoiceChips(listOf(0 to "Gefüllt", 1 to "Umriss", 2 to "Verlauf", 3 to "Papier"), s.design) { s = s.copy(design = it) }
                Box(Modifier.padding(vertical = 4.dp)) { ChecklistCard(s.copy(title = s.title.ifBlank { "Vorschau" }), emptyList(), tags, onClick = {}) }
                SwitchRow("Erledigte ausblenden", s.hideDone) { s = s.copy(hideDone = it) }
                SwitchRow("⭐ Favorit", s.favorite) { s = s.copy(favorite = it) }
                Text("Tags"); TagSelector(tags, s.tags, { s = s.copy(tags = it) }) { t, done -> vm.saveTag(t, done) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(s.copy(title = s.title.ifBlank { "Checkliste" })) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private val taskSortLabels = listOf("Manuell", "Nach Priorität", "Nach Fälligkeit", "Alphabetisch")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(vm: AppViewModel, nav: Nav, id: Long) {
    val lists by vm.checklists.collectAsStateWithLifecycle()
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val list = lists.firstOrNull { it.id == id }
    var newText by remember { mutableStateOf("") }
    var editTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editList by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Checkliste wird geladen …") }
        return
    }
    val tasks = allTasks.filter { it.checklistId == id }
    val sorted = remember(tasks, list.sortMode) { TaskLogic.sort(tasks, list.sortMode) }
    val visible = if (list.hideDone) sorted.filter { !it.done } else sorted
    var items by remember(visible) { mutableStateOf(visible) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val f = items.indexOfFirst { it.id == from.key }; val t = items.indexOfFirst { it.id == to.key }
        if (f >= 0 && t >= 0) items = items.toMutableList().apply { add(t, removeAt(f)) }
    }
    val headerColor = if (list.design == 1 || list.design == 3) MaterialTheme.colorScheme.surface else Color(list.color)
    val headerFg = if (list.design == 1 || list.design == 3) MaterialTheme.colorScheme.onSurface else Color(list.textColor)

    fun add() {
        val t = newText.trim()
        if (t.isNotEmpty()) vm.saveTask(TaskEntity(checklistId = id, text = t))
        newText = ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("${list.emoji} ${list.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor, titleContentColor = headerFg,
                    navigationIconContentColor = headerFg, actionIconContentColor = headerFg),
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = {
                    IconButton(onClick = { vm.saveChecklist(list.copy(favorite = !list.favorite)) }) {
                        Icon(if (list.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favorit")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            taskSortLabels.forEachIndexed { i, l ->
                                DropdownMenuItem(text = { Text((if (list.sortMode == i) "✓ " else "") + l) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                                    onClick = { menu = false; vm.saveChecklist(list.copy(sortMode = i)) })
                            }
                            DropdownMenuItem(text = { Text(if (list.hideDone) "Erledigte anzeigen" else "Erledigte ausblenden") },
                                onClick = { menu = false; vm.saveChecklist(list.copy(hideDone = !list.hideDone)) })
                            DropdownMenuItem(text = { Text("Erledigte entfernen") }, onClick = { menu = false; vm.clearDone(id) })
                            DropdownMenuItem(text = { Text("Alle zurücksetzen") }, onClick = { menu = false; vm.resetChecklist(id) })
                            DropdownMenuItem(text = { Text("Liste bearbeiten") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; editList = true })
                            DropdownMenuItem(text = { Text("Liste löschen") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; confirm = true })
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp).padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newText, onValueChange = { newText = it }, placeholder = { Text("Neue Aufgabe …") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { add() }) { Icon(Icons.AutoMirrored.Filled.Send, "Hinzufügen") }
            }
        },
    ) { pad ->
        val done = tasks.count { it.done }
        val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
        val animated by animateFloatAsState(progress, label = "p")
        LazyColumn(state = listState, modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp)) {
                    Row {
                        Text("Fortschritt", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("${(progress * 100).toInt()} %  ·  $done von ${tasks.size}", fontWeight = FontWeight.Bold, color = Color(list.color))
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = Color(list.color), trackColor = Color(list.color).copy(alpha = 0.2f), drawStopIndicator = {})
                    if (progress == 1f && tasks.isNotEmpty()) Text("Alles erledigt! 🎉", Modifier.padding(top = 6.dp))
                }
            }
            if (items.isEmpty()) item { EmptyState("📝", "Keine Aufgaben", "Füge unten deine erste Aufgabe hinzu.") }
            items(items, key = { it.id }) { t ->
                ReorderableItem(reorder, key = t.id, enabled = list.sortMode == 0) { _ ->
                    TaskRow(
                        t, tags, accent = Color(list.color),
                        onToggle = {
                            vm.toggleTask(t.id) { r ->
                                if (r != null && !r.done && r.repeat.isRepeating && !t.done) {
                                    scope.launch {
                                        snackbar.showSnackbar("🔁 Erledigt – nächste Fälligkeit: ${DateLogic.parse(r.due)?.let { DateLogic.formatDateTime(it) } ?: "-"}")
                                    }
                                }
                            }
                        },
                        onClick = { editTask = t },
                        handle = if (list.sortMode == 0) {
                            {
                                Icon(Icons.Default.DragHandle, "Verschieben", modifier = Modifier.draggableHandle(onDragStopped = {
                                    vm.reorderTasks(items.map { it.id })
                                }).padding(4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else null,
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
    editTask?.let { t ->
        TaskEditDialog(t, tags, lists, vm, onDismiss = { editTask = null })
    }
    if (editList) ChecklistEditDialog(list, tags, vm, { editList = false }) { vm.saveChecklist(it); editList = false }
    if (confirm) ConfirmDeleteDialog("„${list.title}“ löschen?", { confirm = false }) { confirm = false; vm.deleteChecklist(id); nav.back() }
}

@Composable
private fun TaskRow(t: TaskEntity, tags: List<TagEntity>, accent: Color, onToggle: () -> Unit, onClick: () -> Unit, handle: (@Composable () -> Unit)?) {
    val now = LocalDateTime.now()
    val due = DateLogic.parse(t.due)
    val overdue = TaskLogic.isOverdue(t, now)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).animateContentSize().padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(48.dp).background(priorityColors[t.priority.coerceIn(0, 3)]))
        Checkbox(checked = t.done, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = accent))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(t.text, textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (due != null) {
                    Text(
                        (if (overdue) "⚠️ " else "📅 ") + DateLogic.formatDateTime(due, due.toLocalTime() != LocalTime.MIDNIGHT),
                        fontSize = 11.sp, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (t.repeat.isRepeating) Icon(Icons.Default.Repeat, null, Modifier.size(14.dp).padding(start = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (t.reminder) Icon(Icons.Default.Notifications, null, Modifier.size(14.dp).padding(start = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (t.note.isNotBlank()) Text(t.note, fontSize = 12.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TagRow(t.tags, tags)
        }
        handle?.invoke()
    }
}

@Composable
private fun TaskEditDialog(t: TaskEntity, tags: List<TagEntity>, lists: List<ChecklistEntity>, vm: AppViewModel, onDismiss: () -> Unit) {
    var s by remember { mutableStateOf(t) }
    var confirm by remember { mutableStateOf(false) }
    val due = DateLogic.parse(s.due)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aufgabe bearbeiten") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(s.text, { s = s.copy(text = it) }, label = { Text("Aufgabe") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(s.note, { s = s.copy(note = it) }, label = { Text("Notiz") }, modifier = Modifier.fillMaxWidth())
                Text("Priorität")
                ChoiceChips(priorityLabels.indices.map { it to priorityLabels[it] }, s.priority) { s = s.copy(priority = it) }
                Text("Fälligkeit")
                if (due == null) {
                    TextButton(onClick = { s = s.copy(due = DateLogic.format(LocalDate.now().atTime(18, 0))) }) { Text("+ Fälligkeitsdatum") }
                } else {
                    DateTimeRow("Fällig", due.toLocalDate(), due.toLocalTime(),
                        onDate = { d -> s = s.copy(due = DateLogic.format(d.atTime(due.toLocalTime()))) },
                        onTime = { tm -> s = s.copy(due = DateLogic.format(due.toLocalDate().atTime(tm))) })
                    SwitchRow("🔔 Erinnerung bei Fälligkeit", s.reminder) { s = s.copy(reminder = it) }
                    TextButton(onClick = { s = s.copy(due = null, reminder = false) }) { Text("Fälligkeit entfernen") }
                }
                Text("Wiederholung")
                RepeatEditor(s.repeat, { r ->
                    s = s.copy(repeat = r, due = s.due ?: if (r.isRepeating) DateLogic.format(LocalDate.now().atTime(9, 0)) else null)
                })
                Text("Tags")
                TagSelector(tags, s.tags, { s = s.copy(tags = it) }) { tag, done -> vm.saveTag(tag, done) }
                if (lists.size > 1) {
                    Text("In andere Liste verschieben")
                    ChoiceChips(lists.map { it.id to "${it.emoji} ${it.title}" }, s.checklistId) { s = s.copy(checklistId = it) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.saveTask(if (s.checklistId != t.checklistId) s.copy(position = Int.MAX_VALUE / 2) else s)
                onDismiss()
            }) { Text("Speichern") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirm = true }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
    if (confirm) ConfirmDeleteDialog("Aufgabe löschen?", { confirm = false }) { vm.deleteTask(t.id); confirm = false; onDismiss() }
}
