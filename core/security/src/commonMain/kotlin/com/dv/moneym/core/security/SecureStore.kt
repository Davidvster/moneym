package com.dv.moneym.core.security

interface SecureStore {
    suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean = false)
    suspend fun get(key: String): ByteArray?
    suspend fun remove(key: String)
}

// Platform implementations provided via AndroidSecureStore / IosSecureStore (Phase 4)
