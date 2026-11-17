package com.planora.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🏷️",
    val color: Int = Defaults.vivid[0],
    val description: String = "",
)

@Serializable
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val emoji: String = "📅",
    val color: Int = Defaults.vivid[3],
    /** ISO LocalDateTime, z. B. 2026-11-17T14:00 */
    val start: String,
    val end: String,
    val allDay: Boolean = false,
    val category: String = "",
    val tags: List<Long> = emptyList(),
    /** Erinnerungen in Minuten vor Beginn */
    val reminders: List<Long> = emptyList(),
    val repeat: RepeatRule = RepeatRule(),
    val favorite: Boolean = false,
    val style: EventStyle = EventStyle(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val emoji: String = "🎉",
    /** Name eines vorgefertigten Symbols (optional, ersetzt das Emoji) */
    val icon: String? = null,
    /** eigenes Vorschaubild (Dateiname im internen Bildordner) */
    val thumbnail: String? = null,
    val target: String,
    /** optionales Ende, z. B. für Urlaube (Anzeige „30. Jul – 4. Aug“) */
    val endDate: String? = null,
    /** 0 = Countdown, 1 = Hochzählen seit */
    val mode: Int = 0,
    val repeat: RepeatRule = RepeatRule(),
    val category: String = "",
    val tags: List<Long> = emptyList(),
    /** Erinnerungen in Minuten vor dem Ereignis */
    val reminders: List<Long> = emptyList(),
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val style: CountdownStyle = CountdownStyle(),
)

@Serializable
@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val color: Int = Defaults.vivid[5],
    val textColor: Int = 0xFFFFFFFF.toInt(),
    /** 0 = gefüllt, 1 = Umriss, 2 = Verlauf, 3 = Papier */
    val design: Int = 0,
    val tags: List<Long> = emptyList(),
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val sortIndex: Int = 0,
    /** 0 = manuell, 1 = Priorität, 2 = Fälligkeit, 3 = alphabetisch */
    val sortMode: Int = 0,
    val hideDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "tasks", indices = [Index("checklistId")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checklistId: Long,
    val text: String,
    val done: Boolean = false,
    /** 0 = keine, 1 = niedrig, 2 = mittel, 3 = hoch */
    val priority: Int = 0,
    /** ISO LocalDateTime oder null */
    val due: String? = null,
    val reminder: Boolean = false,
    val repeat: RepeatRule = RepeatRule(),
    val tags: List<Long> = emptyList(),
    val position: Int = 0,
    val note: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Inhalt in Planora-Markup (siehe ui/screens/Markup.kt) */
    val content: String = "",
    val color: Int? = null,
    val textColor: Int? = null,
    val imageName: String? = null,
    /** vorgefertigtes Design, -1 = keins */
    val design: Int = -1,
    val category: String = "",
    val tags: List<Long> = emptyList(),
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Gestaltung einzelner Kalendertage oder Monate.
 * key: "d:2026-11-17" für einen Tag, "m:2026-11" für einen Monat.
 */
@Serializable
@Entity(tableName = "decor")
data class DecorEntity(
    @PrimaryKey val key: String,
    val color: Int? = null,
    val imageName: String? = null,
    val emoji: String? = null,
    val alpha: Float = 0.5f,
)
