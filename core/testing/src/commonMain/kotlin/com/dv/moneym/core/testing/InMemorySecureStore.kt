package com.dv.moneym.core.testing

import com.dv.moneym.core.security.SecureStore

class InMemorySecureStore : SecureStore {
    private val store = mutableMapOf<String, ByteArray>()
    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) { store[key] = value }
    override suspend fun get(key: String): ByteArray? = store[key]
    override suspend fun remove(key: String) { store.remove(key) }
}
