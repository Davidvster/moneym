package com.dv.moneym.platform

/**
 * Platform bridge for file save and open operations.
 * Implementations are platform-specific.
 */
expect class FilePlatform {
    /** Save text content to a file. Returns true on success. */
    suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean
    /** Open a text file and return its content, or null if cancelled. */
    suspend fun openTextFile(): String?
}
