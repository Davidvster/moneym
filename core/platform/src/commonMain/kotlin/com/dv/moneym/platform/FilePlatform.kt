package com.dv.moneym.platform

/**
 * Platform bridge for file save and open operations.
 * Implementations are platform-specific.
 */
expect class FilePlatform {
    /** Save text content to a file. Returns true on success. */
    suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean
    /** Save text content to a platform-specific local path. Returns the absolute path or null on failure. */
    suspend fun saveFileLocally(name: String, content: String): String?
    /** Save binary content to a platform-specific local path. Returns the absolute path or null on failure. */
    suspend fun saveFileLocallyBinary(name: String, bytes: ByteArray): String?
    /** Open a text file and return its content, or null if cancelled. */
    suspend fun openTextFile(): String?
    /** Save binary content to a SAF directory URI. Returns the file URI string or null on failure. */
    suspend fun saveFileToDirBinary(dirUri: String, name: String, bytes: ByteArray): String?
}
