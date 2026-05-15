package com.dv.moneym.data.accounts

import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedAccountsUseCaseTest {

    @Test
    fun seedsOneDefaultAccountOnFirstRun() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo)

        useCase()

        assertEquals(1, repo.accounts.size)
        assertTrue(repo.accounts.first().isDefault)
    }

    @Test
    fun doesNotSeedWhenAccountAlreadyExists() = runTestWithDispatchers {
        val repo = FakeAccountRepository()
        val useCase = SeedAccountsUseCase(repo)

        useCase()
        useCase()

        assertEquals(1, repo.accounts.size)
    }
}
