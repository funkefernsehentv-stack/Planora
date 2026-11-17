package com.planora.app.data

import android.content.Context
import androidx.room.withTransaction
import com.planora.app.domain.DateLogic
import com.planora.app.domain.TaskLogic
import com.planora.app.notify.ReminderScheduler
import com.planora.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Einfacher Service-Locator für Datenbank, Einstellungen und Repository. */
object Graph {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var repo: Repository
        private set
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        db = AppDatabase.get(app)
        settings = SettingsRepository(app)
        repo = Repository(app, db, settings)
        initialized = true
    }
}

@OptIn(FlowPreview::class)
class Repository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        // Nach Änderungen Erinnerungen neu planen und Widgets aktualisieren (gebündelt)
        Graph.appScope.launch {
            changes.debounce(400).collect {
                runCatching { ReminderScheduler.rescheduleAll(context) }
                runCatching { WidgetUpdater.updateAll(context) }
            }
        }
    }

    fun notifyChanged() {
        changes.tryEmit(Unit)
    }

    val tags = db.tags().observe()
    val events = db.events().observe()
    val countdowns = db.countdowns().observe()
    val checklists = db.checklists().observe()
    val tasks = db.checklists().observeTasks()
    val notes = db.notes().observe()
    val decor = db.decor().observe()

    // ---------- Tags ----------
    suspend fun saveTag(t: TagEntity): Long = db.tags().upsert(t).also { notifyChanged() }

    suspend fun deleteTag(t: TagEntity) {
        db.withTransaction {
            db.tags().delete(t)
            val id = t.id
            db.events().all().filter { id in it.tags }.forEach { db.events().upsert(it.copy(tags = it.tags - id)) }
            db.countdowns().all().filter { id in it.tags }.forEach { db.countdowns().upsert(it.copy(tags = it.tags - id)) }
            db.checklists().all().filter { id in it.tags }.forEach { db.checklists().upsert(it.copy(tags = it.tags - id)) }
            db.checklists().allTasks().filter { id in it.tags }.forEach { db.checklists().upsertTask(it.copy(tags = it.tags - id)) }
            db.notes().all().filter { id in it.tags }.forEach { db.notes().upsert(it.copy(tags = it.tags - id)) }
        }
        notifyChanged()
    }

    // ---------- Termine ----------
    suspend fun event(id: Long) = db.events().get(id)
    suspend fun saveEvent(e: EventEntity): Long = db.events().upsert(e).also { notifyChanged() }
    suspend fun deleteEvent(id: Long) { db.events().delete(id); notifyChanged() }
    suspend fun duplicateEvent(e: EventEntity): Long =
        saveEvent(e.copy(id = 0, title = e.title + " (Kopie)", createdAt = System.currentTimeMillis()))

    // ---------- Countdowns ----------
    suspend fun countdown(id: Long) = db.countdowns().get(id)
    suspend fun saveCountdown(c: CountdownEntity): Long {
        val toSave = if (c.id == 0L) c.copy(sortIndex = (db.countdowns().all().maxOfOrNull { it.sortIndex } ?: 0) + 1) else c
        return db.countdowns().upsert(toSave).also { notifyChanged() }
    }
    suspend fun deleteCountdown(id: Long) { db.countdowns().delete(id); notifyChanged() }
    suspend fun duplicateCountdown(c: CountdownEntity): Long =
        saveCountdown(c.copy(id = 0, title = c.title + " (Kopie)", createdAt = System.currentTimeMillis(), pinned = false))
    suspend fun reorderCountdowns(orderedIds: List<Long>) {
        val all = db.countdowns().all().associateBy { it.id }
        db.countdowns().updateAll(orderedIds.mapIndexedNotNull { i, id -> all[id]?.copy(sortIndex = i) })
        notifyChanged()
    }

    // ---------- Checklisten ----------
    suspend fun checklist(id: Long) = db.checklists().get(id)
    suspend fun saveChecklist(c: ChecklistEntity): Long {
        val toSave = if (c.id == 0L) c.copy(sortIndex = (db.checklists().all().maxOfOrNull { it.sortIndex } ?: 0) + 1) else c
        return db.checklists().upsert(toSave).also { notifyChanged() }
    }
    suspend fun deleteChecklist(id: Long) {
        db.withTransaction {
            db.checklists().deleteTasksOf(id)
            db.checklists().delete(id)
        }
        notifyChanged()
    }
    suspend fun duplicateChecklist(c: ChecklistEntity): Long {
        val newId = saveChecklist(c.copy(id = 0, title = c.title + " (Kopie)", createdAt = System.currentTimeMillis()))
        db.checklists().insertTasks(db.checklists().tasksOf(c.id).map { it.copy(id = 0, checklistId = newId, done = false, completedAt = null) })
        notifyChanged()
        return newId
    }
    suspend fun reorderChecklists(orderedIds: List<Long>) {
        val all = db.checklists().all().associateBy { it.id }
        db.checklists().updateAll(orderedIds.mapIndexedNotNull { i, id -> all[id]?.copy(sortIndex = i) })
        notifyChanged()
    }

    suspend fun saveTask(t: TaskEntity): Long {
        val toSave = if (t.id == 0L) t.copy(position = (db.checklists().tasksOf(t.checklistId).maxOfOrNull { it.position } ?: 0) + 1) else t
        return db.checklists().upsertTask(toSave).also { notifyChanged() }
    }
    suspend fun deleteTask(id: Long) { db.checklists().deleteTask(id); notifyChanged() }

    /** Abhaken inkl. Wiederholungslogik. Gibt die aktualisierte Aufgabe zurück. */
    suspend fun toggleTask(id: Long): TaskEntity? {
        val t = db.checklists().getTask(id) ?: return null
        val updated = TaskLogic.toggle(t, LocalDateTime.now())
        db.checklists().upsertTask(updated)
        notifyChanged()
        return updated
    }

    suspend fun reorderTasks(orderedIds: List<Long>) {
        val all = db.checklists().allTasks().associateBy { it.id }
        db.checklists().updateTasks(orderedIds.mapIndexedNotNull { i, id -> all[id]?.copy(position = i) })
        notifyChanged()
    }

    suspend fun moveTask(taskId: Long, toChecklist: Long) {
        val t = db.checklists().getTask(taskId) ?: return
        val pos = (db.checklists().tasksOf(toChecklist).maxOfOrNull { it.position } ?: 0) + 1
        db.checklists().upsertTask(t.copy(checklistId = toChecklist, position = pos))
        notifyChanged()
    }

    suspend fun clearDone(checklistId: Long) {
        db.checklists().tasksOf(checklistId).filter { it.done }.forEach { db.checklists().deleteTask(it.id) }
        notifyChanged()
    }

    suspend fun resetChecklist(checklistId: Long) {
        db.checklists().updateTasks(db.checklists().tasksOf(checklistId).map { it.copy(done = false, completedAt = null) })
        notifyChanged()
    }

    // ---------- Notizen ----------
    suspend fun note(id: Long) = db.notes().get(id)
    suspend fun saveNote(n: NoteEntity): Long = db.notes().upsert(n).also { notifyChanged() }
    suspend fun deleteNote(id: Long) { db.notes().delete(id); notifyChanged() }
    suspend fun duplicateNote(n: NoteEntity): Long =
        saveNote(n.copy(id = 0, title = n.title + " (Kopie)", createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))

    // ---------- Gestaltung von Tagen/Monaten ----------
    suspend fun saveDecor(d: DecorEntity) { db.decor().upsert(d); notifyChanged() }
    suspend fun deleteDecor(key: String) { db.decor().delete(key); notifyChanged() }

    // ---------- Beispieldaten beim ersten Start ----------
    suspend fun seedIfNeeded() {
        val s = settings.current()
        if (s.seeded) return
        if (db.countdowns().all().isNotEmpty() || db.tags().all().isNotEmpty()) {
            settings.update { it.copy(seeded = true) }
            return
        }
        val tagIds = listOf(
            TagEntity(name = "Geburtstag", emoji = "🎂", color = 0xFFE63757.toInt()),
            TagEntity(name = "Familie", emoji = "❤️", color = 0xFFF7208F.toInt()),
            TagEntity(name = "Urlaub", emoji = "✈️", color = 0xFF1CB5E0.toInt()),
            TagEntity(name = "Schule", emoji = "📚", color = 0xFF5B1E91.toInt()),
            TagEntity(name = "Sport", emoji = "🏃", color = 0xFFFFA800.toInt()),
            TagEntity(name = "Veranstaltungen", emoji = "🎉", color = 0xFF12B05A.toInt()),
            TagEntity(name = "Wichtig", emoji = "⭐", color = 0xFFFFC107.toInt()),
            TagEntity(name = "Arbeit", emoji = "💼", color = 0xFF546E7A.toInt()),
        ).map { db.tags().upsert(it) }

        val now = LocalDateTime.now()
        val year = now.year
        fun next(month: Int, day: Int, h: Int = 0, m: Int = 0): String {
            var d = LocalDateTime.of(year, month, day, h, m)
            if (d.isBefore(now)) d = d.plusYears(1)
            return DateLogic.format(d)
        }
        val yearly = RepeatRule(RepeatRule.YEARLY)
        val samples = listOf(
            CountdownEntity(title = "Geburtstagsparty", emoji = "🎂", target = DateLogic.format(now.plusDays(12).withHour(18).withMinute(0)),
                tags = listOf(tagIds[0], tagIds[5]), style = CountdownStyle(color = 0xFFE63757.toInt())),
            CountdownEntity(title = "Workout", emoji = "🏋️", target = DateLogic.format(now.plusDays(1).withHour(7).withMinute(0)),
                repeat = RepeatRule(RepeatRule.WEEKLY), tags = listOf(tagIds[4]), style = CountdownStyle(color = 0xFFFFA800.toInt())),
            CountdownEntity(title = "Urlaub", emoji = "✈️", target = DateLogic.format(now.plusDays(40).withHour(8).withMinute(0)),
                endDate = DateLogic.format(now.plusDays(46).withHour(20).withMinute(0)),
                tags = listOf(tagIds[2]), style = CountdownStyle(color = 0xFF1CB5E0.toInt())),
            CountdownEntity(title = "Halloween", emoji = "🎃", target = next(10, 31), repeat = yearly,
                tags = listOf(tagIds[5]), style = CountdownStyle(color = 0xFF12B05A.toInt())),
            CountdownEntity(title = "Weihnachten", emoji = "🎅", target = next(12, 24, 18), repeat = yearly,
                tags = listOf(tagIds[1]), favorite = true,
                style = CountdownStyle(bgType = 1, color = 0xFFE53935.toInt(), color2 = 0xFFB71C1C.toInt())),
            CountdownEntity(title = "Silvester", emoji = "🎆", target = next(12, 31, 23, 59), repeat = yearly,
                style = CountdownStyle(bgType = 3, patternId = 3, color = 0xFF5B1E91.toInt(), color2 = 0xFF1A237E.toInt(), autoUnit = false,
                    units = TimeUnits.DAYS or TimeUnits.HOURS or TimeUnits.MINUTES)),
        )
        samples.forEachIndexed { i, c -> db.countdowns().upsert(c.copy(sortIndex = i, reminders = listOf(1440L, 0L))) }

        val listId = db.checklists().upsert(ChecklistEntity(title = "Einkaufsliste", emoji = "🛒", color = 0xFF12B05A.toInt(), sortIndex = 0))
        listOf("Milch", "Brot", "Äpfel", "Kaffee").forEachIndexed { i, s ->
            db.checklists().upsertTask(TaskEntity(checklistId = listId, text = s, position = i))
        }
        val routineId = db.checklists().upsert(ChecklistEntity(title = "Tagesroutine", emoji = "☀️", color = 0xFFFFA800.toInt(), sortIndex = 1))
        val today = LocalDate.now()
        listOf("Wasser trinken 💧" to 9, "10 Minuten lesen 📖" to 20, "Spaziergang 🚶" to 17).forEachIndexed { i, (s, h) ->
            db.checklists().upsertTask(TaskEntity(checklistId = routineId, text = s, position = i,
                due = DateLogic.format(today.atTime(h, 0)), repeat = RepeatRule(RepeatRule.DAILY), priority = 2))
        }
        db.notes().upsert(NoteEntity(title = "Willkommen bei Planora 👋",
            content = "# Schön, dass du da bist!\nHier kannst du **fett**, *kursiv* und [c=#E63757]farbig[/c] schreiben.\n- Aufzählungen\n- mit Emojis 🎉\n1. und Nummerierungen\n[s=24]Große Schrift[/s]",
            color = 0xFFFFF3C4.toInt(), pinned = true, tags = listOf(tagIds[6])))
        db.events().upsert(EventEntity(title = "Team-Meeting", emoji = "💼", color = 0xFF546E7A.toInt(),
            start = DateLogic.format(today.plusDays(1).atTime(10, 0)), end = DateLogic.format(today.plusDays(1).atTime(11, 0)),
            reminders = listOf(15), tags = listOf(tagIds[7]), repeat = RepeatRule(RepeatRule.WEEKLY)))
        settings.update { it.copy(seeded = true) }
        notifyChanged()
    }
}
