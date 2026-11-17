package com.planora.app.domain

import com.planora.app.data.RepeatRule
import com.planora.app.data.TimeUnits
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Zentrale, rein funktionale Datumslogik (ohne Android-Abhängigkeiten, daher per Unit-Test prüfbar).
 *
 * Wiederholungen werden immer vom ursprünglichen Startzeitpunkt aus berechnet
 * (start + k * Intervall) und nicht schrittweise, damit z. B. ein Termin am 31. nicht
 * nach einem Februar dauerhaft auf den 28. „rutscht“. Monats- und Jahresarithmetik
 * von java.time klemmt ungültige Tage korrekt (31. Jan + 1 Monat = 28./29. Feb,
 * 29. Feb + 1 Jahr = 28. Feb im Nicht-Schaltjahr).
 */
object DateLogic {
    val locale: Locale = Locale.GERMANY
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun parse(s: String?): LocalDateTime? =
        if (s.isNullOrBlank()) null else runCatching { LocalDateTime.parse(s) }
            .recoverCatching { LocalDate.parse(s).atStartOfDay() }
            .getOrNull()

    fun format(dt: LocalDateTime): String = dt.withSecond(0).withNano(0).toString()

    /** k-tes Vorkommen (k >= 0) einer Wiederholungsregel. */
    fun occurrence(start: LocalDateTime, rule: RepeatRule, k: Long): LocalDateTime {
        val n = rule.interval.coerceAtLeast(1).toLong() * k
        return when (rule.type) {
            RepeatRule.DAILY -> start.plusDays(n)
            RepeatRule.WEEKLY -> start.plusWeeks(n)
            RepeatRule.MONTHLY -> start.plusMonths(n)
            RepeatRule.YEARLY -> start.plusYears(n)
            else -> start
        }
    }

    private fun untilLimit(rule: RepeatRule): LocalDateTime? =
        rule.until?.let { runCatching { LocalDate.parse(it).atTime(LocalTime.MAX) }.getOrNull() }

    /** Schätzt einen Startindex k, damit nicht alle Vorkommen ab Beginn durchlaufen werden müssen. */
    private fun estimateIndex(start: LocalDateTime, rule: RepeatRule, target: LocalDateTime): Long {
        if (!target.isAfter(start)) return 0
        val interval = rule.interval.coerceAtLeast(1).toLong()
        val units = when (rule.type) {
            RepeatRule.DAILY -> ChronoUnit.DAYS.between(start, target)
            RepeatRule.WEEKLY -> ChronoUnit.WEEKS.between(start, target)
            RepeatRule.MONTHLY -> ChronoUnit.MONTHS.between(start, target)
            RepeatRule.YEARLY -> ChronoUnit.YEARS.between(start, target)
            else -> 0
        }
        return (units / interval - 1).coerceAtLeast(0)
    }

    /**
     * Erstes Vorkommen, das >= [from] ist (bzw. > [from], wenn [strict]).
     * Gibt null zurück, wenn es keines mehr gibt (Ende erreicht / keine Wiederholung und vorbei).
     */
    fun nextOccurrence(
        start: LocalDateTime,
        rule: RepeatRule,
        from: LocalDateTime,
        strict: Boolean = false,
    ): LocalDateTime? {
        fun ok(t: LocalDateTime) = if (strict) t.isAfter(from) else !t.isBefore(from)
        if (!rule.isRepeating) return if (ok(start)) start else null
        val limit = untilLimit(rule)
        var k = estimateIndex(start, rule, from)
        // Rückwärts korrigieren, falls die Schätzung zu weit vorne liegt
        while (k > 0 && ok(occurrence(start, rule, k - 1))) k--
        var guard = 0
        while (guard++ < 100_000) {
            val t = occurrence(start, rule, k)
            if (limit != null && t.isAfter(limit)) return null
            if (ok(t)) return t
            k++
        }
        return null
    }

    /** Letztes Vorkommen, das <= [from] liegt (für „seit“-Anzeigen und „heute“-Erkennung). */
    fun previousOccurrence(start: LocalDateTime, rule: RepeatRule, from: LocalDateTime): LocalDateTime? {
        if (start.isAfter(from)) return null
        if (!rule.isRepeating) return start
        val limit = untilLimit(rule)
        var k = estimateIndex(start, rule, from)
        while (!occurrence(start, rule, k + 1).isAfter(from)) k++
        while (k > 0 && occurrence(start, rule, k).isAfter(from)) k--
        val t = occurrence(start, rule, k)
        return if (limit != null && t.isAfter(limit)) null else t
    }

