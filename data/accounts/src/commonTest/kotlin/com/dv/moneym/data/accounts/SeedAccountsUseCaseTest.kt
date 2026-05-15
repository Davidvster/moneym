package com.dv.moneym.data.accounts

import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedAccountsUseCaseTest {

    private fun makeUseCase(settings: FakeAppSettings = FakeAppSettings()) =
        SeedAccountsUseCase(FakeAccountRepository(), settings)

    @Test
    fun seedsOneDefaultAccountOnFirstRun() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings())

        useCase()

        assertEquals(1, repo.accounts.size)
        assertTrue(repo.accounts.first().isDefault)
    }

    @Test
    fun doesNotSeedWhenAccountAlreadyExists() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings())

        useCase()
        useCase()

        assertEquals(1, repo.accounts.size)
    }

    @Test
    fun usesUserSelectedCurrencyFromSettings() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val settings = FakeAppSettings()
        settings.putString("pref.default_currency", "USD")
        val useCase = SeedAccountsUseCase(repo, settings)

        useCase()

        assertEquals("USD", repo.accounts.first().currency.value)
    }

    @Test
    fun fallsBackToEurWhenNoCurrencySet() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings())

        useCase()

        assertEquals("EUR", repo.accounts.first().currency.value)
    }
}
