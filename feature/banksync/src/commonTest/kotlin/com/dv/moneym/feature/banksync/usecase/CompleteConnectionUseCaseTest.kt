package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.feature.banksync.FakeEnableBankingClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CompleteConnectionUseCaseTest {

    private val client = FakeEnableBankingClient()
    private val settings = FakeAppSettings()
    private val bankRepo = FakeBankSyncRepository()
    private val credentialsStore = EnableBankingCredentialsStore(InMemorySecureStore())
    private val useCase = CompleteConnectionUseCase(
        client = client,
        credentialsStore = credentialsStore,
        bankSyncRepository = bankRepo,
        appSettings = settings,
    )

    @Test
    fun storesSessionValidityAndAccounts() = runTest {
        val result = useCase(code = "c0de", bankName = "Tatra")

        assertTrue(result.isSuccess)
        assertEquals("sess-1", credentialsStore.loadSessionId())
        assertTrue(settings.getLong(PrefKeys.BANK_SYNC_SESSION_VALID_UNTIL_MS) > 0)
        val account = bankRepo.accounts.single()
        assertEquals("acc-1", account.uid)
        assertEquals("Tatra", account.bankName)
    }

    @Test
    fun failureLeavesNoSession() = runTest {
        client.createSessionError = EbError.Unauthorized("bad code")

        val result = useCase(code = "c0de", bankName = "Tatra")

        assertTrue(result.isFailure)
        assertNull(credentialsStore.loadSessionId())
        assertTrue(bankRepo.accounts.isEmpty())
    }
}
