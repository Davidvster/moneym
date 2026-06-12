package com.dv.moneym.feature.banksync

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.data.banksync.BankAuthCallbackBus
import com.dv.moneym.data.banksync.BankSyncEngine
import com.dv.moneym.data.banksync.EbBank
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.data.banksync.ExternalIdResolver
import com.dv.moneym.feature.banksync.usecase.CompleteConnectionUseCase
import com.dv.moneym.feature.banksync.usecase.ConnectBankUseCase
import com.dv.moneym.feature.banksync.usecase.ParseRedirectCodeUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BankSyncSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val client = FakeEnableBankingClient()
    private val settings = FakeAppSettings()
    private val bankRepo = FakeBankSyncRepository()
    private val credentialsStore = EnableBankingCredentialsStore(InMemorySecureStore())
    private val clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z"))
    private val callbackBus = BankAuthCallbackBus()

    private fun vm() = BankSyncSettingsViewModel(
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
        connectBank = ConnectBankUseCase(client, clock),
        completeConnection = CompleteConnectionUseCase(client, credentialsStore, bankRepo, settings),
        parseRedirectCode = ParseRedirectCodeUseCase(),
        callbackBus = callbackBus,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun savingValidCredentialsConfigures() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncSettingsIntent.AppIdChanged("app-1"))
            vm.onIntent(BankSyncSettingsIntent.PemChanged("PEM"))
            vm.onIntent(BankSyncSettingsIntent.SaveCredentials)
            var s = awaitItem()
            while (!s.configured) s = awaitItem()
            assertTrue(s.configured)
            assertEquals("", s.appIdDraft)
            cancelAndIgnoreRemainingEvents()
        }
        assertNotNull(credentialsStore.loadCredentials())
        assertTrue(settings.getBoolean(PrefKeys.BANK_SYNC_CONFIGURED))
    }

    @Test
    fun invalidCredentialsSurfaceError() = runTest(testDispatcher) {
        client.validateResult = Result.failure(EbError.Unauthorized("bad key"))
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncSettingsIntent.AppIdChanged("app-1"))
            vm.onIntent(BankSyncSettingsIntent.PemChanged("PEM"))
            vm.onIntent(BankSyncSettingsIntent.SaveCredentials)
            var s = awaitItem()
            while (s.credentialsError == null) s = awaitItem()
            assertEquals("bad key", s.credentialsError)
            assertFalse(s.configured)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(credentialsStore.loadCredentials())
    }

    @Test
    fun connectFlowEndToEndWithPastedRedirect() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(com.dv.moneym.data.banksync.EbCredentials("app-1", "PEM"))
        client.banks = listOf(EbBank("Tatra", "SK"))
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncSettingsIntent.CountryChanged("sk"))
            vm.onIntent(BankSyncSettingsIntent.LoadBanks)
            var s = awaitItem()
            while (s.banks.isEmpty()) s = awaitItem()
            assertEquals("SK", s.countryDraft)

            vm.onIntent(BankSyncSettingsIntent.ConnectBank("Tatra", "SK"))
            while (!s.awaitingAuth) s = awaitItem()
            assertEquals("https://bank.example/auth", s.authUrlToOpen)

            vm.onIntent(BankSyncSettingsIntent.AuthUrlOpened)
            vm.onIntent(BankSyncSettingsIntent.RedirectChanged("moneym://bank-callback?code=c0de&state=x"))
            vm.onIntent(BankSyncSettingsIntent.SubmitRedirect)
            while (s.awaitingAuth || !s.connected) s = awaitItem()

            assertTrue(s.connected)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("sess-1", credentialsStore.loadSessionId())
        assertEquals(1, bankRepo.accounts.size)
        assertEquals("acc-1", bankRepo.accounts.single().uid)
        assertTrue(settings.getLong(PrefKeys.BANK_SYNC_SESSION_VALID_UNTIL_MS) > 0)
    }

    @Test
    fun garbageRedirectFlagsInvalidWithoutSession() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncSettingsIntent.ConnectBank("Tatra", "SK"))
            var s = awaitItem()
            while (!s.awaitingAuth) s = awaitItem()

            vm.onIntent(BankSyncSettingsIntent.RedirectChanged("no code here"))
            vm.onIntent(BankSyncSettingsIntent.SubmitRedirect)
            while (!s.redirectInvalid) s = awaitItem()

            assertTrue(s.awaitingAuth)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(credentialsStore.loadSessionId())
    }

    @Test
    fun disconnectClearsEverything() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(com.dv.moneym.data.banksync.EbCredentials("app-1", "PEM"))
        credentialsStore.saveSessionId("sess-1")
        settings.putBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, true)
        val vm = vm()
        vm.state.test {
            awaitItem()
            var s = awaitItem()
            while (!s.connected) s = awaitItem()

            vm.onIntent(BankSyncSettingsIntent.Disconnect)
            while (s.configured) s = awaitItem()

            assertFalse(s.connected)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("sess-1", client.deletedSessionId)
        assertNull(credentialsStore.loadCredentials())
        assertFalse(settings.getBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED))
    }

    @Test
    fun deepLinkCallbackCompletesSessionWhileAwaitingAuth() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(com.dv.moneym.data.banksync.EbCredentials("app-1", "PEM"))
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncSettingsIntent.ConnectBank("Tatra", "SK"))
            var s = awaitItem()
            while (!s.awaitingAuth) s = awaitItem()

            callbackBus.emit("moneym://bank-callback?code=deep&state=x")
            while (s.awaitingAuth || !s.connected) s = awaitItem()

            assertTrue(s.connected)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("sess-1", credentialsStore.loadSessionId())
    }

    @Test
    fun deepLinkIgnoredWhenNotAwaitingAuth() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            awaitItem()
            callbackBus.emit("moneym://bank-callback?code=stray&state=x")
            testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(credentialsStore.loadSessionId())
    }

    @Test
    fun toggleAutoSyncPersists() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankSyncSettingsIntent.ToggleAutoSync)
            var s = awaitItem()
            while (!s.autoSyncEnabled) s = awaitItem()
            assertTrue(settings.getBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
