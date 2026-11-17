package com.planora.app.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.planora.app.data.CountdownEntity
import com.planora.app.data.CountdownStyle
import com.planora.app.data.TagEntity
import com.planora.app.data.TimeUnits
import com.planora.app.domain.CountdownLogic
import com.planora.app.domain.CountdownState
import com.planora.app.domain.DateLogic
import com.planora.app.ui.AppViewModel
import com.planora.app.ui.Nav
import com.planora.app.ui.Routes
import com.planora.app.ui.TopBarActions
import com.planora.app.ui.components.CountdownIcons
import com.planora.app.ui.components.EmptyState
import com.planora.app.ui.components.StyledBackground
import com.planora.app.ui.components.TagRow
import com.planora.app.ui.components.imageFile
import com.planora.app.ui.components.rememberNow
import com.planora.app.ui.theme.Spacing
import com.planora.app.ui.theme.fontFamilyOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime
import java.time.LocalTime

// ---------------------------------------------------------------------------------------------
// Countdown-Karte (Design nach Referenzbild, vollständig konfigurierbar)
// ---------------------------------------------------------------------------------------------

fun cardShape(style: CountdownStyle): Shape = when (style.cardShape) {
    1 -> CutCornerShape(style.cornerRadius.dp)
    2 -> RoundedCornerShape(50)
    3 -> RectangleShape
    else -> RoundedCornerShape(style.cornerRadius.dp)
}

@Composable
fun CountdownSymbol(c: CountdownEntity, sizeDp: Int, tint: Color) {
    val thumb = imageFile(c.thumbnail)
    val icon = CountdownIcons.byName(c.icon)
    when {
        thumb != null -> AsyncImage(
            model = thumb, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(sizeDp.dp).clip(CircleShape),
        )
        icon != null -> Icon(icon, null, tint = tint, modifier = Modifier.size(sizeDp.dp))
        else -> Text(c.emoji, fontSize = (sizeDp * 0.8f).sp)
    }
}

