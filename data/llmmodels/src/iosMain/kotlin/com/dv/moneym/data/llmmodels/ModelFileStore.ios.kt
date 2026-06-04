package com.dv.moneym.data.llmmodels

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.seekToEndOfFile
import platform.Foundation.writeData
import platform.Foundation.closeFile
import platform.posix.memcpy

actual fun createModelFileStore(rootDir: String): ModelFileStore = IosModelFileStore(rootDir)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosModelFileStore(rootDir: String) : ModelFileStore {

    private val fm = NSFileManager.defaultManager
    private val modelsDir = "$rootDir/models"

    private fun finalPathOf(fileName: String) = "$modelsDir/$fileName"
    private fun partPathOf(fileName: String) = "$modelsDir/$fileName.part"

    private fun ensureDir() {
        fm.createDirectoryAtPath(modelsDir, withIntermediateDirectories = true, attributes = null, error = null)
    }

    private fun sizeOf(path: String): Long {
        if (!fm.fileExistsAtPath(path)) return 0L
        val attrs = fm.attributesOfItemAtPath(path, error = null) ?: return 0L
        val size = attrs["NSFileSize"] as? NSNumber ?: return 0L
        return size.longLongValue
    }

    override fun finalPath(fileName: String): String = finalPathOf(fileName)

    override suspend fun finalExists(fileName: String): Boolean = withContext(Dispatchers.Default) {
        fm.fileExistsAtPath(finalPathOf(fileName))
    }

    override suspend fun finalSize(fileName: String): Long = withContext(Dispatchers.Default) {
        sizeOf(finalPathOf(fileName))
    }

    override suspend fun partSize(fileName: String): Long = withContext(Dispatchers.Default) {
        sizeOf(partPathOf(fileName))
    }

    override suspend fun appendToPart(fileName: String, bytes: ByteArray, offset: Int, length: Int) =
        withContext(Dispatchers.Default) {
            ensureDir()
            val path = partPathOf(fileName)
            if (!fm.fileExistsAtPath(path)) {
                fm.createFileAtPath(path, contents = null, attributes = null)
            }
            val slice = if (offset == 0 && length == bytes.size) bytes else bytes.copyOfRange(offset, offset + length)
            val data = slice.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = slice.size.toULong())
            }
            val handle = NSFileHandle.fileHandleForWritingAtPath(path)
            handle?.seekToEndOfFile()
            handle?.writeData(data)
            handle?.closeFile()
            Unit
        }

    override suspend fun resetPart(fileName: String) = withContext(Dispatchers.Default) {
        fm.removeItemAtPath(partPathOf(fileName), error = null)
        Unit
    }

    override suspend fun sha256OfPart(fileName: String): String = withContext(Dispatchers.Default) {
        val data = NSData.dataWithContentsOfFile(partPathOf(fileName)) ?: NSData()
        val length = data.length.toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
        sha256Hex(bytes)
    }

    override suspend fun promotePart(fileName: String) = withContext(Dispatchers.Default) {
        val target = finalPathOf(fileName)
        fm.removeItemAtPath(target, error = null)
        fm.moveItemAtPath(partPathOf(fileName), toPath = target, error = null)
        Unit
    }

    override suspend fun deletePart(fileName: String) = withContext(Dispatchers.Default) {
        fm.removeItemAtPath(partPathOf(fileName), error = null)
        Unit
    }

    override suspend fun deleteFinal(fileName: String) = withContext(Dispatchers.Default) {
        fm.removeItemAtPath(finalPathOf(fileName), error = null)
        Unit
    }
}
