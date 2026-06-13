package com.dv.moneym.feature.banksync.credentials

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.feature.banksync.FakeEnableBankingClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BankSyncCredentialsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val client = FakeEnableBankingClient()
    private val settings = FakeAppSettings()
    private val credentialsStore = EnableBankingCredentialsStore(InMemorySecureStore())

    private fun vm() = BankSyncCredentialsViewModel(
        credentialsStore = credentialsStore,
        client = client,
        appSettings = settings,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun savingValidCredentialsStoresAndConfigures() = runTest(testDispatcher) {
        val vm = vm()
        vm.onIntent(BankSyncCredentialsIntent.AppIdChanged("app-1"))
        vm.onIntent(BankSyncCredentialsIntent.PemChanged("PEM"))
        vm.onIntent(BankSyncCredentialsIntent.SaveCredentials)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(credentialsStore.loadCredentials())
        assertTrue(settings.getBoolean(PrefKeys.BANK_SYNC_CONFIGURED))
    }

    @Test
    fun invalidCredentialsSurfaceError() = runTest(testDispatcher) {
        client.validateResult = Result.failure(EbError.Unauthorized("bad key"))
        val vm = vm()
        vm.onIntent(BankSyncCredentialsIntent.AppIdChanged("app-1"))
        vm.onIntent(BankSyncCredentialsIntent.PemChanged("PEM"))
        vm.onIntent(BankSyncCredentialsIntent.SaveCredentials)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("bad key", vm.state.value.credentialsError)
        assertNull(credentialsStore.loadCredentials())
    }
}
