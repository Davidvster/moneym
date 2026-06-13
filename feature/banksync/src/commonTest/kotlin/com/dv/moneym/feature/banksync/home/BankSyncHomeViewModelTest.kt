package com.dv.moneym.feature.banksync.home

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.data.banksync.BankSyncEngine
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.data.banksync.ExternalIdResolver
import com.dv.moneym.feature.banksync.FakeEnableBankingClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BankSyncHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val client = FakeEnableBankingClient()
    private val settings = FakeAppSettings()
    private val bankRepo = FakeBankSyncRepository()
    private val credentialsStore = EnableBankingCredentialsStore(InMemorySecureStore())
    private val clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z"))

    private fun vm() = BankSyncHomeViewModel(
        credentialsStore = credentialsStore,
        client = client,
        bankSyncRepository = bankRepo,
        engine = BankSyncEngine(
            client = client,
            credentialsStore = credentialsStore,
            bankSyncRepository = bankRepo,
            transactionRepository = FakeTransactionRepository(),
            externalIdResolver = ExternalIdResolver(),
            appSettings = settings,
            clock = clock,
        ),
        appSettings = settings,
        accountRepository = FakeAccountRepository(),
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun disconnectClearsEverything() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(EbCredentials("app-1", "PEM"))
        credentialsStore.saveSessionId("sess-1")
        settings.putBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, true)
        val vm = vm()
        vm.state.test {
            awaitItem()
            var s = awaitItem()
            while (!s.connected) s = awaitItem()

            vm.onIntent(BankSyncHomeIntent.Disconnect)
            while (s.configured) s = awaitItem()

            assertFalse(s.connected)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("sess-1", client.deletedSessionId)
        assertNull(credentialsStore.loadCredentials())
        assertFalse(settings.getBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED))
    }

    @Test
    fun toggleAutoSyncPersists() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(EbCredentials("app-1", "PEM"))
        credentialsStore.saveSessionId("sess-1")
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncHomeIntent.ToggleAutoSync)
            var s = awaitItem()
            while (!s.autoSyncEnabled) s = awaitItem()
            assertTrue(settings.getBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
