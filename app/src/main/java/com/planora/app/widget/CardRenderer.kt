package com.planora.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.planora.app.data.CountdownStyle
import com.planora.app.data.ImageStore
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Vorgefertigte Hintergrundmuster – werden sowohl in der App als auch in Widgets gezeichnet. */
object PatternPainter {
    val names = listOf("Konfetti", "Streifen", "Wellen", "Sterne", "Blasen", "Punkte", "Herzen", "Schnee")

    fun draw(canvas: Canvas, w: Float, h: Float, id: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(id * 7919 + 13)
        val unit = min(w, h)
        when (id) {
            0 -> repeat(40) { // Konfetti
                p.color = listOf(0x66FFFFFF, 0x55FFEB3B, 0x55FF4081, 0x5540C4FF)[it % 4].toInt()
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                canvas.save()
                canvas.rotate(rnd.nextFloat() * 180, x, y)
                canvas.drawRoundRect(RectF(x, y, x + unit * 0.06f, y + unit * 0.025f), 4f, 4f, p)
                canvas.restore()
            }
            1 -> { // Streifen
                p.color = 0x22FFFFFF
                p.strokeWidth = unit * 0.08f
                var x = -h
                while (x < w + h) {
                    canvas.drawLine(x, h, x + h, 0f, p)
                    x += unit * 0.22f
                }
            }
            2 -> { // Wellen
                p.style = Paint.Style.STROKE
                p.strokeWidth = unit * 0.035f
                p.color = 0x33FFFFFF
                var y = h * 0.15f
                while (y < h * 1.1f) {
                    val path = Path()
                    path.moveTo(0f, y)
                    var x = 0f
                    val step = unit * 0.5f
                    while (x < w) {
                        path.quadTo(x + step / 4, y - unit * 0.08f, x + step / 2, y)
                        path.quadTo(x + 3 * step / 4, y + unit * 0.08f, x + step, y)
                        x += step
                    }
                    canvas.drawPath(path, p)
                    y += unit * 0.22f
                }
            }
            3 -> repeat(35) { // Sterne
                p.color = if (it % 3 == 0) 0x88FFFFFF.toInt() else 0x44FFFFFF
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val r = unit * (0.008f + rnd.nextFloat() * 0.025f)
                star(canvas, x, y, r, p)
            }
            4 -> repeat(14) { // Blasen
                p.color = 0x22FFFFFF
                canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, unit * (0.05f + rnd.nextFloat() * 0.25f), p)
            }
            5 -> { // Punkte
                p.color = 0x2EFFFFFF
                val step = unit * 0.14f
                var y = step / 2
                var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) step / 2 else step
                    while (x < w) {
                        canvas.drawCircle(x, y, unit * 0.022f, p)
                        x += step
                    }
                    y += step
                    row++
                }
            }
            6 -> repeat(16) { // Herzen
                p.color = if (it % 2 == 0) 0x40FFFFFF else 0x30FF80AB
                heart(canvas, rnd.nextFloat() * w, rnd.nextFloat() * h, unit * (0.05f + rnd.nextFloat() * 0.08f), p)
            }
            7 -> repeat(60) { // Schnee
                p.color = 0x99FFFFFF.toInt()
                canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, unit * (0.006f + rnd.nextFloat() * 0.016f), p)
            }
        }
    }

    private fun star(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        val path = Path()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) r else r * 0.45f
            val a = Math.PI / 5 * i - Math.PI / 2
            val x = cx + (rr * Math.cos(a)).toFloat()
            val y = cy + (rr * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, p)
    }

    private fun heart(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        val path = Path()
        path.moveTo(cx, cy + s * 0.35f)
        path.cubicTo(cx - s, cy - s * 0.3f, cx - s * 0.4f, cy - s, cx, cy - s * 0.4f)
        path.cubicTo(cx + s * 0.4f, cy - s, cx + s, cy - s * 0.3f, cx, cy + s * 0.35f)
        c.drawPath(path, p)
    }
}

