package com.dv.moneym.data.remotebackup

import com.dv.moneym.core.security.SecureStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSecureStore : SecureStore {
    private val map = mutableMapOf<String, ByteArray>()
    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) { map[key] = value }
    override suspend fun get(key: String): ByteArray? = map[key]
    override suspend fun remove(key: String) { map.remove(key) }
}

class SyncPassphraseStoreTest {

    @Test
    fun persistThenLoad_roundTrips() = runTest {
        val store = SyncPassphraseStore(FakeSecureStore())
        store.persist("hunter2".toCharArray())
        assertContentEquals("hunter2".toCharArray(), store.load())
    }

    @Test
    fun clear_removes() = runTest {
        val store = SyncPassphraseStore(FakeSecureStore())
        store.persist("secret".toCharArray())
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun hydrate_setsSessionWhenPresent() = runTest {
        val store = SyncPassphraseStore(FakeSecureStore())
        store.persist("secret".toCharArray())
        val session = SessionPassphrase()
        store.hydrate(session)
        assertTrue(session.isSet.value)
        assertContentEquals("secret".toCharArray(), session.get())
    }

    @Test
    fun hydrate_noOpWhenAbsent() = runTest {
        val store = SyncPassphraseStore(FakeSecureStore())
        val session = SessionPassphrase()
        store.hydrate(session)
        assertFalse(session.isSet.value)
        assertNull(session.get())
    }
}
