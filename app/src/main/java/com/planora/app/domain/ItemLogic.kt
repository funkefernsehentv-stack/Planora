package com.planora.app.domain

import com.planora.app.data.CountdownEntity
import com.planora.app.data.EventEntity
import com.planora.app.data.TagEntity
import com.planora.app.data.TaskEntity
import com.planora.app.data.TimeUnits
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Ergebnis der Countdown-Berechnung zu einem Zeitpunkt. */
data class CountdownState(
    /** maßgeblicher Zeitpunkt (nächstes Vorkommen bzw. Startpunkt beim Hochzählen) */
    val target: LocalDateTime,
    /** true, wenn hochgezählt wird (Zeitpunkt liegt in der Vergangenheit) */
    val countingUp: Boolean,
    /** Ereignis findet heute statt (bzw. hat heute bereits begonnen) */
    val isToday: Boolean,
    /** einmaliger Countdown, der vorbei ist */
    val expired: Boolean,
    /** 0 = heute, 1 = bevorstehend, 2 = hochzählend, 3 = abgelaufen */
    val group: Int,
)

object CountdownLogic {
    const val MODE_DOWN = 0
    const val MODE_UP = 1

    fun state(c: CountdownEntity, now: LocalDateTime): CountdownState {
        val base = DateLogic.parse(c.target) ?: now
        if (c.mode == MODE_UP) {
            return if (base.isAfter(now)) {
                CountdownState(base, countingUp = false, isToday = false, expired = false, group = 1)
            } else {
                CountdownState(base, countingUp = true, isToday = base.toLocalDate() == now.toLocalDate(), expired = false, group = 2)
            }
        }
        val prev = DateLogic.previousOccurrence(base, c.repeat, now)
        val end = DateLogic.parse(c.endDate)
        val spanEnd = if (prev != null && end != null && end.isAfter(base)) prev.plus(Duration.between(base, end)) else null
        // „Heute“: das letzte Vorkommen war heute oder ein mehrtägiges Ereignis läuft gerade
        val today = prev != null && (prev.toLocalDate() == now.toLocalDate() ||
            (spanEnd != null && !now.toLocalDate().isAfter(spanEnd.toLocalDate())))
        val next = DateLogic.nextOccurrence(base, c.repeat, now)
        return when {
            today -> CountdownState(next ?: prev!!, countingUp = false, isToday = true, expired = false, group = 0)
            next != null -> CountdownState(next, countingUp = false, isToday = false, expired = false, group = 1)
            else -> CountdownState(prev ?: base, countingUp = true, isToday = false, expired = true, group = 3)
        }
    }

    /** Anzuzeigende Werte für die Karte bzw. Detailansicht. */
    fun displayValues(c: CountdownEntity, s: CountdownState, now: LocalDateTime, forceAll: Boolean = false): List<Pair<Int, Long>> {
        val st = c.style
        return if (st.autoUnit && !forceAll) listOf(DateLogic.autoUnit(now, s.target))
        else DateLogic.breakdown(now, s.target, if (st.autoUnit) TimeUnits.DAYS or TimeUnits.HOURS or TimeUnits.MINUTES or TimeUnits.SECONDS else st.units)
    }

    /** Erste nicht-null Einheit (für die große Zahl auf der Karte). */
    fun primary(values: List<Pair<Int, Long>>): Pair<Int, Long> =
        values.firstOrNull { it.second > 0 } ?: values.lastOrNull() ?: (TimeUnits.DAYS to 0L)

    fun suffix(s: CountdownState): String = when {
        s.isToday -> "heute"
        s.expired -> "vorbei"
        s.countingUp -> "seit"
        else -> "noch"
    }

    /**
     * Sortierung. Standard (0) = nächste Countdowns immer oben:
     * angeheftete zuerst, dann „heute“, dann nach nächstem Termin aufsteigend,
     * dann Hochzähler, zuletzt abgelaufene.
     */
    fun sort(list: List<CountdownEntity>, mode: Int, now: LocalDateTime, tags: List<TagEntity>): List<CountdownEntity> {
        val states = list.associate { it.id to state(it, now) }
        val tagNames = tags.associate { it.id to it.name.lowercase() }
        val byNext = compareBy<CountdownEntity>({ states[it.id]!!.group }, {
            val s = states[it.id]!!
            when (s.group) {
                2, 3 -> -DateLogic.toEpochSecondsLocal(s.target) // jüngste zuerst
                else -> DateLogic.toEpochSecondsLocal(s.target)
            }
        })
        val comparator: Comparator<CountdownEntity> = when (mode) {
            1 -> compareByDescending { it.createdAt }
            2 -> compareBy { it.title.lowercase() }
            3 -> compareBy { it.sortIndex }
            4 -> compareBy<CountdownEntity> { c -> c.tags.mapNotNull { tagNames[it] }.minOrNull() ?: "￿" }.then(byNext)
            else -> byNext
        }
        return list.sortedWith(compareByDescending<CountdownEntity> { it.pinned }.then(comparator))
    }
}