fun dateLine(c: CountdownEntity, s: CountdownState): String {
    val t = s.target
    val withTime = t.toLocalTime() != LocalTime.MIDNIGHT
    val end = DateLogic.parse(c.endDate)
    return if (end != null && end.isAfter(DateLogic.parse(c.target) ?: t)) {
        val base = DateLogic.parse(c.target) ?: t
        val shift = java.time.Duration.between(base, end)
        val occEnd = t.plus(shift)
        "${DateLogic.formatDay(t.toLocalDate())} – ${DateLogic.formatDay(occEnd.toLocalDate())}"
    } else DateLogic.formatDateTime(t, withTime)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CountdownCard(
    c: CountdownEntity,
    now: LocalDateTime,
    tags: List<TagEntity> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val st = c.style
    val state = CountdownLogic.state(c, now)
    val values = CountdownLogic.displayValues(c, state, now)
    val primary = CountdownLogic.primary(values)
    val textColor = Color(st.textColor)
    val sub = textColor.copy(alpha = 0.85f)
    val family = fontFamilyOf(st.fontFamily)
    val shape = cardShape(st)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        StyledBackground(
            st,
            modifier
                .fillMaxWidth(st.widthFraction.coerceIn(0.4f, 1f))
                .then(if (st.shadow) Modifier.shadow(6.dp, shape, clip = false) else Modifier)
                .clip(shape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = st.height.dp),
        ) {
            val timeBlock: @Composable (Modifier) -> Unit = { m ->
                Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    when {
                        state.isToday -> {
                            Text("🎉", fontSize = (st.numberSize * 0.85f).sp)
                            Text("heute!", color = sub, fontSize = 11.sp, fontFamily = family)
                        }
                        st.autoUnit || values.size == 1 -> {
                            Text(primary.second.toString(), color = textColor, fontSize = st.numberSize.sp,
                                fontWeight = FontWeight.Bold, fontFamily = family, maxLines = 1)
                            Text("${TimeUnits.label(primary.first, primary.second)} ${CountdownLogic.suffix(state)}",
                                color = sub, fontSize = 11.sp, fontFamily = family, maxLines = 1)
                        }
                        else -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                values.forEach { (u, v) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (u == TimeUnits.YEARS || u == TimeUnits.MONTHS || u == TimeUnits.WEEKS) v.toString() else v.toString().padStart(2, '0'),
                                            color = textColor, fontSize = (st.numberSize * if (values.size > 3) 0.62f else 0.78f).sp,
                                            fontWeight = FontWeight.Bold, fontFamily = family)
                                        Text(TimeUnits.short(u), color = sub, fontSize = 10.sp, fontFamily = family)
                                    }
                                }
                            }
                            Text(CountdownLogic.suffix(state), color = sub, fontSize = 10.sp, fontFamily = family)
                        }
                    }
                }
            }
            val titleBlock: @Composable (Modifier, Boolean) -> Unit = { m, showTime ->
                Column(
                    m.padding(vertical = 8.dp),
                    horizontalAlignment = if (st.titleAlign == 1) Alignment.CenterHorizontally else Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(c.title, color = textColor, fontSize = st.titleSize.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = family, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = if (st.titleAlign == 1) TextAlign.Center else TextAlign.Start)
                    if (st.showDate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (c.repeat.isRepeating) Icons.Filled.Repeat else Icons.Filled.CalendarToday, null,
                                tint = sub, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(dateLine(c, state), color = sub, fontSize = 12.sp, fontFamily = family, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (st.showDescription && c.description.isNotBlank()) {
                        Text(c.description, color = sub, fontSize = 12.sp, fontFamily = family, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (tags.isNotEmpty() && c.tags.isNotEmpty() && st.height >= 90) {
                        Spacer(Modifier.height(4.dp)); TagRow(c.tags, tags)
                    }
                    if (showTime) { Spacer(Modifier.height(6.dp)); timeBlock(Modifier) }
                }
            }
            val pad = st.padding.dp
            when (st.layout) {
                1 -> Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Groß
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CountdownSymbol(c, 40, textColor); Spacer(Modifier.width(10.dp))
                        titleBlock(Modifier.weight(1f, fill = false), false)
                    }
                    val all = if (state.isToday) emptyList() else CountdownLogic.displayValues(c, state, now, forceAll = true)
                    if (state.isToday) Text("Heute ist es so weit! 🎉", color = textColor, fontSize = (st.numberSize * 0.7f).sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                        all.forEach { (u, v) ->
                            Column(
                                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(v.toString().padStart(2, '0'), color = textColor, fontSize = st.numberSize.sp, fontWeight = FontWeight.Bold, fontFamily = family)
                                Text(TimeUnits.label(u, v), color = sub, fontSize = 10.sp, fontFamily = family)
                            }
                        }
                    }
                    if (!state.isToday) Text(CountdownLogic.suffix(state), color = sub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                2 -> Row(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = pad, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Kompakt
                    CountdownSymbol(c, 22, textColor); Spacer(Modifier.width(8.dp))
                    Text(c.title, color = textColor, fontSize = (st.titleSize * 0.85f).sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = family, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        if (state.isToday) "heute 🎉" else values.joinToString(" ") { "${it.second} ${TimeUnits.short(it.first)}" },
                        color = textColor, fontWeight = FontWeight.Bold, fontSize = (st.numberSize * 0.55f).sp, fontFamily = family,
                    )
                    trailing?.invoke()
                }
                3 -> Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Zentriert
                    CountdownSymbol(c, 36, textColor)
                    Text(c.title, color = textColor, fontSize = st.titleSize.sp, fontWeight = FontWeight.SemiBold, fontFamily = family, textAlign = TextAlign.Center)
                    if (st.showDate) Text(dateLine(c, state), color = sub, fontSize = 12.sp, fontFamily = family)
                    Spacer(Modifier.height(4.dp))
                    timeBlock(Modifier)
                }
                else -> Row(Modifier.fillMaxWidth().heightIn(min = st.height.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    // Klassisch wie im Referenzbild: Symbol links, Titel/Datum Mitte, Zeit rechts
                    val symbol: @Composable () -> Unit = {
                        Box(Modifier.padding(horizontal = pad).widthIn(min = 40.dp), contentAlignment = Alignment.Center) {
                            CountdownSymbol(c, 40, textColor)
                        }
                    }
                    if (!st.emojiRight) symbol() else Spacer(Modifier.width(pad))
                    titleBlock(Modifier.weight(1f), st.timePosition == 1)
                    if (st.emojiRight) symbol()
                    if (st.timePosition == 0) {
                        Box(
                            Modifier.fillMaxHeight().widthIn(min = 92.dp)
                                .background(if (st.showPanel) Color.Black.copy(alpha = 0.13f) else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) { timeBlock(Modifier) }
                    } else Spacer(Modifier.width(pad))
                    trailing?.invoke()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Countdown-Übersicht
// ---------------------------------------------------------------------------------------------

private val sortLabels = listOf("Nächste zuerst", "Nach Erstellung", "Alphabetisch", "Manuell (Drag & Drop)", "Nach Tags")
private val groupLabels = listOf("🎉 Heute", "⏳ Als Nächstes", "↗️ Hochzählend", "✔️ Vergangen")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownsScreen(vm: AppViewModel, nav: Nav, outer: PaddingValues) {
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val now = rememberNow(1000)
    var sortMenu by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableLongStateOf(0L) }
    var menuFor by remember { mutableStateOf<CountdownEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<CountdownEntity?>(null) }
    val mode = settings.countdownSort
    val filtered = countdowns.filter { tagFilter == 0L || tagFilter in it.tags }
    // Sortierung nur minütlich neu berechnen, damit Drag & Drop flüssig bleibt
    val minute = now.withSecond(0).withNano(0)
    val sorted = remember(filtered, mode, minute, tags) { CountdownLogic.sort(filtered, mode, minute, tags) }
    var items by remember(sorted) { mutableStateOf(sorted) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = items.indexOfFirst { it.id == from.key }
        val toIdx = items.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) items = items.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = { Text("Countdowns", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sortieren") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            sortLabels.forEachIndexed { i, l ->
                                DropdownMenuItem(
                                    text = { Text((if (i == mode) "✓ " else "") + l) },
                                    onClick = { vm.updateSettings { it.copy(countdownSort = i) }; sortMenu = false },
                                )
                            }
                        }
                    }
                    TopBarActions(nav)
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.go(Routes.countdownEdit(0)) },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Countdown") },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (tags.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.screen()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = tagFilter == 0L, onClick = { tagFilter = 0 }, label = { Text("Alle") })
                    tags.forEach { t ->
                        FilterChip(selected = tagFilter == t.id, onClick = { tagFilter = if (tagFilter == t.id) 0 else t.id },
                            label = { Text("${t.emoji} ${t.name}") })
                    }
                }
            }
            if (items.isEmpty()) {
                EmptyState("⏳", "Noch keine Countdowns", "Tippe auf „+ Countdown“, um den ersten zu erstellen.")
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = Spacing.screen(), end = Spacing.screen(), top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.gap()),
            ) {
                items(items, key = { it.id }) { c ->
                    ReorderableItem(reorderState, key = c.id, enabled = mode == 3) { dragging ->
                        Column {
                            if (mode == 0) {
                                val idx = items.indexOf(c)
                                val label = headerFor(c, now)
                                val prevLabel = items.getOrNull(idx - 1)?.let { headerFor(it, now) }
                                if (label != prevLabel) {
                                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp))
                                }
                            }
                            Box {
                                CountdownCard(
                                    c, now, tags,
                                    modifier = Modifier.animateContentSize().then(if (dragging) Modifier.shadow(16.dp, RoundedCornerShape(20.dp)) else Modifier),
                                    onClick = { nav.go(Routes.countdown(c.id)) },
                                    onLongClick = { menuFor = c },
                                    trailing = if (mode == 3) {
                                        {
                                            IconButton(
                                                onClick = {},
                                                modifier = Modifier.draggableHandle(onDragStopped = { vm.reorderCountdowns(items.map { it.id }) }),
                                            ) { Icon(Icons.Default.DragHandle, "Verschieben", tint = Color(c.style.textColor)) }
                                        }
                                    } else null,
                                )
                                DropdownMenu(expanded = menuFor?.id == c.id, onDismissRequest = { menuFor = null }) {
                                    CountdownMenuItems(c, vm, nav, onDelete = { confirmDelete = c }) { menuFor = null }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { c ->
        ConfirmDeleteDialog("„${c.title}“ löschen?", onDismiss = { confirmDelete = null }) {
            vm.deleteCountdown(c.id); confirmDelete = null
        }
    }
}

private fun headerFor(c: CountdownEntity, now: LocalDateTime): String =
    if (c.pinned) "📌 Angeheftet" else groupLabels[CountdownLogic.state(c, now).group]

@Composable
fun CountdownMenuItems(c: CountdownEntity, vm: AppViewModel, nav: Nav, onDelete: () -> Unit, close: () -> Unit) {
    DropdownMenuItem(text = { Text("Bearbeiten") }, leadingIcon = { Icon(Icons.Default.Edit, null) },
        onClick = { close(); nav.go(Routes.countdownEdit(c.id)) })
    DropdownMenuItem(text = { Text("Duplizieren") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
        onClick = { close(); vm.duplicateCountdown(c) })
    DropdownMenuItem(text = { Text(if (c.favorite) "Aus Favoriten entfernen" else "Zu Favoriten") },
        leadingIcon = { Icon(if (c.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
        onClick = { close(); vm.saveCountdown(c.copy(favorite = !c.favorite)) })
    DropdownMenuItem(text = { Text(if (c.pinned) "Lösen" else "Oben anheften") },
        leadingIcon = { Icon(if (c.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null) },
        onClick = { close(); vm.saveCountdown(c.copy(pinned = !c.pinned)) })
    DropdownMenuItem(text = { Text("Löschen") }, leadingIcon = { Icon(Icons.Default.Delete, null) },
        onClick = { close(); onDelete() })
}

@Composable
fun ConfirmDeleteDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("Das kann nicht rückgängig gemacht werden.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Löschen", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

// ---------------------------------------------------------------------------------------------
// Detailansicht mit Live-Countdown
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownDetailScreen(vm: AppViewModel, nav: Nav, id: Long) {
    val countdowns by vm.countdowns.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val c = countdowns.firstOrNull { it.id == id }
    val now = rememberNow(1000)
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(countdowns) { if (countdowns.isNotEmpty()) loadedOnce = true }
    if (c == null) {
        if (loadedOnce) LaunchedEffect(Unit) { nav.back() }
        return
    }
    val st = c.style
    val state = CountdownLogic.state(c, now)
    val values = CountdownLogic.displayValues(c, state, now, forceAll = true)
    val textColor = Color(st.textColor)
    val family = fontFamilyOf(st.fontFamily)

    StyledBackground(st.copy(alpha = 1f), Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent,
                        navigationIconContentColor = textColor, actionIconContentColor = textColor),
                    navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                    actions = {
                        IconButton(onClick = { vm.saveCountdown(c.copy(favorite = !c.favorite)) }) {
                            Icon(if (c.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favorit")
                        }
                        IconButton(onClick = {
                            val text = "${c.emoji} ${c.title}: " + if (state.isToday) "heute! 🎉" else
                                values.filter { it.second > 0 }.joinToString(", ") { "${it.second} ${TimeUnits.label(it.first, it.second)}" } +
                                " ${CountdownLogic.suffix(state)} (${dateLine(c, state)})"
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                            }, "Teilen"))
                        }) { Icon(Icons.Default.Share, "Teilen") }
                        IconButton(onClick = { vm.duplicateCountdown(c) }) { Icon(Icons.Default.ContentCopy, "Duplizieren") }
                        IconButton(onClick = { nav.go(Routes.countdownEdit(c.id)) }) { Icon(Icons.Default.Edit, "Bearbeiten") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Löschen") }
                    },
                )
            },
        ) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(24.dp))
                CountdownSymbol(c, 84, textColor)
                Spacer(Modifier.height(12.dp))
                Text(c.title, color = textColor, fontSize = (st.titleSize * 1.5f).sp, fontWeight = FontWeight.Bold,
                    fontFamily = family, textAlign = TextAlign.Center)
                Text(dateLine(c, state), color = textColor.copy(alpha = 0.85f), fontFamily = family)
                Spacer(Modifier.height(28.dp))
                if (state.isToday) {
                    Text("Heute ist es so weit! 🎉", color = textColor, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        fontFamily = family, textAlign = TextAlign.Center)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        values.forEach { (u, v) ->
                            Column(
                                Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.18f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(v.toString().padStart(2, '0'), color = textColor, fontSize = if (values.size > 4) 26.sp else 36.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = family)
                                Text(TimeUnits.label(u, v), color = textColor.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = family)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            state.expired -> "ist vorbei"
                            state.countingUp -> "sind vergangen"
                            else -> "verbleibend"
                        },
                        color = textColor.copy(alpha = 0.85f), fontFamily = family,
                    )
                }
                Spacer(Modifier.height(24.dp))
                if (c.description.isNotBlank()) {
                    Text(c.description, color = textColor, textAlign = TextAlign.Center, fontFamily = family,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.15f)).padding(14.dp))
                    Spacer(Modifier.height(12.dp))
                }
                if (c.repeat.isRepeating) Text("🔁 ${c.repeat.describe()}", color = textColor.copy(alpha = 0.9f))
                if (c.reminders.isNotEmpty()) {
                    Text("🔔 " + c.reminders.sortedDescending().joinToString(", ") { DateLogic.describeOffset(it) },
                        color = textColor.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
                TagRow(c.tags, tags, small = false)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog("„${c.title}“ löschen?", onDismiss = { confirmDelete = false }) {
            confirmDelete = false
            vm.deleteCountdown(c.id)
            nav.back()
        }
    }
}
