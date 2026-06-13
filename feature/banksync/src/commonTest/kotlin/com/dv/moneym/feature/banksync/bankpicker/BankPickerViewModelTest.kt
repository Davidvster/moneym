package com.dv.moneym.feature.banksync.bankpicker

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.data.banksync.BankAuthCallbackBus
import com.dv.moneym.data.banksync.EbBank
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.feature.banksync.FakeEnableBankingClient
import com.dv.moneym.feature.banksync.usecase.CompleteConnectionUseCase
import com.dv.moneym.feature.banksync.usecase.ConnectBankUseCase
import com.dv.moneym.feature.banksync.usecase.ParseRedirectCodeUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BankPickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val client = FakeEnableBankingClient()
    private val settings = FakeAppSettings()
    private val bankRepo = FakeBankSyncRepository()
    private val credentialsStore = EnableBankingCredentialsStore(InMemorySecureStore())
    private val clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z"))
    private val callbackBus = BankAuthCallbackBus()

    private fun vm() = BankPickerViewModel(
        client = client,
        connectBank = ConnectBankUseCase(client, clock),
        completeConnection = CompleteConnectionUseCase(client, credentialsStore, bankRepo, settings),
        parseRedirectCode = ParseRedirectCodeUseCase(),
        callbackBus = callbackBus,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun connectFlowEndToEndWithPastedRedirect() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(EbCredentials("app-1", "PEM"))
        client.banks = listOf(EbBank("Tatra", "SK"))
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankPickerIntent.CountrySelected("SK"))
            var s = awaitItem()
            while (s.banks.isEmpty()) s = awaitItem()
            assertEquals("SK", s.selectedCountry)

            vm.onIntent(BankPickerIntent.ConnectBank("Tatra", "SK"))
            while (!s.awaitingAuth) s = awaitItem()
            assertEquals("https://bank.example/auth", s.authUrlToOpen)

            vm.onIntent(BankPickerIntent.AuthUrlOpened)
            vm.onIntent(BankPickerIntent.RedirectChanged("moneym://bank-callback?code=c0de&state=x"))
            vm.onIntent(BankPickerIntent.SubmitRedirect)
            while (s.awaitingAuth) s = awaitItem()
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
            vm.onIntent(BankPickerIntent.ConnectBank("Tatra", "SK"))
            var s = awaitItem()
            while (!s.awaitingAuth) s = awaitItem()

            vm.onIntent(BankPickerIntent.RedirectChanged("no code here"))
            vm.onIntent(BankPickerIntent.SubmitRedirect)
            while (!s.redirectInvalid) s = awaitItem()

            assertTrue(s.awaitingAuth)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(credentialsStore.loadSessionId())
    }

    @Test
    fun deepLinkCallbackCompletesSessionWhileAwaitingAuth() = runTest(testDispatcher) {
        credentialsStore.saveCredentials(EbCredentials("app-1", "PEM"))
        val vm = vm()
        vm.state.test {
            awaitItem()
            vm.onIntent(BankPickerIntent.ConnectBank("Tatra", "SK"))
            var s = awaitItem()
            while (!s.awaitingAuth) s = awaitItem()

            callbackBus.emit("moneym://bank-callback?code=deep&state=x")
            while (s.awaitingAuth) s = awaitItem()
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
}
