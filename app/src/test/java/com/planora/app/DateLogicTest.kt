package com.planora.app

import com.planora.app.data.*
import com.planora.app.domain.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.*

/** Prüft Datumsberechnungen, Wiederholungen (Monatslängen, Schaltjahre), Countdown-Sortierung und Aufgaben-Wiederholung. */
class DateLogicTest {
    private fun check(name: String, cond: Boolean) = assertTrue(name, cond)

    @Test
    fun dateLogic() {
        val now = LocalDateTime.of(2026, 11, 17, 14, 42)
        // Monatlich am 31.
        val s31 = LocalDateTime.of(2026, 1, 31, 10, 0)
        val m = RepeatRule(RepeatRule.MONTHLY)
        check("31. Jan + 1 Monat = 28. Feb", DateLogic.occurrence(s31, m, 1) == LocalDateTime.of(2026, 2, 28, 10, 0))
        check("31. Jan + 2 Monate = 31. Mär (kein Drift)", DateLogic.occurrence(s31, m, 2) == LocalDateTime.of(2026, 3, 31, 10, 0))
        check("next monthly ab 17.11 = 30.11", DateLogic.nextOccurrence(s31, m, now) == LocalDateTime.of(2026, 11, 30, 10, 0))
        // Schaltjahr
        val leap = LocalDateTime.of(2024, 2, 29, 0, 0)
        val y = RepeatRule(RepeatRule.YEARLY)
        check("29.2. jährlich -> 2027-02-28", DateLogic.nextOccurrence(leap, y, now) == LocalDateTime.of(2027, 2, 28, 0, 0))
        check("29.2. jährlich -> 2028-02-29", DateLogic.nextOccurrence(leap, y, LocalDateTime.of(2027, 3, 1, 0, 0)) == LocalDateTime.of(2028, 2, 29, 0, 0))
        // Geburtstag jährlich
        val bday = LocalDateTime.of(1990, 7, 28, 18, 0)
        check("Geburtstag nächster = 2027-07-28", DateLogic.nextOccurrence(bday, y, now) == LocalDateTime.of(2027, 7, 28, 18, 0))
        check("prev Geburtstag = 2026-07-28", DateLogic.previousOccurrence(bday, y, now) == LocalDateTime.of(2026, 7, 28, 18, 0))
        // Wöchentlich alle 2 Wochen
        val w2 = RepeatRule(RepeatRule.WEEKLY, 2)
        val ws = LocalDateTime.of(2026, 11, 2, 9, 0)
        check("2-wöchentlich next = 16.11? nein -> 30.11", DateLogic.nextOccurrence(ws, w2, now) == LocalDateTime.of(2026, 11, 30, 9, 0))
        // until
        val until = RepeatRule(RepeatRule.DAILY, 1, "2026-11-10")
        check("Ende erreicht -> null", DateLogic.nextOccurrence(ws, until, now) == null)
        // occurrencesBetween
        val occ = DateLogic.occurrencesBetween(ws, Duration.ofHours(1), RepeatRule(RepeatRule.DAILY), LocalDateTime.of(2026,11,10,0,0), LocalDateTime.of(2026,11,12,23,59))
        check("tägl. 3 Vorkommen", occ.size == 3 && occ[0].dayOfMonth == 10)
        val occ2 = DateLogic.occurrencesBetween(s31, Duration.ZERO, m, LocalDate.of(2026,1,1).atStartOfDay(), LocalDate.of(2026,12,31).atTime(23,59))
        check("monatlich 12 Vorkommen", occ2.size == 12)
        // Mehrtägig, Beginn vor Bereich
        val occ3 = DateLogic.occurrencesBetween(LocalDateTime.of(2026,11,15,0,0), Duration.ofDays(4), RepeatRule(), LocalDateTime.of(2026,11,17,0,0), LocalDateTime.of(2026,11,17,23,59))
        check("mehrtägig überlappt", occ3.size == 1)
        // Breakdown
        val to = LocalDateTime.of(2027, 2, 12, 16, 7, 30)
        val b = DateLogic.breakdown(now, to, TimeUnits.MONTHS or TimeUnits.WEEKS or TimeUnits.DAYS)
        println(b)
        check("Mo/W/T", b == listOf(TimeUnits.MONTHS to 2L, TimeUnits.WEEKS to 3L, TimeUnits.DAYS to 5L))
        val b2 = DateLogic.breakdown(now, to, TimeUnits.DAYS or TimeUnits.HOURS or TimeUnits.MINUTES or TimeUnits.SECONDS)
        println(b2)
        check("T/Std/Min/Sek", b2 == listOf(TimeUnits.DAYS to 87L, TimeUnits.HOURS to 1L, TimeUnits.MINUTES to 25L, TimeUnits.SECONDS to 30L))
        check("auto 55 Minuten", DateLogic.autoUnit(now, now.plusMinutes(55).plusSeconds(10)) == (TimeUnits.MINUTES to 55L))
        // Countdown-Sortierung
        val cs = listOf(
            CountdownEntity(id = 1, title = "Weihnachten", target = "2026-12-25T00:00", repeat = y),
            CountdownEntity(id = 2, title = "Morgen", target = "2026-11-18T10:00"),
            CountdownEntity(id = 3, title = "Vorbei", target = "2026-10-01T10:00"),
            CountdownEntity(id = 4, title = "Seit", target = "2020-01-01T00:00", mode = 1),
            CountdownEntity(id = 5, title = "Heute Geburtstag", target = "2000-11-17T09:00", repeat = y),
            CountdownEntity(id = 6, title = "Silvester", target = "2026-12-31T23:59"),
        )
        val sorted = CountdownLogic.sort(cs, 0, now, emptyList()).map { it.id }
        println(sorted)
        check("Sortierung nächste oben", sorted == listOf(5L, 2L, 1L, 6L, 4L, 3L))
        val pinned = CountdownLogic.sort(cs.map { if (it.id == 6L) it.copy(pinned = true) else it }, 0, now, emptyList()).map { it.id }
        check("Angeheftet zuerst", pinned.first() == 6L)
        // Aufgaben
        val t = TaskEntity(checklistId = 1, text = "Zähne", due = "2026-11-17T08:00", repeat = RepeatRule(RepeatRule.DAILY))
        val t2 = TaskLogic.toggle(t, now)
        check("tägliche Aufgabe -> morgen", !t2.done && t2.due == "2026-11-18T08:00")
        val t3 = TaskLogic.toggle(t.copy(due = "2026-11-10T08:00"), now)
        check("überfällig -> morgen", t3.due == "2026-11-18T08:00")
        val t4 = TaskLogic.toggle(t.copy(repeat = RepeatRule()), now)
        check("einmalig -> erledigt", t4.done)
        // Tageszuordnung
        val e = EventEntity(title = "Urlaub", start = "2026-07-30T00:00", end = "2026-08-05T00:00", allDay = true)
        val o = EventLogic.occurrences(listOf(e), LocalDate.of(2026,7,1), LocalDate.of(2026,8,31))
        val bd = EventLogic.byDay(o, LocalDate.of(2026,7,1), LocalDate.of(2026,8,31))
        check("Urlaub 6 Tage", bd.size == 6)
    }
}
