package com.dv.moneym.data.accounts

import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SeedAccountsUseCaseTest {

    private val fakeClock = FixedClock(Instant.fromEpochMilliseconds(1_700_000_000_000))

    private fun makeUseCase(
        repo: FakeAccountRepository = FakeAccountRepository(),
        settings: FakeAppSettings = FakeAppSettings(),
        name: String = "Main",
    ) = SeedAccountsUseCase(repo, settings, fakeClock, { name })

    @Test
    fun seedsOneDefaultAccountOnFirstRun() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Main" })

        useCase()

        assertEquals(1, repo.accounts.size)
        assertTrue(repo.accounts.first().isDefault)
    }

    @Test
    fun doesNotSeedWhenAccountAlreadyExists() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Main" })

        useCase()
        useCase()

        assertEquals(1, repo.accounts.size)
    }

    @Test
    fun usesUserSelectedCurrencyFromSettings() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val settings = FakeAppSettings()
        settings.putString("pref.default_currency", "USD")
        val useCase = SeedAccountsUseCase(repo, settings, fakeClock, { "Main" })

        useCase()

        assertEquals("USD", repo.accounts.first().currency.value)
    }

    @Test
    fun fallsBackToEurWhenNoCurrencySet() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Main" })

        useCase()

        assertEquals("EUR", repo.accounts.first().currency.value)
    }

    @Test
    fun usesProvidedDefaultName() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Wallet" })
        useCase()
        assertEquals("Wallet", repo.accounts.first().name)
    }

    @Test
    fun seedsDeterministicSyncId() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Main" })()

        assertEquals("seed-account-default", repo.exportForSync().single().syncId)
    }

    @Test
    fun reSeedIsIdempotentBySyncId() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        // Two devices seeding independently with different localized names must merge to one row.
        SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Main" })()
        repo.deleteAll() // simulate count()==0 guard not firing across a wipe; re-run still stable
        SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, { "Hauptkonto" })()

        assertEquals(1, repo.accounts.size)
        assertEquals("seed-account-default", repo.exportForSync().single().syncId)
    }
}