    /** Alle Vorkommen, deren Zeitraum [start, start+duration] den Bereich [from, to] berührt. */
    fun occurrencesBetween(
        start: LocalDateTime,
        duration: Duration,
        rule: RepeatRule,
        from: LocalDateTime,
        to: LocalDateTime,
        max: Int = 500,
    ): List<LocalDateTime> {
        val result = ArrayList<LocalDateTime>()
        if (!rule.isRepeating) {
            val end = start.plus(duration)
            if (!start.isAfter(to) && !end.isBefore(from)) result += start
            return result
        }
        val limit = untilLimit(rule)
        val searchFrom = from.minus(duration)
        var idx = estimateIndex(start, rule, searchFrom)
        while (idx > 0 && !occurrence(start, rule, idx - 1).isBefore(searchFrom)) idx--
        var guard = 0
        while (result.size < max && guard++ < 100_000) {
            val t = occurrence(start, rule, idx)
            if (t.isAfter(to)) break
            if (limit != null && t.isAfter(limit)) break
            if (!t.plus(duration).isBefore(from)) result += t
            idx++
        }
        return result
    }

    /** Aufschlüsselung einer Zeitspanne in die gewählten Einheiten (Bitmaske). */
    fun breakdown(from: LocalDateTime, to: LocalDateTime, unitMask: Int): List<Pair<Int, Long>> {
        val mask = if (unitMask == 0) TimeUnits.DAYS else unitMask
        val a: LocalDateTime
        val b: LocalDateTime
        if (to.isBefore(from)) { a = to; b = from } else { a = from; b = to }
        val result = ArrayList<Pair<Int, Long>>()
        var cursor = a
        if (mask and TimeUnits.YEARS != 0) {
            val y = ChronoUnit.YEARS.between(cursor, b)
            cursor = cursor.plusYears(y)
            result += TimeUnits.YEARS to y
        }
        if (mask and TimeUnits.MONTHS != 0) {
            val m = ChronoUnit.MONTHS.between(cursor, b)
            cursor = cursor.plusMonths(m)
            result += TimeUnits.MONTHS to m
        }
        var secs = Duration.between(cursor, b).seconds.coerceAtLeast(0)
        val fixed = listOf(
            TimeUnits.WEEKS to 7L * 86_400,
            TimeUnits.DAYS to 86_400L,
            TimeUnits.HOURS to 3_600L,
            TimeUnits.MINUTES to 60L,
            TimeUnits.SECONDS to 1L,
        )
        for ((unit, size) in fixed) {
            if (mask and unit != 0) {
                val v = secs / size
                secs -= v * size
                result += unit to v
            }
        }
        return result
    }

    /** Automatische Einheit wie im Referenzbild: Tage, sonst Stunden, sonst Minuten, sonst Sekunden. */
    fun autoUnit(from: LocalDateTime, to: LocalDateTime): Pair<Int, Long> {
        val secs = kotlin.math.abs(Duration.between(from, to).seconds)
        return when {
            secs >= 86_400 -> TimeUnits.DAYS to secs / 86_400
            secs >= 3_600 -> TimeUnits.HOURS to secs / 3_600
            secs >= 60 -> TimeUnits.MINUTES to secs / 60
            else -> TimeUnits.SECONDS to secs
        }
    }

    // ---------- Formatierung ----------
    private val dayFmt = DateTimeFormatter.ofPattern("EEE, d. MMM yyyy", locale)
    private val dayShortFmt = DateTimeFormatter.ofPattern("d. MMM", locale)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    private val longDayFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", locale)

    fun formatDay(d: LocalDate): String = d.format(dayFmt)
    fun formatDayShort(d: LocalDate): String = d.format(dayShortFmt)
    fun formatLongDay(d: LocalDate): String = d.format(longDayFmt)
    fun formatTime(t: LocalDateTime): String = t.format(timeFmt)
    fun formatMonth(d: LocalDate): String = d.format(monthFmt).replaceFirstChar { it.uppercase(locale) }
    fun formatDateTime(t: LocalDateTime, withTime: Boolean = true): String =
        if (withTime) "${t.toLocalDate().format(dayFmt)}, ${t.format(timeFmt)}" else t.toLocalDate().format(dayFmt)

    fun toEpochSecondsLocal(t: LocalDateTime): Long = t.toEpochSecond(java.time.ZoneOffset.UTC)

    fun toEpochMillis(t: LocalDateTime): Long = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    fun fromEpochMillis(ms: Long): LocalDateTime =
        java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun describeOffset(minutes: Long): String = when {
        minutes == 0L -> "Zum Zeitpunkt"
        minutes % (7 * 1440) == 0L -> (minutes / (7 * 1440)).let { "$it ${if (it == 1L) "Woche" else "Wochen"} vorher" }
        minutes % 1440 == 0L -> (minutes / 1440).let { "$it ${if (it == 1L) "Tag" else "Tage"} vorher" }
        minutes % 60 == 0L -> (minutes / 60).let { "$it ${if (it == 1L) "Stunde" else "Stunden"} vorher" }
        else -> "$minutes Minuten vorher"
    }
}
