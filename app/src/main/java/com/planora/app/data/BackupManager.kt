package com.planora.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupData(
    val format: String = "planora-backup",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val tags: List<TagEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val countdowns: List<CountdownEntity> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val decor: List<DecorEntity> = emptyList(),
    val settings: AppSettings? = null,
    /** Dateiname -> Base64 (nur beim vollständigen Export) */
    val images: Map<String, String> = emptyMap(),
)

object BackupManager {

    suspend fun createBackup(context: Context, includeImages: Boolean): BackupData {
        val db = Graph.db
        val data = BackupData(
            tags = db.tags().all(),
            events = db.events().all(),
            countdowns = db.countdowns().all(),
            checklists = db.checklists().all(),
            tasks = db.checklists().allTasks(),
            notes = db.notes().all(),
            decor = db.decor().all(),
            settings = Graph.settings.current(),
        )
        if (!includeImages) return data
        val names = buildSet {
            data.countdowns.forEach { c -> c.style.imageName?.let { add(it) }; c.thumbnail?.let { add(it) } }
            data.events.forEach { e -> e.style.imageName?.let { add(it) } }
            data.notes.forEach { n -> n.imageName?.let { add(it) } }
            data.decor.forEach { d -> d.imageName?.let { add(it) } }
            data.settings?.appBackground?.let { add(it) }
        }
        val images = names.mapNotNull { n ->
            ImageStore.file(context, n)?.let { f -> n to Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) }
        }.toMap()
        return data.copy(images = images)
    }

    suspend fun exportTo(context: Context, uri: Uri) {
        val json = AppJson.encodeToString(createBackup(context, includeImages = true))
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
            ?: error("Datei konnte nicht geschrieben werden")
    }

    suspend fun importFrom(context: Context, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Datei konnte nicht gelesen werden")
        restore(context, text)
    }

    suspend fun restore(context: Context, json: String) {
        val data = AppJson.decodeFromString<BackupData>(json)
        require(data.format == "planora-backup") { "Keine Planora-Sicherung" }
        data.images.forEach { (name, b64) ->
            runCatching { File(ImageStore.dir(context), name).writeBytes(Base64.decode(b64, Base64.NO_WRAP)) }
        }
        val db = Graph.db
        db.withTransaction {
            db.tags().clear(); db.events().clear(); db.countdowns().clear()
            db.checklists().clearTasks(); db.checklists().clear(); db.notes().clear(); db.decor().clear()
            db.tags().insertAll(data.tags)
            db.events().insertAll(data.events)
            db.countdowns().insertAll(data.countdowns)
            db.checklists().insertAll(data.checklists)
            db.checklists().insertTasks(data.tasks)
            db.notes().insertAll(data.notes)
            db.decor().insertAll(data.decor)
        }
        data.settings?.let { s -> Graph.settings.replace(s.copy(seeded = true)) }
        Graph.repo.notifyChanged()
    }

    // ---------- Automatische lokale Sicherung ----------
    fun backupDir(context: Context) = File(context.filesDir, "backups").apply { mkdirs() }

    suspend fun autoBackup(context: Context) {
        val json = AppJson.encodeToString(createBackup(context, includeImages = false))
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.GERMANY).format(Date())
        File(backupDir(context), "auto_$stamp.json").writeText(json)
        localBackups(context).drop(7).forEach { it.delete() }
        Graph.settings.update { it.copy(lastAutoBackup = System.currentTimeMillis()) }
    }

    fun localBackups(context: Context): List<File> =
        backupDir(context).listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()

    suspend fun restoreLocal(context: Context, file: File) = restore(context, file.readText())
}
