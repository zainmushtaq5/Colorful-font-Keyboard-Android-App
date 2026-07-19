package com.example.colortextstudio.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All Context/Bitmap/MediaStore side effects live here, isolated from the ViewModel so the
 * ViewModel's span/text logic remains unit-testable without Android dependencies.
 */
@Singleton
class ImageExporter @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    /** Saves [bitmap] to the device gallery via MediaStore (scoped storage, API 29+ safe). */
    fun saveToGallery(bitmap: Bitmap, displayName: String): android.net.Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ColorTextStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    /** Writes [bitmap] to app cache and returns a content:// Uri via FileProvider, for
     * sharing/clipboard where a MediaStore Uri isn't appropriate. */
    fun cacheAndGetContentUri(bitmap: Bitmap, fileName: String): android.net.Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareImage(uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyToClipboard(uri: android.net.Uri) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(context.contentResolver, "ColorText Studio image", uri)
        clipboard.setPrimaryClip(clip)
    }

    /** Saves a small thumbnail copy for the "My Creations" gallery, returns its content Uri string. */
    fun saveThumbnail(bitmap: Bitmap, fileName: String): String {
        val dir = File(context.cacheDir, "thumbs").apply { mkdirs() }
        val file = File(dir, fileName)
        val thumb = Bitmap.createScaledBitmap(bitmap, 240, (240f * bitmap.height / bitmap.width).toInt().coerceAtLeast(1), true)
        FileOutputStream(file).use { out -> thumb.compress(Bitmap.CompressFormat.PNG, 90, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
    }
}
