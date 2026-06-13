package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.feature.banksync.FakeEnableBankingClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ConnectBankUseCaseTest {

    private val client = FakeEnableBankingClient()
    private val useCase = ConnectBankUseCase(
        client = client,
        clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z")),
    )

    @Test
    fun returnsAuthStartOnSuccess() = runTest {
        client.authUrl = "https://bank.example/auth"
        val result = useCase(bankName = "Tatra", country = "SK")
        assertTrue(result.isSuccess)
        assertEquals("https://bank.example/auth", result.getOrNull()?.url)
    }

    @Test
    fun propagatesFailure() = runTest {
        client.startAuthError = EbError.RateLimited("slow down")
        val result = useCase(bankName = "Tatra", country = "SK")
        assertTrue(result.isFailure)
    }
}
