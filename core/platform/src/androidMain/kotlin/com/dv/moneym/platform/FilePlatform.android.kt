package com.dv.moneym.platform

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FilePlatform(private val context: Context) {
    actual suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(context.cacheDir, suggestedName)
                cacheFile.writeText(content)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cacheFile,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Save $suggestedName").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    actual suspend fun saveFileLocally(name: String, content: String): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    resolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Downloads._ID),
                        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                        arrayOf(name),
                        null,
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(0)
                            resolver.delete(
                                ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                                null, null,
                            )
                        }
                    }
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { u ->
                        resolver.openOutputStream(u)?.use { os -> os.write(content.toByteArray()) }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(u, contentValues, null, null)
                        u.toString()
                    }
                } else {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, name)
                    file.writeText(content)
                    file.absolutePath
                }
            }.getOrNull()
        }
    }

    actual suspend fun saveFileLocallyBinary(name: String, bytes: ByteArray): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    // Delete any existing file with the same name to avoid (1), (2), etc.
                    resolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Downloads._ID),
                        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                        arrayOf(name),
                        null,
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(0)
                            resolver.delete(
                                ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                                null, null,
                            )
                        }
                    }
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { u ->
                        resolver.openOutputStream(u)?.use { os -> os.write(bytes) }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(u, contentValues, null, null)
                        u.toString()
                    }
                } else {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, name)
                    file.writeBytes(bytes)
                    file.absolutePath
                }
            }.getOrNull()
        }
    }

    actual suspend fun openTextFile(): String? = null
}
