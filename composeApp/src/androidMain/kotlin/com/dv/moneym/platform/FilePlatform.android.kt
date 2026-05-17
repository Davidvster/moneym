package com.dv.moneym.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
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

    actual suspend fun openTextFile(): String? = null
}
