package com.planora.app.data

import kotlinx.serialization.Serializable

/** Wiederholungsregel für Termine, Countdowns und Aufgaben. */
@Serializable
data class RepeatRule(
    val type: Int = NONE,
    val interval: Int = 1,
    /** optionales Enddatum (yyyy-MM-dd) */
    val until: String? = null,
) {
    val isRepeating: Boolean get() = type != NONE

    companion object {
        const val NONE = 0
        const val DAILY = 1
        const val WEEKLY = 2
        const val MONTHLY = 3
        const val YEARLY = 4

        val labels = listOf("Keine", "Täglich", "Wöchentlich", "Monatlich", "Jährlich")
        val unitLabels = listOf("", "Tage", "Wochen", "Monate", "Jahre")
    }

    fun describe(): String = when {
        type == NONE -> "Keine Wiederholung"
        interval <= 1 -> labels[type]
        else -> "Alle $interval ${unitLabels[type]}"
    }
}

/** Countdown-Zeiteinheiten als Bitmaske. */
object TimeUnits {
    const val YEARS = 1
    const val MONTHS = 2
    const val WEEKS = 4
    const val DAYS = 8
    const val HOURS = 16
    const val MINUTES = 32
    const val SECONDS = 64
    val ALL = listOf(YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS)

    fun label(unit: Int, value: Long): String = when (unit) {
        YEARS -> if (value == 1L) "Jahr" else "Jahre"
        MONTHS -> if (value == 1L) "Monat" else "Monate"
        WEEKS -> if (value == 1L) "Woche" else "Wochen"
        DAYS -> if (value == 1L) "Tag" else "Tage"
        HOURS -> if (value == 1L) "Stunde" else "Stunden"
        MINUTES -> if (value == 1L) "Minute" else "Minuten"
        else -> if (value == 1L) "Sekunde" else "Sekunden"
    }

    fun short(unit: Int): String = when (unit) {
        YEARS -> "J"
        MONTHS -> "Mo"
        WEEKS -> "W"
        DAYS -> "T"
        HOURS -> "Std"
        MINUTES -> "Min"
        else -> "Sek"
    }

    fun name(unit: Int): String = when (unit) {
        YEARS -> "Jahre"
        MONTHS -> "Monate"
        WEEKS -> "Wochen"
        DAYS -> "Tage"
        HOURS -> "Stunden"
        MINUTES -> "Minuten"
        else -> "Sekunden"
    }
}

/** Vollständige Gestaltung einer Countdown-Karte. */
@Serializable
data class CountdownStyle(
    /** 0 = Farbe, 1 = Farbverlauf, 2 = eigenes Bild, 3 = vorgefertigtes Muster */
    val bgType: Int = 0,
    val color: Int = 0xFFE63757.toInt(),
    val color2: Int = 0xFFFF8A65.toInt(),
    /** 0 = horizontal, 1 = vertikal, 2 = diagonal */
    val gradientDirection: Int = 2,
    val imageName: String? = null,
    val patternId: Int = 0,
    /** Deckkraft der Karte (Transparenz) */
    val alpha: Float = 1f,
    val brightness: Float = 1f,
    val tintColor: Int = 0xFF000000.toInt(),
    val tintAlpha: Float = 0f,
    val overlayEnabled: Boolean = false,
    /** 0 = nach unten dunkler, 1 = nach oben, 2 = nach rechts, 3 = nach links */
    val overlayDirection: Int = 0,
    val overlayColor: Int = 0xFF000000.toInt(),
    val overlayIntensity: Float = 0.6f,
    val focusX: Float = 0f,
    val focusY: Float = 0f,
    val zoom: Float = 1f,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val fontFamily: Int = 0,
    val titleSize: Int = 18,
    val numberSize: Int = 30,
    val cornerRadius: Int = 18,
    val height: Int = 80,
    val widthFraction: Float = 1f,
    val padding: Int = 12,
    /** 0 = Klassisch (wie Referenz), 1 = Groß, 2 = Kompakt, 3 = Zentriert */
    val layout: Int = 0,
    val emojiRight: Boolean = false,
    /** 0 = rechts, 1 = unter dem Titel */
    val timePosition: Int = 0,
    /** 0 = linksbündig, 1 = zentriert */
    val titleAlign: Int = 0,
    val autoUnit: Boolean = true,
    val units: Int = TimeUnits.DAYS or TimeUnits.HOURS or TimeUnits.MINUTES or TimeUnits.SECONDS,
    val showPanel: Boolean = true,
    /** 0 = abgerundet, 1 = abgeschrägt, 2 = Pille, 3 = eckig */
    val cardShape: Int = 0,
    val showDate: Boolean = true,
    val showDescription: Boolean = false,
    val shadow: Boolean = true,
)

/** Design eines Kalendertermins. */
@Serializable
data class EventStyle(
    /** 0 = gefüllt, 1 = Umriss, 2 = Verlauf, 3 = Bild */
    val design: Int = 0,
    val imageName: String? = null,
    val patternId: Int = -1,
)

object Defaults {
    /** Kräftige Farben, angelehnt an das Referenzbild. */
    val vivid = listOf(
        0xFFE63757.toInt(), 0xFFFFA800.toInt(), 0xFFF7208F.toInt(), 0xFF1CB5E0.toInt(),
        0xFF5B1E91.toInt(), 0xFF12B05A.toInt(), 0xFF8E95F2.toInt(), 0xFFE53935.toInt(),
        0xFF00897B.toInt(), 0xFF3949AB.toInt(), 0xFFFF7043.toInt(), 0xFF6D4C41.toInt(),
        0xFF546E7A.toInt(), 0xFFAB47BC.toInt(), 0xFF26C6DA.toInt(), 0xFF9CCC65.toInt(),
        0xFFFFD54F.toInt(), 0xFFF48FB1.toInt(), 0xFF212121.toInt(), 0xFFFFFFFF.toInt(),
    )
}
