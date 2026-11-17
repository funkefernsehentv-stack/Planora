package com.planora.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Bilder werden in den internen Speicher kopiert (filesDir/images) und nur per Dateiname referenziert.
 * Dadurch bleiben sie dauerhaft verfügbar und lassen sich im Export mitsichern.
 */
object ImageStore {
    fun dir(context: Context): File = File(context.filesDir, "images").apply { mkdirs() }

    fun file(context: Context, name: String?): File? =
        name?.let { File(dir(context), it) }?.takeIf { it.exists() }

    /** Kopiert ein ausgewähltes Bild, verkleinert es auf max. 1600 px und speichert es als JPEG. */
    fun import(context: Context, uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val name = "img_${UUID.randomUUID()}.jpg"
        File(dir(context), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bmp.recycle()
        name
    }.getOrNull()

    fun delete(context: Context, name: String?) {
        name?.let { File(dir(context), it).delete() }
    }

    /** Lädt ein Bild verkleinert (für Widgets). */
    fun loadScaled(context: Context, name: String?, maxSize: Int): Bitmap? {
        val f = file(context, name) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) sample *= 2
        return BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
