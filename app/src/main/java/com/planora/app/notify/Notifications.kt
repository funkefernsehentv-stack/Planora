package com.planora.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.planora.app.MainActivity
import com.planora.app.R
import com.planora.app.data.AppSettings
import com.planora.app.data.BackupManager
import com.planora.app.data.Graph
import com.planora.app.domain.CountdownLogic
import com.planora.app.domain.DateLogic
import com.planora.app.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val QUIET_CHANNEL = "quiet_v1"
    private const val PREFIX = "rem_"

    fun channelId(s: AppSettings): String =
        PREFIX + (s.soundUri?.hashCode()?.toUInt()?.toString() ?: "default") + if (s.vibration) "_v" else "_n"

    /**
     * Android-Kanäle sind nach dem Anlegen unveränderlich. Daher wird für jede Kombination aus Ton
     * und Vibration ein eigener Kanal angelegt und veraltete Kanäle werden entfernt.
     */
    fun ensureChannels(context: Context, s: AppSettings) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val id = channelId(s)
        if (nm.getNotificationChannel(id) == null) {
            val ch = NotificationChannel(id, "Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Termine, Countdowns und Aufgaben"
                enableVibration(s.vibration)
                if (s.vibration) vibrationPattern = longArrayOf(0, 250, 150, 250)
                when {
                    s.soundUri == "" -> setSound(null, null)
                    s.soundUri != null -> setSound(
                        Uri.parse(s.soundUri),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                }
            }
            nm.createNotificationChannel(ch)
        }
        nm.notificationChannels.filter { it.id.startsWith(PREFIX) && it.id != id }.forEach { nm.deleteNotificationChannel(it.id) }
        if (nm.getNotificationChannel(QUIET_CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(QUIET_CHANNEL, "Erinnerungen (Ruhezeit)", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Lautlose Erinnerungen während der Ruhezeiten"
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
    }

    fun inQuietHours(s: AppSettings, now: LocalTime = LocalTime.now()): Boolean {
        if (!s.quietEnabled) return false
        val m = now.hour * 60 + now.minute
        return if (s.quietStart <= s.quietEnd) m in s.quietStart until s.quietEnd
        else m >= s.quietStart || m < s.quietEnd
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openIntent(context: Context, type: String, id: Long, requestCode: Int): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TYPE, type)
            putExtra(MainActivity.EXTRA_OPEN_ID, id)
        }
        return PendingIntent.getActivity(context, requestCode, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun show(
        context: Context,
        s: AppSettings,
        notificationId: Int,
        title: String,
        text: String,
        openType: String,
        openId: Long,
        color: Int? = null,
    ) {
        if (!canPost(context)) return
        ensureChannels(context, s)
        val quiet = inQuietHours(s)
        val channel = if (quiet) QUIET_CHANNEL else channelId(s)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (s.showPreview) title else "Planora-Erinnerung")
            .setContentText(if (s.showPreview) text else "Tippe zum Öffnen")
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (s.showPreview) text else "Tippe zum Öffnen"))
            .setAutoCancel(true)
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent(context, openType, openId, notificationId))
            .setVisibility(if (s.showPreview) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
        color?.let { builder.setColor(it) }
        if (!s.showPreview) {
            builder.setPublicVersion(
                NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Planora-Erinnerung").build(),
            )
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }
}

/** Plant genau einen Alarm pro Eintrag (den jeweils nächsten). Deterministische IDs verhindern Duplikate. */
object ReminderScheduler {
    const val TYPE_EVENT = 1
    const val TYPE_COUNTDOWN = 2
    const val TYPE_TASK = 3
    const val TYPE_SUMMARY = 4

    const val EXTRA_TYPE = "type"
    const val EXTRA_ID = "id"
    const val EXTRA_TRIGGER = "trigger"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_COLOR = "color"

    data class Planned(val type: Int, val id: Long, val at: LocalDateTime, val title: String, val text: String, val color: Int?)

    fun requestCode(type: Int, id: Long): Int = (type * 10_000_000L + (id % 10_000_000L)).toInt()

