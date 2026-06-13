package com.dv.moneym.data.banksync

import com.dv.moneym.core.testing.InMemorySecureStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class EnableBankingCredentialsStoreTest {

    private val store = EnableBankingCredentialsStore(InMemorySecureStore())

    @Test
    fun roundTripsCredentials() = runTest {
        store.saveCredentials(EbCredentials(applicationId = "app-1", privateKeyPem = "PEM"))
        val loaded = store.loadCredentials()
        assertEquals("app-1", loaded?.applicationId)
        assertEquals("PEM", loaded?.privateKeyPem)
    }

    @Test
    fun roundTripsSessionId() = runTest {
        store.saveSessionId("sess-1")
        assertEquals("sess-1", store.loadSessionId())
    }

    @Test
    fun clearSessionKeepsCredentials() = runTest {
        store.saveCredentials(EbCredentials("app-1", "PEM"))
        store.saveSessionId("sess-1")
        store.clearSession()
        assertNull(store.loadSessionId())
        assertEquals("app-1", store.loadCredentials()?.applicationId)
    }

    @Test
    fun clearAllRemovesEverything() = runTest {
        store.saveCredentials(EbCredentials("app-1", "PEM"))
        store.saveSessionId("sess-1")
        store.clearAll()
        assertNull(store.loadCredentials())
        assertNull(store.loadSessionId())
    }
}
