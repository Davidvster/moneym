package com.dv.moneym.data.llmmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

actual fun createModelFileStore(rootDir: String): ModelFileStore = AndroidModelFileStore(rootDir)

private class AndroidModelFileStore(rootDir: String) : ModelFileStore {

    private val modelsDir = File(rootDir, "models")

    private fun finalFile(fileName: String) = File(modelsDir, fileName)
    private fun partFile(fileName: String) = File(modelsDir, "$fileName.part")

    override fun finalPath(fileName: String): String = finalFile(fileName).absolutePath

    override suspend fun finalExists(fileName: String): Boolean = withContext(Dispatchers.IO) {
        finalFile(fileName).exists()
    }

    override suspend fun finalSize(fileName: String): Long = withContext(Dispatchers.IO) {
        finalFile(fileName).takeIf { it.exists() }?.length() ?: 0L
    }

    override suspend fun partSize(fileName: String): Long = withContext(Dispatchers.IO) {
        partFile(fileName).takeIf { it.exists() }?.length() ?: 0L
    }

    override suspend fun appendToPart(fileName: String, bytes: ByteArray, offset: Int, length: Int) =
        withContext(Dispatchers.IO) {
            modelsDir.mkdirs()
            FileOutputStream(partFile(fileName), true).use { out ->
                out.write(bytes, offset, length)
            }
        }

    override suspend fun resetPart(fileName: String) = withContext(Dispatchers.IO) {
        partFile(fileName).delete()
        Unit
    }

    override suspend fun sha256OfPart(fileName: String): String = withContext(Dispatchers.IO) {
        // Stream the file through the digest. readBytes() would load the whole model into one
        // ByteArray, which OOMs (or exceeds the 2 GB array limit) for multi-GB models.
        val digest = MessageDigest.getInstance("SHA-256")
        partFile(fileName).inputStream().buffered().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    override suspend fun promotePart(fileName: String) = withContext(Dispatchers.IO) {
        val part = partFile(fileName)
        val target = finalFile(fileName)
        target.delete()
        part.renameTo(target)
        Unit
    }

    override suspend fun deletePart(fileName: String) = withContext(Dispatchers.IO) {
        partFile(fileName).delete()
        Unit
    }

    override suspend fun deleteFinal(fileName: String) = withContext(Dispatchers.IO) {
        finalFile(fileName).delete()
        Unit
    }
}
