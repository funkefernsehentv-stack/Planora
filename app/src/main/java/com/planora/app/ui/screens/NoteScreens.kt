package com.planora.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.planora.app.data.NoteEntity
import com.planora.app.data.TagEntity
import com.planora.app.domain.DateLogic
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.Routes
import com.planora.app.ui.TopBarActions
import com.planora.app.ui.components.ColorPalette
import com.planora.app.ui.components.EmojiPickerDialog
import com.planora.app.ui.components.EmptyState
import com.planora.app.ui.components.ImagePickRow
import com.planora.app.ui.components.SectionTitle
import com.planora.app.ui.components.SwitchRow
import com.planora.app.ui.components.TagRow
import com.planora.app.ui.components.TagSelector
import com.planora.app.ui.components.imageFile
import com.planora.app.ui.components.rememberImagePicker
import com.planora.app.ui.theme.Spacing
import com.planora.app.ui.theme.onColor
import java.time.Instant
import java.time.ZoneId

/** Vorgefertigte Notizdesigns: Name, Hintergrund, Schrift, liniert */
data class NoteDesign(val name: String, val bg: Int, val fg: Int, val lined: Boolean = false)

val noteDesigns = listOf(
    NoteDesign("Sonnengelb", 0xFFFFF3C4.toInt(), 0xFF3E2723.toInt()),
    NoteDesign("Minze", 0xFFD7F5E9.toInt(), 0xFF1B4332.toInt()),
    NoteDesign("Himmel", 0xFFD6ECFF.toInt(), 0xFF0D2B45.toInt()),
    NoteDesign("Rosé", 0xFFFFE1EC.toInt(), 0xFF4A1D2F.toInt()),
    NoteDesign("Lavendel", 0xFFE9E1FF.toInt(), 0xFF2E1A47.toInt()),
    NoteDesign("Nacht", 0xFF1E1B2E.toInt(), 0xFFF2EEF7.toInt()),
    NoteDesign("Liniert", 0xFFFBF8F1.toInt(), 0xFF2B2B2B.toInt(), lined = true),
    NoteDesign("Kraftpapier", 0xFFD9C3A5.toInt(), 0xFF3B2A1A.toInt()),
)

fun noteColors(n: NoteEntity, fallbackBg: Color, fallbackFg: Color): Pair<Color, Color> {
    val d = noteDesigns.getOrNull(n.design)
    val bg = n.color?.let { Color(it) } ?: d?.let { Color(it.bg) } ?: fallbackBg
    val fg = n.textColor?.let { Color(it) } ?: d?.let { Color(it.fg) } ?: if (n.color != null) bg.onColor() else fallbackFg
    return bg to fg
}

