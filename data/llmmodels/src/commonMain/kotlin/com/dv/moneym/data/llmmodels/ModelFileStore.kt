package com.dv.moneym.data.llmmodels

/**
 * File-IO seam for model blobs. Kept free of [com.dv.moneym.platform.DbPlatform] so the repository
 * and downloader stay unit-testable from commonTest (DbPlatform is an expect class and cannot be
 * constructed there). The production impl is backed by DbPlatform.appFilesDirectory.
 */
interface ModelFileStore {
    fun finalPath(fileName: String): String
    suspend fun finalExists(fileName: String): Boolean
    suspend fun finalSize(fileName: String): Long
    suspend fun partSize(fileName: String): Long
    suspend fun appendToPart(fileName: String, bytes: ByteArray, offset: Int, length: Int)
    suspend fun resetPart(fileName: String)
    suspend fun sha256OfPart(fileName: String): String
    suspend fun promotePart(fileName: String)
    suspend fun deletePart(fileName: String)
    suspend fun deleteFinal(fileName: String)
}
