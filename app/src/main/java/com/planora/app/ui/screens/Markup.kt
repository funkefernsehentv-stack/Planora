package com.planora.app.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Einfache, robuste Formatierungssprache für Notizen:
 *  # Überschrift, ## Zwischenüberschrift, - Aufzählung, 1. Nummerierung,
 *  **fett**, *kursiv*, [c=#RRGGBB]farbig[/c], [s=24]groß[/s]
 * Im Editor bleiben die Markierungen sichtbar (gedimmt), die Formatierung wird live angezeigt.
 */
object Markup {
    private val inline = Regex("""\*\*(.+?)\*\*|\*([^*\n]+)\*|\[c=(#?[0-9A-Fa-f]{6,8})](.+?)\[/c]|\[s=(\d{1,2})](.+?)\[/s]""")
    private val numbered = Regex("""^\d+\.\s""")

    private fun parseColor(hex: String): Color? = runCatching {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    }.getOrNull()

    private val h1 = SpanStyle(fontSize = 1.5.em, fontWeight = FontWeight.Bold)
    private val h2 = SpanStyle(fontSize = 1.25.em, fontWeight = FontWeight.Bold)

    /** Darstellung ohne Markierungen (Vorschau, Karten). */
    fun render(text: String): AnnotatedString = buildAnnotatedString {
        text.split("\n").forEachIndexed { i, raw ->
            if (i > 0) append("\n")
            var line = raw
            when {
                line.startsWith("## ") -> withStyle(h2) { renderInline(this, line.drop(3)) }
                line.startsWith("# ") -> withStyle(h1) { renderInline(this, line.drop(2)) }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("  •  ")
                    renderInline(this, line.drop(2))
                }
                numbered.containsMatchIn(line) -> {
                    val m = numbered.find(line)!!
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("  " + m.value) }
                    line = line.substring(m.range.last + 1)
                    renderInline(this, line)
                }
                else -> renderInline(this, line)
            }
        }
    }

    private fun renderInline(b: AnnotatedString.Builder, s: String) {
        var last = 0
        for (m in inline.findAll(s)) {
            b.append(s.substring(last, m.range.first))
            val g = m.groups
            when {
                g[1] != null -> b.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { renderInline(this, m.groupValues[1]) }
                g[2] != null -> b.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { renderInline(this, m.groupValues[2]) }
                g[3] != null -> {
                    val c = parseColor(m.groupValues[3])
                    if (c != null) b.withStyle(SpanStyle(color = c)) { renderInline(this, m.groupValues[4]) }
                    else renderInline(b, m.groupValues[4])
                }
                g[5] != null -> b.withStyle(SpanStyle(fontSize = (m.groupValues[5].toIntOrNull() ?: 16).coerceIn(8, 60).sp)) {
                    renderInline(this, m.groupValues[6])
                }
            }
            last = m.range.last + 1
        }
        b.append(s.substring(last))
    }

    fun toPlain(text: String): String = render(text).text

    /** Formatierung bei sichtbaren Markierungen (Länge des Texts bleibt identisch). */
    fun styleRaw(text: String, markerColor: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        val dim = SpanStyle(color = markerColor, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal)
        var offset = 0
        for (line in text.split("\n")) {
            when {
                line.startsWith("## ") -> { addStyle(h2, offset, offset + line.length); addStyle(dim, offset, offset + 3) }
                line.startsWith("# ") -> { addStyle(h1, offset, offset + line.length); addStyle(dim, offset, offset + 2) }
                line.startsWith("- ") || line.startsWith("* ") -> addStyle(SpanStyle(color = markerColor, fontWeight = FontWeight.Bold), offset, offset + 1)
                numbered.containsMatchIn(line) -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), offset, offset + numbered.find(line)!!.value.length)
            }
            styleInline(this, line, offset, dim)
            offset += line.length + 1
        }
    }

    private fun styleInline(b: AnnotatedString.Builder, s: String, base: Int, dim: SpanStyle) {
        for (m in inline.findAll(s)) {
            val start = base + m.range.first
            val end = base + m.range.last + 1
            val g = m.groups
            val (open, close, style, innerIdx) = when {
                g[1] != null -> Quad(2, 2, SpanStyle(fontWeight = FontWeight.Bold), 1)
                g[2] != null -> Quad(1, 1, SpanStyle(fontStyle = FontStyle.Italic), 2)
                g[3] != null -> Quad(m.groupValues[3].length + 4, 4, parseColor(m.groupValues[3])?.let { SpanStyle(color = it) } ?: SpanStyle(), 4)
                else -> Quad(m.groupValues[5].length + 4, 4,
                    SpanStyle(fontSize = (m.groupValues[5].toIntOrNull() ?: 16).coerceIn(8, 60).sp), 6)
            }
            b.addStyle(style, start + open, end - close)
            b.addStyle(dim, start, start + open)
            b.addStyle(dim, end - close, end)
            styleInline(b, m.groupValues[innerIdx], start + open, dim)
        }
    }

    private data class Quad(val a: Int, val b: Int, val c: SpanStyle, val d: Int)
}

class MarkupTransformation(private val markerColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(Markup.styleRaw(text.text, markerColor), OffsetMapping.Identity)
}
