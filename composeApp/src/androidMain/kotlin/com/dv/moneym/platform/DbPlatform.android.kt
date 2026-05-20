package com.dv.moneym.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

actual class DbPlatform(private val context: Context) {
    actual val dbDirectory: String
        get() = context.getDatabasePath("x").parentFile?.absolutePath ?: context.filesDir.absolutePath

    actual suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(path).readBytes() }.getOrNull()
    }

    actual suspend fun writeBytes(path: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }.isSuccess
    }

    actual fun terminateApp() { exitProcess(0) }
}