/** Zeichnet Karten-Hintergründe als Bitmap (für Homescreen-Widgets). */
object CardRenderer {

    fun gradientPoints(direction: Int, w: Float, h: Float): FloatArray = when (direction) {
        0 -> floatArrayOf(0f, 0f, w, 0f)
        1 -> floatArrayOf(0f, 0f, 0f, h)
        else -> floatArrayOf(0f, 0f, w, h)
    }

    fun render(
        context: Context,
        style: CountdownStyle,
        widthPx: Int,
        heightPx: Int,
        radiusPx: Float,
        alphaFactor: Float = 1f,
        overrideColor: Int? = null,
    ): Bitmap {
        // Speichergrenzen von RemoteViews beachten
        val scale = min(1f, 900f / max(widthPx, heightPx).coerceAtLeast(1))
        val w = (widthPx * scale).toInt().coerceAtLeast(8)
        val h = (heightPx * scale).toInt().coerceAtLeast(8)
        val r = radiusPx * scale
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Path.Direction.CW) }
        c.clipPath(clip)
        val layerAlpha = (255 * (style.alpha * alphaFactor).coerceIn(0f, 1f)).toInt()
        c.saveLayerAlpha(0f, 0f, w.toFloat(), h.toFloat(), layerAlpha)

        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (overrideColor != null) {
            c.drawColor(overrideColor)
        } else {
            when (style.bgType) {
                1, 3 -> {
                    val pts = gradientPoints(style.gradientDirection, w.toFloat(), h.toFloat())
                    p.shader = LinearGradient(pts[0], pts[1], pts[2], pts[3], style.color, style.color2, Shader.TileMode.CLAMP)
                    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
                    p.shader = null
                    if (style.bgType == 3) PatternPainter.draw(c, w.toFloat(), h.toFloat(), style.patternId)
                }
                2 -> {
                    c.drawColor(style.color)
                    ImageStore.loadScaled(context, style.imageName, max(w, h))?.let { img ->
                        val s = max(w.toFloat() / img.width, h.toFloat() / img.height) * style.zoom.coerceAtLeast(1f)
                        val dw = img.width * s
                        val dh = img.height * s
                        val left = (w - dw) * (1f + style.focusX) / 2f
                        val top = (h - dh) * (1f + style.focusY) / 2f
                        val b = style.brightness
                        p.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(b, b, b, 1f) })
                        c.drawBitmap(img, null, RectF(left, top, left + dw, top + dh), p)
                        p.colorFilter = null
                        img.recycle()
                    }
                }
                else -> c.drawColor(style.color)
            }
            if (style.tintAlpha > 0f) {
                c.drawColor(withAlpha(style.tintColor, style.tintAlpha))
            }
            if (style.overlayEnabled) {
                val oc = withAlpha(style.overlayColor, style.overlayIntensity)
                val transparent = withAlpha(style.overlayColor, 0f)
                val (x0, y0, x1, y1) = when (style.overlayDirection) {
                    1 -> listOf(0f, h.toFloat(), 0f, 0f)
                    2 -> listOf(0f, 0f, w.toFloat(), 0f)
                    3 -> listOf(w.toFloat(), 0f, 0f, 0f)
                    else -> listOf(0f, 0f, 0f, h.toFloat())
                }
                p.shader = LinearGradient(x0, y0, x1, y1, transparent, oc, Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
                p.shader = null
            }
        }
        c.restore()
        return bmp
    }

    /** Einfarbiger, abgerundeter Hintergrund. */
    fun solid(context: Context, color: Int, widthPx: Int, heightPx: Int, radiusPx: Float, alpha: Float = 1f): Bitmap =
        render(context, CountdownStyle(bgType = 0, color = color), widthPx, heightPx, radiusPx, alphaFactor = alpha)

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }
}
