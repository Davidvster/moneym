package com.dv.moneym.platform

expect class DbPlatform {
    val dbDirectory: String
    suspend fun readBytes(path: String): ByteArray?
    suspend fun writeBytes(path: String, bytes: ByteArray): Boolean
    suspend fun deleteFile(path: String)
    fun terminateApp()
}