    private fun earliest(occurrences: List<LocalDateTime>, offsets: List<Long>, now: LocalDateTime): Pair<LocalDateTime, Long>? {
        var best: Pair<LocalDateTime, Long>? = null
        for (o in occurrences) for (m in offsets) {
            val t = o.minusMinutes(m)
            if (t.isAfter(now) && (best == null || t.isBefore(best.first))) best = t to m
        }
        return best
    }

    private fun offsetText(m: Long, prefixNow: String): String = when {
        m == 0L -> prefixNow
        m % 1440 == 0L -> (m / 1440).let { if (it == 1L) "Morgen" else "In $it Tagen" }
        m % 60 == 0L -> (m / 60).let { if (it == 1L) "In 1 Stunde" else "In $it Stunden" }
        else -> "In $m Minuten"
    }

    suspend fun computeAll(now: LocalDateTime): List<Planned> {
        val s = Graph.settings.current()
        if (!s.notificationsEnabled) return emptyList()
        val db = Graph.db
        val out = ArrayList<Planned>()
        if (s.notifyEvents) {
            for (e in db.events().all()) {
                if (e.reminders.isEmpty()) continue
                val start = DateLogic.parse(e.start) ?: continue
                val maxOff = e.reminders.max()
                val occ = DateLogic.occurrencesBetween(start, java.time.Duration.ZERO, e.repeat, now, now.plusMinutes(maxOff).plusYears(2), max = 30)
                val (at, m) = earliest(occ, e.reminders, now) ?: continue
                val occStart = at.plusMinutes(m)
                val time = if (e.allDay) "ganztägig" else DateLogic.formatTime(occStart)
                out += Planned(TYPE_EVENT, e.id, at, "${e.emoji} ${e.title}",
                    "${offsetText(m, "Jetzt")} · ${DateLogic.formatDay(occStart.toLocalDate())}, $time", e.color)
            }
        }
        if (s.notifyCountdowns) {
            for (c in db.countdowns().all()) {
                if (c.reminders.isEmpty() || c.mode == CountdownLogic.MODE_UP) continue
                val start = DateLogic.parse(c.target) ?: continue
                val maxOff = c.reminders.max()
                val occ = DateLogic.occurrencesBetween(start, java.time.Duration.ZERO, c.repeat, now, now.plusMinutes(maxOff).plusYears(2), max = 10)
                val (at, m) = earliest(occ, c.reminders, now) ?: continue
                val text = when {
                    m == 0L -> "Heute ist es so weit! 🎉"
                    m % 1440 == 0L -> (m / 1440).let { if (it == 1L) "Nur noch 1 Tag!" else "Noch $it Tage" }
                    else -> offsetText(m, "Jetzt")
                }
                out += Planned(TYPE_COUNTDOWN, c.id, at, "${c.emoji} ${c.title}", text, c.style.color)
            }
        }
        if (s.notifyTasks) {
            val lists = db.checklists().all().associateBy { it.id }
            for (t in db.checklists().allTasks()) {
                if (t.done || !t.reminder) continue
                val due = DateLogic.parse(t.due) ?: continue
                if (!due.isAfter(now)) continue
                val list = lists[t.checklistId]
                out += Planned(TYPE_TASK, t.id, due, "✅ ${t.text}",
                    "Fällig · ${list?.let { "${it.emoji} ${it.title}" } ?: ""}", list?.color)
            }
            if (s.dailyTaskSummary) {
                var at = now.toLocalDate().atTime(s.dailyTaskSummaryTime / 60, s.dailyTaskSummaryTime % 60)
                if (!at.isAfter(now)) at = at.plusDays(1)
                out += Planned(TYPE_SUMMARY, 0, at, "Offene Aufgaben", "", null)
            }
        }
        return out
    }

