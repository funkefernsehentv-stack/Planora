package com.planora.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.data.AppSettings
import com.planora.app.data.BackupManager
import com.planora.app.data.ChecklistEntity
import com.planora.app.data.CountdownEntity
import com.planora.app.data.DecorEntity
import com.planora.app.data.EventEntity
import com.planora.app.data.Graph
import com.planora.app.data.ImageStore
import com.planora.app.data.NoteEntity
import com.planora.app.data.TagEntity
import com.planora.app.data.TaskEntity
import com.planora.app.notify.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppViewModel(app: Application) : AndroidViewModel(app) {
    init {
        Graph.init(app)
    }

    private val repo = Graph.repo

    private fun <T> StateFlowOf(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val settings: StateFlow<AppSettings> = StateFlowOf(Graph.settings.flow, AppSettings())
    val tags: StateFlow<List<TagEntity>> = StateFlowOf(repo.tags, emptyList())
    val events: StateFlow<List<EventEntity>> = StateFlowOf(repo.events, emptyList())
    val countdowns: StateFlow<List<CountdownEntity>> = StateFlowOf(repo.countdowns, emptyList())
    val checklists: StateFlow<List<ChecklistEntity>> = StateFlowOf(repo.checklists, emptyList())
    val tasks: StateFlow<List<TaskEntity>> = StateFlowOf(repo.tasks, emptyList())
    val notes: StateFlow<List<NoteEntity>> = StateFlowOf(repo.notes, emptyList())
    val decor: StateFlow<List<DecorEntity>> = StateFlowOf(repo.decor, emptyList())

    init {
        viewModelScope.launch { repo.seedIfNeeded() }
    }

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    // Einstellungen
    fun updateSettings(transform: (AppSettings) -> AppSettings) = io {
        val before = Graph.settings.current()
        Graph.settings.update(transform)
        val after = Graph.settings.current()
        if (before.copy(lastAutoBackup = 0) != after.copy(lastAutoBackup = 0)) repo.notifyChanged()
    }

    // Tags
    fun saveTag(t: TagEntity, onSaved: (Long) -> Unit = {}) = io {
        val id = repo.saveTag(t)
        withContext(Dispatchers.Main) { onSaved(if (t.id == 0L) id else t.id) }
    }
    fun deleteTag(t: TagEntity) = io { repo.deleteTag(t) }

    // Termine
    fun saveEvent(e: EventEntity) = io { repo.saveEvent(e) }
    fun deleteEvent(id: Long) = io { repo.deleteEvent(id) }
    fun duplicateEvent(e: EventEntity) = io { repo.duplicateEvent(e) }
    fun saveDecor(d: DecorEntity) = io { repo.saveDecor(d) }
    fun deleteDecor(key: String) = io { repo.deleteDecor(key) }

    // Countdowns
    fun saveCountdown(c: CountdownEntity) = io { repo.saveCountdown(c) }
    fun deleteCountdown(id: Long) = io { repo.deleteCountdown(id) }
    fun duplicateCountdown(c: CountdownEntity) = io { repo.duplicateCountdown(c) }
    fun reorderCountdowns(ids: List<Long>) = io { repo.reorderCountdowns(ids) }

    // Checklisten
    fun saveChecklist(c: ChecklistEntity, onSaved: (Long) -> Unit = {}) = io {
        val id = repo.saveChecklist(c)
        withContext(Dispatchers.Main) { onSaved(if (c.id == 0L) id else c.id) }
    }
    fun deleteChecklist(id: Long) = io { repo.deleteChecklist(id) }
    fun duplicateChecklist(c: ChecklistEntity) = io { repo.duplicateChecklist(c) }
    fun reorderChecklists(ids: List<Long>) = io { repo.reorderChecklists(ids) }
    fun saveTask(t: TaskEntity) = io { repo.saveTask(t) }
    fun deleteTask(id: Long) = io { repo.deleteTask(id) }
    fun toggleTask(id: Long, onResult: (TaskEntity?) -> Unit = {}) = io {
        val r = repo.toggleTask(id)
        withContext(Dispatchers.Main) { onResult(r) }
    }
    fun reorderTasks(ids: List<Long>) = io { repo.reorderTasks(ids) }
    fun moveTask(taskId: Long, to: Long) = io { repo.moveTask(taskId, to) }
    fun clearDone(listId: Long) = io { repo.clearDone(listId) }
    fun resetChecklist(listId: Long) = io { repo.resetChecklist(listId) }

    // Notizen
    fun saveNote(n: NoteEntity, onSaved: (Long) -> Unit = {}) = io {
        val id = repo.saveNote(n)
        withContext(Dispatchers.Main) { onSaved(if (n.id == 0L) id else n.id) }
    }
    fun deleteNote(id: Long) = io { repo.deleteNote(id) }
    fun duplicateNote(n: NoteEntity) = io { repo.duplicateNote(n) }

    // Bilder
    fun importImage(uri: Uri, onDone: (String?) -> Unit) = viewModelScope.launch {
        val name = withContext(Dispatchers.IO) { ImageStore.import(getApplication(), uri) }
        onDone(name)
    }

    // Sicherung
    fun export(uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching { BackupManager.exportTo(getApplication(), uri) }.fold({ "Export erfolgreich ✅" }, { "Export fehlgeschlagen: ${it.message}" })
        }
        onDone(msg)
    }

    fun import(uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching { BackupManager.importFrom(getApplication(), uri) }.fold({ "Daten wiederhergestellt ✅" }, { "Import fehlgeschlagen: ${it.message}" })
        }
        onDone(msg)
    }

    fun backupNow(onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching { BackupManager.autoBackup(getApplication()) }.fold({ "Lokale Sicherung erstellt ✅" }, { "Fehler: ${it.message}" })
        }
        onDone(msg)
    }

    fun localBackups(): List<File> = BackupManager.localBackups(getApplication())

    fun restoreLocal(f: File, onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching { BackupManager.restoreLocal(getApplication(), f) }.fold({ "Sicherung wiederhergestellt ✅" }, { "Fehler: ${it.message}" })
        }
        onDone(msg)
    }

    fun rescheduleReminders() = io { ReminderScheduler.rescheduleAll(getApplication()) }
}