fun Modifier.linedPaper(enabled: Boolean, color: Color): Modifier = if (!enabled) this else drawBehind {
    val step = 28.dp.toPx()
    var y = step
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

private fun formatMillis(ms: Long): String =
    DateLogic.formatDateTime(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(n: NoteEntity, tags: List<TagEntity>, modifier: Modifier = Modifier, maxLines: Int = 8, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val (bg, fg) = noteColors(n, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
    val img = imageFile(n.imageName)
    val lined = noteDesigns.getOrNull(n.design)?.lined == true
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        if (img != null) {
            AsyncImage(model = img, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(), alpha = 0.35f)
        }
        Column(Modifier.linedPaper(lined, fg.copy(alpha = 0.12f)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.title.ifBlank { "Ohne Titel" }, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (n.pinned) Icon(Icons.Filled.PushPin, null, tint = fg, modifier = Modifier.size(14.dp))
                if (n.favorite) Icon(Icons.Filled.Star, null, tint = fg, modifier = Modifier.size(14.dp))
            }
            if (n.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(Markup.render(n.content), color = fg.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            if (n.category.isNotBlank()) Text("📁 ${n.category}", color = fg.copy(alpha = 0.7f), fontSize = 11.sp)
            TagRow(n.tags, tags)
            Text(formatMillis(n.updatedAt), color = fg.copy(alpha = 0.55f), fontSize = 10.sp)
        }
    }
}

private val noteSortLabels = listOf("Zuletzt bearbeitet", "Erstellungsdatum", "Kategorie", "Titel")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(vm: AppViewModel, nav: Nav, outer: PaddingValues) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var onlyFav by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    var confirm by remember { mutableStateOf<NoteEntity?>(null) }
    val mode = settings.notesSort
    val filtered = notes.filter { n ->
        (!onlyFav || n.favorite) && (query.isBlank() || n.title.contains(query, true) || Markup.toPlain(n.content).contains(query, true) ||
            n.category.contains(query, true) || tags.any { it.id in n.tags && it.name.contains(query, true) })
    }
    val base: Comparator<NoteEntity> = when (mode) {
        1 -> compareByDescending { it.createdAt }
        2 -> compareBy<NoteEntity> { it.category.ifBlank { "￿" }.lowercase() }.thenByDescending { it.updatedAt }
        3 -> compareBy { it.title.lowercase() }
        else -> compareByDescending { it.updatedAt }
    }
    val sorted = filtered.sortedWith(compareByDescending<NoteEntity> { it.pinned }.then(base))

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text("Notizen", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { vm.updateSettings { it.copy(notesGrid = !it.notesGrid) } }) {
                        Icon(if (settings.notesGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView, "Ansicht")
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sortieren") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            noteSortLabels.forEachIndexed { i, l ->
                                DropdownMenuItem(text = { Text((if (mode == i) "✓ " else "") + l) },
                                    onClick = { sortMenu = false; vm.updateSettings { it.copy(notesSort = i) } })
                            }
                        }
                    }
                    TopBarActions(nav)
                },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { nav.go(Routes.note(0)) }) { Icon(Icons.Default.Add, "Neue Notiz") } },
    ) { pad ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (settings.notesGrid) 2 else 1),
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.screen(), end = Spacing.screen(), bottom = 96.dp),
            verticalItemSpacing = Spacing.gap(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gap()),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    OutlinedTextField(query, { query = it }, placeholder = { Text("Notizen durchsuchen …") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50))
                    Row(Modifier.padding(top = 6.dp)) {
                        FilterChip(selected = onlyFav, onClick = { onlyFav = !onlyFav }, label = { Text("⭐ Nur Favoriten") })
                    }
                }
            }
            if (sorted.isEmpty()) item(span = StaggeredGridItemSpan.FullLine) {
                EmptyState("📝", if (notes.isEmpty()) "Noch keine Notizen" else "Nichts gefunden", "Tippe auf +, um eine Notiz zu schreiben.")
            }
            var lastCat: String? = null
            sorted.forEach { n ->
                if (mode == 2 && !n.pinned && n.category != lastCat) {
                    lastCat = n.category
                    item(key = "cat_${n.category}_${n.id}", span = StaggeredGridItemSpan.FullLine) { SectionTitle(n.category.ifBlank { "Ohne Kategorie" }) }
                }
                item(key = n.id) {
                    Box {
                        NoteCard(n, tags, onClick = { nav.go(Routes.note(n.id)) }, onLongClick = { menuFor = n.id })
                        DropdownMenu(expanded = menuFor == n.id, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(text = { Text(if (n.pinned) "Lösen" else "Anheften") }, leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                                onClick = { menuFor = null; vm.saveNote(n.copy(pinned = !n.pinned)) })
                            DropdownMenuItem(text = { Text(if (n.favorite) "Aus Favoriten entfernen" else "Zu Favoriten") },
                                leadingIcon = { Icon(if (n.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
                                onClick = { menuFor = null; vm.saveNote(n.copy(favorite = !n.favorite)) })
                            DropdownMenuItem(text = { Text("Duplizieren") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                onClick = { menuFor = null; vm.duplicateNote(n) })
                            DropdownMenuItem(text = { Text("Löschen") }, leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { menuFor = null; confirm = n })
                        }
                    }
                }
            }
        }
    }
    confirm?.let { n -> ConfirmDeleteDialog("Notiz löschen?", { confirm = null }) { vm.deleteNote(n.id); confirm = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(vm: AppViewModel, nav: Nav, id: Long) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val original = remember(id) { notes.firstOrNull { it.id == id } ?: NoteEntity(title = "", design = 0) }
    var n by remember(id) { mutableStateOf(original) }
    var savedId by remember(id) { mutableStateOf(id) }
    var content by remember(id) { mutableStateOf(TextFieldValue(original.content)) }
    var preview by remember { mutableStateOf(false) }
    var showDesign by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val (bg, fg) = noteColors(n, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
    val img = imageFile(n.imageName)
    val lined = noteDesigns.getOrNull(n.design)?.lined == true
    val pick = rememberImagePicker { uri -> vm.importImage(uri) { name -> if (name != null) n = n.copy(imageName = name) } }

    fun current() = n.copy(id = savedId, content = content.text)
    fun saveAndClose() {
        val cur = current()
        val changed = cur.title != original.title || cur.content != original.content || cur != original.copy(id = savedId)
        if ((cur.title.isNotBlank() || cur.content.isNotBlank()) && changed) {
            vm.saveNote(cur.copy(updatedAt = System.currentTimeMillis()))
        }
        nav.back()
    }
    BackHandler { saveAndClose() }

    fun wrap(prefix: String, suffix: String) {
        val t = content.text
        val s = content.selection.min
        val e = content.selection.max
        val newText = t.substring(0, s) + prefix + t.substring(s, e) + suffix + t.substring(e)
        content = TextFieldValue(newText, if (s == e) TextRange(s + prefix.length) else TextRange(s + prefix.length, e + prefix.length))
    }
    fun linePrefix(prefix: String) {
        val t = content.text
        val s = content.selection.min
        val lineStart = if (s == 0) 0 else t.lastIndexOf('\n', s - 1) + 1
        val line = t.substring(lineStart)
        content = if (line.startsWith(prefix)) {
            TextFieldValue(t.substring(0, lineStart) + line.removePrefix(prefix), TextRange((s - prefix.length).coerceAtLeast(lineStart)))
        } else {
            TextFieldValue(t.substring(0, lineStart) + prefix + line, TextRange(s + prefix.length))
        }
    }
    fun insert(text: String) {
        val t = content.text
        val s = content.selection.min
        val e = content.selection.max
        content = TextFieldValue(t.substring(0, s) + text + t.substring(e), TextRange(s + text.length))
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, navigationIconContentColor = fg, actionIconContentColor = fg),
                navigationIcon = { IconButton(onClick = { saveAndClose() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = {
                    IconButton(onClick = { preview = !preview }) { Icon(if (preview) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Vorschau") }
                    IconButton(onClick = { n = n.copy(pinned = !n.pinned) }) { Icon(if (n.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, "Anheften") }
                    IconButton(onClick = { n = n.copy(favorite = !n.favorite) }) { Icon(if (n.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favorit") }
                    IconButton(onClick = { showDesign = true }) { Icon(Icons.Default.Palette, "Gestaltung") }
                    if (savedId != 0L) IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, "Löschen") }
                    IconButton(onClick = {
                        val cur = current().copy(updatedAt = System.currentTimeMillis())
                        vm.saveNote(cur) { newId -> savedId = newId }
                        nav.back()
                    }) { Icon(Icons.Default.Check, "Speichern") }
                },
            )
        },
        bottomBar = {
            if (!preview) {
                Row(
                    Modifier.fillMaxWidth().background(fg.copy(alpha = 0.06f)).imePadding().horizontalScroll(rememberScrollState()).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { linePrefix("# ") }) { Icon(Icons.Default.Title, "Überschrift", tint = fg) }
                    TextButton(onClick = { linePrefix("## ") }) { Text("H2", color = fg, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = { wrap("**", "**") }) { Icon(Icons.Default.FormatBold, "Fett", tint = fg) }
                    IconButton(onClick = { wrap("*", "*") }) { Icon(Icons.Default.FormatItalic, "Kursiv", tint = fg) }
                    IconButton(onClick = { linePrefix("- ") }) { Icon(Icons.Default.FormatListBulleted, "Aufzählung", tint = fg) }
                    IconButton(onClick = { linePrefix("1. ") }) { Icon(Icons.Default.FormatListNumbered, "Nummerierung", tint = fg) }
                    IconButton(onClick = { colorDialog = true }) { Icon(Icons.Default.FormatColorText, "Textfarbe", tint = fg) }
                    Box {
                        IconButton(onClick = { sizeMenu = true }) { Icon(Icons.Default.FormatSize, "Schriftgröße", tint = fg) }
                        DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }) {
                            listOf(12, 14, 18, 22, 28, 36).forEach { sz ->
                                DropdownMenuItem(text = { Text("$sz sp", fontSize = sz.coerceAtMost(24).sp) }, onClick = { sizeMenu = false; wrap("[s=$sz]", "[/s]") })
                            }
                        }
                    }
                    TextButton(onClick = { emoji = true }) { Text("😀", fontSize = 20.sp) }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (img != null) AsyncImage(model = img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(), alpha = 0.3f)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                BasicTextField(
                    value = n.title, onValueChange = { n = n.copy(title = it) },
                    textStyle = TextStyle(color = fg, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(fg), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    decorationBox = { inner -> Box { if (n.title.isEmpty()) Text("Titel", color = fg.copy(alpha = 0.4f), fontSize = 26.sp, fontWeight = FontWeight.Bold); inner() } },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (n.category.isNotBlank()) Text("📁 ${n.category}  ", color = fg.copy(alpha = 0.7f), fontSize = 12.sp)
                    TagRow(n.tags, tags)
                }
                Spacer(Modifier.height(8.dp))
                if (preview) {
                    Text(Markup.render(content.text), color = fg, fontSize = 16.sp, lineHeight = 26.sp,
                        modifier = Modifier.fillMaxWidth().linedPaper(lined, fg.copy(alpha = 0.12f)))
                } else {
                    BasicTextField(
                        value = content, onValueChange = { content = it },
                        textStyle = TextStyle(color = fg, fontSize = 16.sp, lineHeight = 28.sp),
                        cursorBrush = SolidColor(fg),
                        visualTransformation = MarkupTransformation(fg.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp).linedPaper(lined, fg.copy(alpha = 0.12f)),
                        decorationBox = { inner ->
                            Box {
                                if (content.text.isEmpty()) Text("Schreib los … (# Überschrift, **fett**, *kursiv*, - Liste)", color = fg.copy(alpha = 0.4f))
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showDesign) {
        AlertDialog(
            onDismissRequest = { showDesign = false },
            title = { Text("Notiz gestalten") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vorgefertigte Designs")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        noteDesigns.forEachIndexed { i, d ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { n = n.copy(design = i, color = null, textColor = null) }) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(d.bg))
                                    .linedPaper(d.lined, Color(d.fg).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Text(if (n.design == i && n.color == null) "✓" else "Aa", color = Color(d.fg))
                                }
                                Text(d.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Text("Eigene Hintergrundfarbe")
                    ColorPalette(n.color, { n = n.copy(color = it) })
                    Text("Schriftfarbe")
                    ColorPalette(n.textColor, { n = n.copy(textColor = it) }, extra = listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()))
                    Text("Hintergrundbild")
                    ImagePickRow(n.imageName, onPick = pick, onRemove = { n = n.copy(imageName = null) })
                    OutlinedTextField(n.category, { n = n.copy(category = it) }, label = { Text("Kategorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Tags")
                    TagSelector(tags, n.tags, { n = n.copy(tags = it) }) { t, done -> vm.saveTag(t, done) }
                    SwitchRow("⭐ Favorit", n.favorite) { n = n.copy(favorite = it) }
                    SwitchRow("📌 Angeheftet", n.pinned) { n = n.copy(pinned = it) }
                }
            },
            confirmButton = { TextButton(onClick = { showDesign = false }) { Text("Fertig") } },
        )
    }
    if (colorDialog) {
        AlertDialog(
            onDismissRequest = { colorDialog = false },
            title = { Text("Textfarbe für Auswahl") },
            text = {
                Column {
                    Text("Markiere Text und wähle eine Farbe:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    ColorPalette(null, { c ->
                        wrap("[c=#%06X]".format(c and 0xFFFFFF), "[/c]"); colorDialog = false
                    })
                }
            },
            confirmButton = { TextButton(onClick = { colorDialog = false }) { Text("Schließen") } },
        )
    }
    if (emoji) EmojiPickerDialog(onDismiss = { emoji = false }) { insert(it); emoji = false }
    if (confirm) ConfirmDeleteDialog("Notiz löschen?", { confirm = false }) { confirm = false; vm.deleteNote(savedId); nav.back() }
}
