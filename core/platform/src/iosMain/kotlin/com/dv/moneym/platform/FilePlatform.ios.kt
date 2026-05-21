package com.dv.moneym.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController

actual class FilePlatform {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean {
        return withContext(Dispatchers.Main) {
            runCatching {
                val filePath = "${NSTemporaryDirectory()}$suggestedName"
                val nsString = NSString.create(string = content)
                nsString.writeToFile(
                    path = filePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null,
                )
                val fileUrl = NSURL.fileURLWithPath(filePath)
                val activityVc = UIActivityViewController(
                    activityItems = listOf(fileUrl),
                    applicationActivities = null,
                )
                topViewController()?.presentViewController(activityVc, animated = true, completion = null)
                true
            }.getOrElse { false }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveFileLocally(name: String, content: String): String? {
        return withContext(Dispatchers.Main) {
            try {
                val dirs = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory, NSUserDomainMask, true
                )
                val documentsDir = dirs.firstOrNull() as? String ?: return@withContext null
                val filePath = "$documentsDir/$name"
                val nsString = NSString.create(string = content)
                nsString.writeToFile(
                    path = filePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null,
                )
                filePath
            } catch (e: Exception) {
                null
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveFileLocallyBinary(name: String, bytes: ByteArray): String? {
        return withContext(Dispatchers.Main) {
            runCatching {
                val dirs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
                val documentsDir = dirs.firstOrNull() as? String ?: return@withContext null
                val filePath = "$documentsDir/$name"
                val data = bytes.usePinned { pinned ->
                    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                }
                if (data.writeToFile(path = filePath, atomically = true)) filePath else null
            }.getOrNull()
        }
    }

    actual suspend fun openTextFile(): String? = null
}
