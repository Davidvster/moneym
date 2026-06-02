package com.dv.moneym.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.posix.memcpy

actual class DbPlatform {
    @OptIn(ExperimentalForeignApi::class)
    actual val dbDirectory: String
        get() = NSFileManager.defaultManager.URLForDirectory(
            NSApplicationSupportDirectory, NSUserDomainMask, null, true, null
        )?.path ?: ""

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val data = NSData.create(contentsOfURL = NSURL.fileURLWithPath(path))
                ?: return@withContext null
            val length = data.length.toInt()
            if (length == 0) return@withContext ByteArray(0)
            val bytes = data.bytes ?: return@withContext null
            ByteArray(length).also { array ->
                array.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), bytes, data.length)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun writeBytes(path: String, bytes: ByteArray): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val fm = NSFileManager.defaultManager
            val parent = path.substringBeforeLast("/")
            fm.createDirectoryAtPath(parent, withIntermediateDirectories = true, attributes = null, error = null)
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToFile(path = path, atomically = true)
        }.getOrDefault(false)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun deleteFile(path: String) = withContext(Dispatchers.Default) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        Unit
    }
}
