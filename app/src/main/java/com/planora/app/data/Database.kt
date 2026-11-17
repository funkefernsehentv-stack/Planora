package com.planora.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

class Converters {
    @TypeConverter
    fun longListToString(list: List<Long>): String = list.joinToString(",")

    @TypeConverter
    fun stringToLongList(s: String): List<Long> =
        if (s.isBlank()) emptyList() else s.split(",").mapNotNull { it.trim().toLongOrNull() }

    @TypeConverter
    fun repeatToString(r: RepeatRule): String = AppJson.encodeToString(r)

    @TypeConverter
    fun stringToRepeat(s: String): RepeatRule =
        runCatching { AppJson.decodeFromString<RepeatRule>(s) }.getOrDefault(RepeatRule())

    @TypeConverter
    fun countdownStyleToString(r: CountdownStyle): String = AppJson.encodeToString(r)

    @TypeConverter
    fun stringToCountdownStyle(s: String): CountdownStyle =
        runCatching { AppJson.decodeFromString<CountdownStyle>(s) }.getOrDefault(CountdownStyle())

    @TypeConverter
    fun eventStyleToString(r: EventStyle): String = AppJson.encodeToString(r)

    @TypeConverter
    fun stringToEventStyle(s: String): EventStyle =
        runCatching { AppJson.decodeFromString<EventStyle>(s) }.getOrDefault(EventStyle())
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observe(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun all(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TagEntity>)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags")
    suspend fun clear()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY start")
    fun observe(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun all(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM events")
    suspend fun clear()
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns")
    fun observe(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns")
    suspend fun all(): List<CountdownEntity>

    @Query("SELECT * FROM countdowns WHERE id = :id")
    suspend fun get(id: Long): CountdownEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: CountdownEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CountdownEntity>)

    @Update
    suspend fun updateAll(list: List<CountdownEntity>)

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM countdowns")
    suspend fun clear()
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists")
    fun observe(): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklists")
    suspend fun all(): List<ChecklistEntity>

    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun get(id: Long): ChecklistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ChecklistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ChecklistEntity>)

    @Update
    suspend fun updateAll(list: List<ChecklistEntity>)

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM checklists")
    suspend fun clear()

    @Query("SELECT * FROM tasks ORDER BY position")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun allTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE checklistId = :checklistId ORDER BY position")
    suspend fun tasksOf(checklistId: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(t: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(list: List<TaskEntity>)

    @Update
    suspend fun updateTasks(list: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE checklistId = :checklistId")
    suspend fun deleteTasksOf(checklistId: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun observe(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun all(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface DecorDao {
    @Query("SELECT * FROM decor")
    fun observe(): Flow<List<DecorEntity>>

    @Query("SELECT * FROM decor")
    suspend fun all(): List<DecorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: DecorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<DecorEntity>)

    @Query("DELETE FROM decor WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM decor")
    suspend fun clear()
}

@Database(
    entities = [
        TagEntity::class, EventEntity::class, CountdownEntity::class,
        ChecklistEntity::class, TaskEntity::class, NoteEntity::class, DecorEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tags(): TagDao
    abstract fun events(): EventDao
    abstract fun countdowns(): CountdownDao
    abstract fun checklists(): ChecklistDao
    abstract fun notes(): NoteDao
    abstract fun decor(): DecorDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "planora.db")
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { instance = it }
        }
    }
}
