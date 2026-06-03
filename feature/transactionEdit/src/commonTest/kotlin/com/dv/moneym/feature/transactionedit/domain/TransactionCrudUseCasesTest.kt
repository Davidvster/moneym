package com.dv.moneym.feature.transactionedit.domain

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class TransactionCrudUseCasesTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val repo = FakeTransactionRepository()

    private fun txn(id: TransactionId, amountMinor: Long = 1000) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = LocalDate(2026, 5, 1),
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun get_returns_existing_transaction() = runTestWithDispatchers {
        val id = repo.upsert(txn(UNSAVED_TRANSACTION_ID))
        val result = GetTransactionUseCase(repo)(id)
        assertEquals(id, result?.id)
    }

    @Test
    fun get_returns_null_for_missing() = runTestWithDispatchers {
        val result = GetTransactionUseCase(repo)(TransactionId(999))
        assertNull(result)
    }

    @Test
    fun upsert_inserts_new_and_returns_generated_id() = runTestWithDispatchers {
        val id = UpsertTransactionUseCase(repo)(txn(UNSAVED_TRANSACTION_ID))
        assertTrue(id.value > 0)
        assertEquals(1, repo.transactions.size)
        assertEquals(id, repo.transactions.first().id)
    }

    @Test
    fun upsert_updates_existing_in_place() = runTestWithDispatchers {
        val id = repo.upsert(txn(UNSAVED_TRANSACTION_ID, amountMinor = 1000))
        val returned = UpsertTransactionUseCase(repo)(txn(id, amountMinor = 5000))
        assertEquals(id, returned)
        assertEquals(1, repo.transactions.size)
        assertEquals(5000L, repo.transactions.first().amount.minorUnits)
    }

    @Test
    fun delete_removes_transaction() = runTestWithDispatchers {
        val id = repo.upsert(txn(UNSAVED_TRANSACTION_ID))
        DeleteTransactionUseCase(repo)(id)
        assertTrue(repo.transactions.none { it.id == id })
    }
}