object EventLogic {
    fun duration(e: EventEntity): Duration {
        val s = DateLogic.parse(e.start) ?: return Duration.ZERO
        val en = DateLogic.parse(e.end) ?: return Duration.ZERO
        return if (en.isAfter(s)) Duration.between(s, en) else Duration.ZERO
    }

    data class Occurrence(val event: EventEntity, val start: LocalDateTime, val end: LocalDateTime)

    fun occurrences(events: List<EventEntity>, from: LocalDate, to: LocalDate): List<Occurrence> {
        val f = from.atStartOfDay()
        val t = to.atTime(23, 59, 59)
        val out = ArrayList<Occurrence>()
        for (e in events) {
            val s = DateLogic.parse(e.start) ?: continue
            val d = duration(e)
            for (o in DateLogic.occurrencesBetween(s, d, e.repeat, f, t)) out += Occurrence(e, o, o.plus(d))
        }
        out.sortWith(compareBy({ !it.event.allDay }, { it.start }))
        return out
    }

    /** Ordnet Vorkommen den Tagen zu (mehrtägige Termine erscheinen an jedem Tag). */
    fun byDay(occ: List<Occurrence>, from: LocalDate, to: LocalDate): Map<LocalDate, List<Occurrence>> {
        val map = HashMap<LocalDate, MutableList<Occurrence>>()
        for (o in occ) {
            var d = maxOf(o.start.toLocalDate(), from)
            // Endzeit genau um Mitternacht zählt nicht zum Folgetag
            val endDay = if (o.end.isAfter(o.start) && o.end.toLocalTime() == java.time.LocalTime.MIDNIGHT)
                o.end.toLocalDate().minusDays(1) else o.end.toLocalDate()
            val last = minOf(maxOf(endDay, o.start.toLocalDate()), to)
            while (!d.isAfter(last)) {
                map.getOrPut(d) { mutableListOf() } += o
                d = d.plusDays(1)
            }
        }
        return map
    }
}

object TaskLogic {
    /**
     * Abhaken einer Aufgabe. Wiederkehrende Aufgaben werden nicht dauerhaft erledigt,
     * sondern erscheinen mit dem nächsten Fälligkeitsdatum erneut.
     */
    fun toggle(t: TaskEntity, now: LocalDateTime): TaskEntity {
        if (t.done) return t.copy(done = false, completedAt = null)
        if (t.repeat.isRepeating) {
            val base = DateLogic.parse(t.due) ?: now.toLocalDate().atStartOfDay()
            // nächstes Vorkommen nach dem aktuellen Fälligkeitsdatum und mindestens nach heute
            val after = if (base.isAfter(now)) base else now
            val next = DateLogic.nextOccurrence(base, t.repeat, after, strict = true)
            if (next != null) {
                return t.copy(done = false, due = DateLogic.format(next), completedAt = System.currentTimeMillis())
            }
        }
        return t.copy(done = true, completedAt = System.currentTimeMillis())
    }

    fun isOverdue(t: TaskEntity, now: LocalDateTime): Boolean =
        !t.done && (DateLogic.parse(t.due)?.isBefore(now) ?: false)

    fun sort(tasks: List<TaskEntity>, mode: Int): List<TaskEntity> = when (mode) {
        1 -> tasks.sortedWith(compareBy<TaskEntity>({ it.done }, { -it.priority }, { it.position }))
        2 -> tasks.sortedWith(compareBy<TaskEntity>({ it.done }, { it.due == null }, { it.due ?: "" }, { it.position }))
        3 -> tasks.sortedWith(compareBy<TaskEntity>({ it.done }, { it.text.lowercase() }))
        else -> tasks.sortedWith(compareBy<TaskEntity>({ it.done }, { it.position }))
    }
}