    suspend fun rescheduleAll(context: Context) {
        Graph.init(context)
        val s = Graph.settings.current()
        NotificationHelper.ensureChannels(context, s)
        val planned = computeAll(LocalDateTime.now())
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val old = prefs.getStringSet("scheduled", emptySet()) ?: emptySet()
        val newKeys = planned.map { "${it.type}:${it.id}" }.toSet()
        for (key in old - newKeys) {
            val (t, id) = key.split(":").let { it[0].toInt() to it[1].toLong() }
            cancel(context, t, id)
        }
        planned.forEach { schedule(context, it) }
        prefs.edit().putStringSet("scheduled", newKeys).apply()
    }

    private fun pendingIntent(context: Context, p: Planned?, type: Int, id: Long, flags: Int): PendingIntent? {
        val i = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.planora.app.REMINDER.$type.$id"
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ID, id)
            if (p != null) {
                putExtra(EXTRA_TRIGGER, DateLogic.toEpochMillis(p.at))
                putExtra(EXTRA_TITLE, p.title)
                putExtra(EXTRA_TEXT, p.text)
                p.color?.let { putExtra(EXTRA_COLOR, it) }
            }
        }
        return PendingIntent.getBroadcast(context, requestCode(type, id), i, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun schedule(context: Context, p: Planned) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, p, p.type, p.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val ms = DateLogic.toEpochMillis(p.at)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }

    private fun cancel(context: Context, type: Int, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        pendingIntent(context, null, type, id, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Graph.init(context)
        Graph.appScope.launch {
            try {
                handle(context, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val type = intent.getIntExtra(ReminderScheduler.EXTRA_TYPE, 0)
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0)
        val trigger = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER, 0)
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val key = "fired_${type}_$id"
        // Doppelte Benachrichtigungen vermeiden
        if (prefs.getLong(key, -1) == trigger) {
            ReminderScheduler.rescheduleAll(context)
            return
        }
        prefs.edit().putLong(key, trigger).apply()
        val s = Graph.settings.current()
        val nid = ReminderScheduler.requestCode(type, id)
        if (s.notificationsEnabled) {
            when (type) {
                ReminderScheduler.TYPE_SUMMARY -> {
                    val now = LocalDateTime.now()
                    val endOfDay = now.toLocalDate().atTime(23, 59)
                    val open = Graph.db.checklists().allTasks().filter { t ->
                        !t.done && (DateLogic.parse(t.due)?.let { !it.isAfter(endOfDay) } ?: false)
                    }
                    if (open.isNotEmpty()) {
                        NotificationHelper.show(context, s, nid, "📋 ${open.size} offene Aufgaben heute",
                            open.take(6).joinToString("\n") { "• ${it.text}" }, "checklists", 0)
                    }
                }
                else -> {
                    val openType = when (type) {
                        ReminderScheduler.TYPE_EVENT -> "event"
                        ReminderScheduler.TYPE_COUNTDOWN -> "countdown"
                        else -> "task"
                    }
                    val color = if (intent.hasExtra(ReminderScheduler.EXTRA_COLOR)) intent.getIntExtra(ReminderScheduler.EXTRA_COLOR, 0) else null
                    NotificationHelper.show(context, s, nid,
                        intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Erinnerung",
                        intent.getStringExtra(ReminderScheduler.EXTRA_TEXT) ?: "", openType, id, color)
                }
            }
        }
        ReminderScheduler.rescheduleAll(context)
        WidgetUpdater.updateAll(context)
    }
}

/** Stellt Erinnerungen und Widgets nach Neustart, Update oder Zeitänderung wieder her. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Graph.init(context)
        Graph.appScope.launch {
            try {
                ReminderScheduler.rescheduleAll(context)
                WidgetUpdater.updateAll(context)
                MaintenanceWorker.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Regelmäßige Wartung: Widgets aktualisieren, Erinnerungen absichern, automatische Sicherung. */
class MaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Graph.init(applicationContext)
        runCatching { WidgetUpdater.updateAll(applicationContext) }
        runCatching { ReminderScheduler.rescheduleAll(applicationContext) }
        val s = Graph.settings.current()
        if (s.autoBackup && System.currentTimeMillis() - s.lastAutoBackup > TimeUnit.HOURS.toMillis(24)) {
            runCatching { BackupManager.autoBackup(applicationContext) }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<MaintenanceWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("planora_maintenance", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
