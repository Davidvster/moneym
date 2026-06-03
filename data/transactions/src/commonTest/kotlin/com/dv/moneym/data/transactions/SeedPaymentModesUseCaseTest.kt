package com.dv.moneym.data.transactions

import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.transactions.internal.FakePaymentModeDao
import com.dv.moneym.data.transactions.internal.FakeTransactionsRoomDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class SeedPaymentModesUseCaseTest {

    @Test
    fun seeds_defaults_when_empty() = runTestWithDispatchers {
        val dao = FakePaymentModeDao()
        val useCase = SeedPaymentModesUseCase(FakeTransactionsRoomDatabase(dao))

        useCase()

        assertEquals(3L, dao.countAll())
        assertEquals(listOf("Cash", "Card", "Transfer"), dao.rows.value.map { it.name })
    }

    @Test
    fun is_idempotent_on_rerun() = runTestWithDispatchers {
        val dao = FakePaymentModeDao()
        val useCase = SeedPaymentModesUseCase(FakeTransactionsRoomDatabase(dao))

        useCase()
        useCase()

        assertEquals(3L, dao.countAll())
    }

    @Test
    fun does_not_seed_when_already_populated() = runTestWithDispatchers {
        val dao = FakePaymentModeDao()
        dao.insert(
            com.dv.moneym.data.transactions.db.PaymentModeEntity(
                name = "Existing",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val useCase = SeedPaymentModesUseCase(FakeTransactionsRoomDatabase(dao))

        useCase()

        assertEquals(1L, dao.countAll())
        assertEquals(listOf("Existing"), dao.rows.value.map { it.name })
    }
}
