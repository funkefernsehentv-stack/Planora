package com.planora.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.planora.app.MainActivity
import com.planora.app.data.AppSettings
import com.planora.app.data.ChecklistEntity
import com.planora.app.data.CountdownEntity
import com.planora.app.data.EventEntity
import com.planora.app.data.Graph
import com.planora.app.data.NoteEntity
import com.planora.app.data.TaskEntity
import com.planora.app.data.TimeUnits
import com.planora.app.domain.CountdownLogic
import com.planora.app.domain.DateLogic
import com.planora.app.domain.EventLogic
import com.planora.app.domain.TaskLogic
import com.planora.app.ui.screens.Markup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle as JTextStyle

// ---------------------------------------------------------------------------------------------
// Gemeinsame Hilfsfunktionen
// ---------------------------------------------------------------------------------------------

object WidgetKeys {
    val ITEM_ID = longPreferencesKey("item_id")
    val ALPHA = floatPreferencesKey("alpha")
    val FONT = floatPreferencesKey("font")
    val CORNER = intPreferencesKey("corner")
    /** 0 = wie Countdown, 1 = Tage, 2 = Tage + Stunden, 3 = Wochen + Tage, 4 = Tage + Std + Min */
    val UNITS = intPreferencesKey("units")
    /** 0 = Design des Eintrags, sonst ARGB-Farbe */
    val BG_COLOR = intPreferencesKey("bg_color")
    val COUNT = intPreferencesKey("count")
    /** Kalender: 0 = Monat, 1 = Heute, 2 = Nächste, 3 = Woche */
    val MODE = intPreferencesKey("mode")
}

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        Graph.init(context)
        runCatching { CountdownWidget().updateAll(context) }
        runCatching { CountdownListWidget().updateAll(context) }
        runCatching { CalendarWidget().updateAll(context) }
        runCatching { ChecklistWidget().updateAll(context) }
        runCatching { NoteWidget().updateAll(context) }
        runCatching { QuickNoteWidget().updateAll(context) }
    }
}

private fun openIntent(context: Context, type: String, id: Long = 0): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("planora://$type/$id")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_OPEN_TYPE, type)
        putExtra(MainActivity.EXTRA_OPEN_ID, id)
    }

private fun tp(color: Int) = ColorProvider(Color(color))

@Composable
private fun bgBitmap(color: Int, alpha: Float, cornerDp: Int): ImageProvider {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = context.resources.displayMetrics.density
    return ImageProvider(
        CardRenderer.solid(context, color, (size.width.value * d).toInt().coerceAtLeast(10),
            (size.height.value * d).toInt().coerceAtLeast(10), cornerDp * d, alpha),
    )
}

private fun Preferences.alpha(s: AppSettings) = this[WidgetKeys.ALPHA] ?: s.widgetAlpha
private fun Preferences.font(s: AppSettings) = this[WidgetKeys.FONT] ?: s.widgetFontScale
private fun Preferences.corner(s: AppSettings) = this[WidgetKeys.CORNER] ?: s.widgetCorner

private fun widgetUnits(mode: Int, c: CountdownEntity, now: LocalDateTime, target: LocalDateTime): List<Pair<Int, Long>> =
    when (mode) {
        1 -> DateLogic.breakdown(now, target, TimeUnits.DAYS)
        2 -> DateLogic.breakdown(now, target, TimeUnits.DAYS or TimeUnits.HOURS)
        3 -> DateLogic.breakdown(now, target, TimeUnits.WEEKS or TimeUnits.DAYS)
        4 -> DateLogic.breakdown(now, target, TimeUnits.DAYS or TimeUnits.HOURS or TimeUnits.MINUTES)
        else -> if (c.style.autoUnit) listOf(DateLogic.autoUnit(now, target))
        else DateLogic.breakdown(now, target, c.style.units and TimeUnits.SECONDS.inv()).ifEmpty { listOf(DateLogic.autoUnit(now, target)) }
    }

// ---------------------------------------------------------------------------------------------
// Einzelner Countdown
// ---------------------------------------------------------------------------------------------

class CountdownWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val countdowns = Graph.db.countdowns().all()
        val tags = Graph.db.tags().all()
        val settings = Graph.settings.current()
        provideContent {
            val prefs = currentState<Preferences>()
            val now = LocalDateTime.now()
            val chosen = prefs[WidgetKeys.ITEM_ID]?.let { cid -> countdowns.firstOrNull { it.id == cid } }
                ?: CountdownLogic.sort(countdowns, 0, now, tags).firstOrNull()
            if (chosen == null) {
                EmptyWidget("Noch kein Countdown", "countdowns")
            } else {
                CountdownWidgetContent(chosen, prefs, settings, now)
            }
        }
    }
}

@Composable
private fun EmptyWidget(text: String, openType: String) {
    val context = LocalContext.current
    Box(
        GlanceModifier.fillMaxSize().background(bgBitmap(0xFF7C4DFF.toInt(), 1f, 20))
            .clickable(actionStartActivity(openIntent(context, openType))),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(color = tp(0xFFFFFFFF.toInt()), fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun CountdownWidgetContent(c: CountdownEntity, prefs: Preferences, s: AppSettings, now: LocalDateTime) {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = context.resources.displayMetrics.density
    val font = prefs.font(s)
    val corner = prefs.corner(s)
    val bgOverride = prefs[WidgetKeys.BG_COLOR]?.takeIf { it != 0 }
    val bmp = CardRenderer.render(
        context, c.style, (size.width.value * d).toInt().coerceAtLeast(10), (size.height.value * d).toInt().coerceAtLeast(10),
        corner * d, prefs.alpha(s), bgOverride,
    )
    val state = CountdownLogic.state(c, now)
    val values = widgetUnits(prefs[WidgetKeys.UNITS] ?: 0, c, now, state.target)
    val textColor = tp(c.style.textColor)
    val subColor = tp(CardRenderer.withAlpha(c.style.textColor, 0.85f))
    val dateText = DateLogic.formatDateTime(state.target, withTime = state.target.toLocalTime() != java.time.LocalTime.MIDNIGHT)
    val tall = size.height.value >= 130f
    Box(
        GlanceModifier.fillMaxSize().background(ImageProvider(bmp), ContentScale.FillBounds)
            .clickable(actionStartActivity(openIntent(context, "countdown", c.id))),
    ) {
        if (tall) {
            Column(GlanceModifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically) {
                Text(c.emoji, style = TextStyle(fontSize = (34 * font).sp))
                Text(c.title, maxLines = 2, style = TextStyle(color = textColor, fontSize = (18 * font).sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                Text(dateText, maxLines = 1, style = TextStyle(color = subColor, fontSize = (12 * font).sp))
                Spacer(GlanceModifier.height(8.dp))
                if (state.isToday) {
                    Text("Heute! 🎉", style = TextStyle(color = textColor, fontSize = (26 * font).sp, fontWeight = FontWeight.Bold))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        values.forEach { (u, v) ->
                            Column(GlanceModifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(v.toString().padStart(2, '0'), style = TextStyle(color = textColor,
                                    fontSize = (28 * font).sp, fontWeight = FontWeight.Bold))
                                Text(TimeUnits.label(u, v), style = TextStyle(color = subColor, fontSize = (11 * font).sp))
                            }
                        }
                    }
                    Text(CountdownLogic.suffix(state), style = TextStyle(color = subColor, fontSize = (11 * font).sp))
                }
            }
        } else {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(c.emoji, modifier = GlanceModifier.padding(start = 12.dp, end = 8.dp), style = TextStyle(fontSize = (30 * font).sp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(c.title, maxLines = 1, style = TextStyle(color = textColor, fontSize = (16 * font).sp, fontWeight = FontWeight.Bold))
                    Text(dateText, maxLines = 1, style = TextStyle(color = subColor, fontSize = (11 * font).sp))
                }
                val primary = CountdownLogic.primary(values)
                Box(
                    GlanceModifier.fillMaxHeight().width(if (values.size > 1) 120.dp else 86.dp)
                        .background(ColorProvider(Color(0x22000000))),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isToday) {
                            Text("🎉", style = TextStyle(fontSize = (26 * font).sp))
                            Text("heute", style = TextStyle(color = subColor, fontSize = (11 * font).sp))
                        } else if (values.size > 1) {
                            Text(values.joinToString(" ") { "${it.second}${TimeUnits.short(it.first)}" }, maxLines = 2,
                                style = TextStyle(color = textColor, fontSize = (15 * font).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                            Text(CountdownLogic.suffix(state), style = TextStyle(color = subColor, fontSize = (11 * font).sp))
                        } else {
                            Text(primary.second.toString(), style = TextStyle(color = textColor, fontSize = (28 * font).sp, fontWeight = FontWeight.Bold))
                            Text("${TimeUnits.label(primary.first, primary.second)} ${CountdownLogic.suffix(state)}",
                                maxLines = 1, style = TextStyle(color = subColor, fontSize = (10 * font).sp))
                        }
                    }
                }
            }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

// ---------------------------------------------------------------------------------------------
// Liste der nächsten Countdowns
// ---------------------------------------------------------------------------------------------

class CountdownListWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val countdowns = Graph.db.countdowns().all()
        val tags = Graph.db.tags().all()
        val settings = Graph.settings.current()
        provideContent {
            val prefs = currentState<Preferences>()
            val now = LocalDateTime.now()
            // Nächste Countdowns immer oben
            val sorted = CountdownLogic.sort(countdowns, 0, now, tags).take(prefs[WidgetKeys.COUNT] ?: 6)
            val font = prefs.font(settings)
            val bg = prefs[WidgetKeys.BG_COLOR]?.takeIf { it != 0 } ?: 0xFF1E1B2E.toInt()
            Column(GlanceModifier.fillMaxSize().background(bgBitmap(bg, prefs.alpha(settings), prefs.corner(settings))).padding(8.dp)) {
                Text("⏳ Nächste Countdowns", modifier = GlanceModifier.padding(start = 6.dp, bottom = 6.dp)
                    .clickable(actionStartActivity(openIntent(LocalContext.current, "countdowns"))),
                    style = TextStyle(color = tp(0xFFFFFFFF.toInt()), fontSize = (14 * font).sp, fontWeight = FontWeight.Bold))
                if (sorted.isEmpty()) {
                    Text("Noch keine Countdowns", style = TextStyle(color = tp(0xCCFFFFFF.toInt())))
                }
                LazyColumn {
                    items(sorted, itemId = { it.id }) { c ->
                        Column {
                            CountdownRow(c, now, font)
                            Spacer(GlanceModifier.height(5.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownRow(c: CountdownEntity, now: LocalDateTime, font: Float) {
    val context = LocalContext.current
    val state = CountdownLogic.state(c, now)
    val (u, v) = DateLogic.autoUnit(now, state.target)
    val text = tp(c.style.textColor)
    val color = c.style.color
    Row(
        GlanceModifier.fillMaxWidth().height((52 * font).dp).background(ColorProvider(Color(color))).cornerRadius(14.dp)
            .clickable(actionStartActivity(openIntent(context, "countdown", c.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.emoji, modifier = GlanceModifier.padding(start = 10.dp, end = 8.dp), style = TextStyle(fontSize = (22 * font).sp))
        Column(GlanceModifier.defaultWeight()) {
            Text(c.title, maxLines = 1, style = TextStyle(color = text, fontSize = (14 * font).sp, fontWeight = FontWeight.Bold))
            Text(DateLogic.formatDay(state.target.toLocalDate()), maxLines = 1,
                style = TextStyle(color = tp(CardRenderer.withAlpha(c.style.textColor, 0.8f)), fontSize = (10 * font).sp))
        }
        Box(GlanceModifier.fillMaxHeight().width(70.dp).background(ColorProvider(Color(0x22000000))), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (state.isToday) "🎉" else v.toString(), style = TextStyle(color = text, fontSize = (18 * font).sp, fontWeight = FontWeight.Bold))
                Text(if (state.isToday) "heute" else TimeUnits.label(u, v), maxLines = 1,
                    style = TextStyle(color = tp(CardRenderer.withAlpha(c.style.textColor, 0.8f)), fontSize = (9 * font).sp))
            }
        }
    }
}

class CountdownListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownListWidget()
}

// ---------------------------------------------------------------------------------------------
// Kalender
// ---------------------------------------------------------------------------------------------

class CalendarWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val events = Graph.db.events().all()
        val settings = Graph.settings.current()
        provideContent {
            val prefs = currentState<Preferences>()
            val font = prefs.font(settings)
            val bg = prefs[WidgetKeys.BG_COLOR]?.takeIf { it != 0 } ?: 0xFFFFFFFF.toInt()
            val dark = isDark(bg)
            val fg = if (dark) 0xFFFFFFFF.toInt() else 0xFF1F1B24.toInt()
            Column(
                GlanceModifier.fillMaxSize().background(bgBitmap(bg, prefs.alpha(settings), prefs.corner(settings))).padding(10.dp)
                    .clickable(actionStartActivity(openIntent(LocalContext.current, "calendar"))),
            ) {
                when (prefs[WidgetKeys.MODE] ?: 0) {
                    1 -> EventListContent("Heute", events, LocalDate.now(), LocalDate.now(), fg, font, settings)
                    2 -> EventListContent("Nächste Termine", events, LocalDate.now(), LocalDate.now().plusDays(60), fg, font, settings)
                    3 -> WeekContent(events, fg, font, settings)
                    else -> MonthContent(events, fg, font, settings)
                }
            }
        }
    }
}

private fun isDark(color: Int): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) < 140
}

@Composable
private fun MonthContent(events: List<EventEntity>, fg: Int, font: Float, s: AppSettings) {
    val today = LocalDate.now()
    val first = today.withDayOfMonth(1)
    val firstDow = if (s.weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    var gridStart = first
    while (gridStart.dayOfWeek != firstDow) gridStart = gridStart.minusDays(1)
    val gridEnd = gridStart.plusDays(41)
    val byDay = EventLogic.byDay(EventLogic.occurrences(events, gridStart, gridEnd), gridStart, gridEnd)
    val size = LocalSize.current
    val rowH = ((size.height.value - 60f) / 7f).coerceAtLeast(14f)
    Text(DateLogic.formatMonth(today), style = TextStyle(color = tp(fg), fontSize = (15 * font).sp, fontWeight = FontWeight.Bold))
    Spacer(GlanceModifier.height(4.dp))
    Row(GlanceModifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val dow = firstDow.plus(i.toLong())
            Text(dow.getDisplayName(JTextStyle.SHORT, DateLogic.locale).take(2), modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = tp(CardRenderer.withAlpha(fg, 0.6f)), fontSize = (10 * font).sp, textAlign = TextAlign.Center))
        }
    }
    for (week in 0 until 6) {
        Row(GlanceModifier.fillMaxWidth().height(rowH.dp), verticalAlignment = Alignment.CenterVertically) {
            for (dayIdx in 0 until 7) {
                val day = gridStart.plusDays((week * 7 + dayIdx).toLong())
                val inMonth = day.month == today.month
                val isToday = day == today
                val evs = byDay[day].orEmpty()
                Box(GlanceModifier.defaultWeight().fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Box(
                        if (isToday) GlanceModifier.size((rowH - 2).coerceAtMost(26f).dp).background(ColorProvider(Color(0xFF7C4DFF))).cornerRadius(13.dp)
                        else GlanceModifier.size((rowH - 2).coerceAtMost(26f).dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.dayOfMonth.toString(), style = TextStyle(
                                color = tp(if (isToday) 0xFFFFFFFF.toInt() else if (inMonth) fg else CardRenderer.withAlpha(fg, 0.35f)),
                                fontSize = (11 * font).sp, fontWeight = if (evs.isNotEmpty() || isToday) FontWeight.Bold else FontWeight.Normal))
                            if (evs.isNotEmpty() && rowH > 24f) {
                                Text("•", style = TextStyle(color = tp(if (isToday) 0xFFFFFFFF.toInt() else evs.first().event.color), fontSize = (9 * font).sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekContent(events: List<EventEntity>, fg: Int, font: Float, s: AppSettings) {
    val today = LocalDate.now()
    val firstDow = if (s.weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    var start = today
    while (start.dayOfWeek != firstDow) start = start.minusDays(1)
    val end = start.plusDays(6)
    val byDay = EventLogic.byDay(EventLogic.occurrences(events, start, end), start, end)
    Text("Diese Woche", style = TextStyle(color = tp(fg), fontSize = (15 * font).sp, fontWeight = FontWeight.Bold))
    Spacer(GlanceModifier.height(4.dp))
    LazyColumn {
        items((0..6).map { start.plusDays(it.toLong()) }, itemId = { it.toEpochDay() }) { day ->
            val evs = byDay[day].orEmpty()
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    day.dayOfWeek.getDisplayName(JTextStyle.SHORT, DateLogic.locale) + " " + day.dayOfMonth,
                    modifier = GlanceModifier.width(52.dp),
                    style = TextStyle(color = tp(if (day == today) 0xFF7C4DFF.toInt() else fg), fontSize = (12 * font).sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    if (evs.isEmpty()) "–" else evs.joinToString("  ") { "${it.event.emoji} ${it.event.title}" },
                    maxLines = 2,
                    style = TextStyle(color = tp(CardRenderer.withAlpha(fg, 0.85f)), fontSize = (12 * font).sp),
                )
            }
        }
    }
}

@Composable
private fun EventListContent(title: String, events: List<EventEntity>, from: LocalDate, to: LocalDate, fg: Int, font: Float, s: AppSettings) {
    val now = LocalDateTime.now()
    val occ = EventLogic.occurrences(events, from, to).filter { !it.end.isBefore(now) || it.event.allDay }.take(25)
    Text(if (from == to) "$title · ${DateLogic.formatDayShort(from)}" else title,
        style = TextStyle(color = tp(fg), fontSize = (15 * font).sp, fontWeight = FontWeight.Bold))
    Spacer(GlanceModifier.height(4.dp))
    if (occ.isEmpty()) {
        Text("Keine Termine 🎈", style = TextStyle(color = tp(CardRenderer.withAlpha(fg, 0.7f)), fontSize = (12 * font).sp))
    }
    LazyColumn {
        items(occ, itemId = { it.event.id * 100_000 + it.start.toLocalDate().toEpochDay() }) { o ->
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.width(4.dp).height(30.dp).background(ColorProvider(Color(o.event.color)))) {}
                Spacer(GlanceModifier.width(6.dp))
                Text(o.event.emoji, style = TextStyle(fontSize = (16 * font).sp))
                Spacer(GlanceModifier.width(6.dp))
                Column {
                    Text(o.event.title, maxLines = 1, style = TextStyle(color = tp(fg), fontSize = (13 * font).sp, fontWeight = FontWeight.Medium))
                    Text(
                        (if (from != to) DateLogic.formatDayShort(o.start.toLocalDate()) + " · " else "") +
                            if (o.event.allDay) "Ganztägig" else "${DateLogic.formatTime(o.start)}–${DateLogic.formatTime(o.end)}",
                        style = TextStyle(color = tp(CardRenderer.withAlpha(fg, 0.65f)), fontSize = (11 * font).sp),
                    )
                }
            }
        }
    }
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

// ---------------------------------------------------------------------------------------------
// Checkliste (mit Abhaken direkt im Widget)
// ---------------------------------------------------------------------------------------------

val TaskIdKey = ActionParameters.Key<Long>("task_id")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Graph.init(context)
        val id = parameters[TaskIdKey] ?: return
        Graph.repo.toggleTask(id)
        ChecklistWidget().update(context, glanceId)
    }
}

class ChecklistWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val lists = Graph.db.checklists().all()
        val tasks = Graph.db.checklists().allTasks()
        val settings = Graph.settings.current()
        provideContent {
            val prefs = currentState<Preferences>()
            val list: ChecklistEntity? = prefs[WidgetKeys.ITEM_ID]?.let { cid -> lists.firstOrNull { it.id == cid } }
                ?: lists.minByOrNull { it.sortIndex }
            if (list == null) {
                EmptyWidget("Noch keine Checkliste", "checklists")
            } else {
                ChecklistContent(list, TaskLogic.sort(tasks.filter { it.checklistId == list.id }, list.sortMode), prefs, settings)
            }
        }
    }
}

@Composable
private fun ChecklistContent(list: ChecklistEntity, tasks: List<TaskEntity>, prefs: Preferences, s: AppSettings) {
    val context = LocalContext.current
    val font = prefs.font(s)
    val bg = prefs[WidgetKeys.BG_COLOR]?.takeIf { it != 0 } ?: list.color
    val fg = list.textColor
    val visible = if (list.hideDone) tasks.filter { !it.done } else tasks
    val done = tasks.count { it.done }
    Column(GlanceModifier.fillMaxSize().background(bgBitmap(bg, prefs.alpha(s), prefs.corner(s))).padding(10.dp)) {
        Row(
            GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openIntent(context, "checklist", list.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${list.emoji} ${list.title}", modifier = GlanceModifier.defaultWeight(), maxLines = 1,
                style = TextStyle(color = tp(fg), fontSize = (15 * font).sp, fontWeight = FontWeight.Bold))
            Text("$done/${tasks.size}", style = TextStyle(color = tp(CardRenderer.withAlpha(fg, 0.8f)), fontSize = (12 * font).sp))
        }
        Spacer(GlanceModifier.height(4.dp))
        if (visible.isEmpty()) {
            Text("Alles erledigt 🎉", style = TextStyle(color = tp(fg), fontSize = (13 * font).sp))
        }
        LazyColumn {
            items(visible, itemId = { it.id }) { t ->
                CheckBox(
                    checked = t.done,
                    onCheckedChange = actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to t.id)),
                    text = (if (t.priority == 3) "❗ " else "") + t.text +
                        (DateLogic.parse(t.due)?.let { "  · " + DateLogic.formatDayShort(it.toLocalDate()) } ?: ""),
                    style = TextStyle(color = tp(fg), fontSize = (13 * font).sp,
                        textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None),
                    maxLines = 2,
                )
            }
        }
    }
}

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistWidget()
}

// ---------------------------------------------------------------------------------------------
// Notizen
// ---------------------------------------------------------------------------------------------

class NoteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val notes = Graph.db.notes().all()
        val settings = Graph.settings.current()
        provideContent {
            val prefs = currentState<Preferences>()
            val note: NoteEntity? = prefs[WidgetKeys.ITEM_ID]?.let { nid -> notes.firstOrNull { it.id == nid } }
                ?: notes.sortedWith(compareByDescending<NoteEntity> { it.pinned }.thenByDescending { it.updatedAt }).firstOrNull()
            val context2 = LocalContext.current
            val font = prefs.font(settings)
            val bg = prefs[WidgetKeys.BG_COLOR]?.takeIf { it != 0 } ?: note?.color ?: 0xFFFFF3C4.toInt()
            val fg = note?.textColor ?: if (isDark(bg)) 0xFFFFFFFF.toInt() else 0xFF2B2B2B.toInt()
            Column(GlanceModifier.fillMaxSize().background(bgBitmap(bg, prefs.alpha(settings), prefs.corner(settings))).padding(12.dp)) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(note?.title ?: "Notizen", modifier = GlanceModifier.defaultWeight()
                        .clickable(actionStartActivity(openIntent(context2, "note", note?.id ?: 0))),
                        maxLines = 1, style = TextStyle(color = tp(fg), fontSize = (15 * font).sp, fontWeight = FontWeight.Bold))
                    Box(
                        GlanceModifier.size(30.dp).background(ColorProvider(Color(0xFF7C4DFF))).cornerRadius(15.dp)
                            .clickable(actionStartActivity(openIntent(context2, "new_note"))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", style = TextStyle(color = tp(0xFFFFFFFF.toInt()), fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    note?.let { Markup.toPlain(it.content) } ?: "Tippe auf +, um eine Notiz zu erstellen.",
                    modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(openIntent(context2, "note", note?.id ?: 0))),
                    style = TextStyle(color = tp(CardRenderer.withAlpha(fg, 0.9f)), fontSize = (13 * font).sp),
                )
            }
        }
    }
}

class NoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteWidget()
}

class QuickNoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                GlanceModifier.fillMaxSize().background(bgBitmap(0xFF7C4DFF.toInt(), 1f, 22))
                    .clickable(actionStartActivity(openIntent(LocalContext.current, "new_note"))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝", style = TextStyle(fontSize = 24.sp))
                    Text("+ Notiz", style = TextStyle(color = tp(0xFFFFFFFF.toInt()), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

class QuickNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickNoteWidget()
}
