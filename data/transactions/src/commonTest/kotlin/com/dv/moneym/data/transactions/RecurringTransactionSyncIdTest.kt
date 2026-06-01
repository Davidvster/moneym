package com.dv.moneym.data.transactions

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.transactions.db.RecurringTransactionDao
import com.dv.moneym.data.transactions.db.RecurringTransactionEntity
import com.dv.moneym.data.transactions.internal.RecurringTransactionRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.time.Instant

class RecurringTransactionSyncIdTest {

    private class FakeRecurringDao : RecurringTransactionDao {
        val rows = mutableMapOf<Long, RecurringTransactionEntity>()
        private var nextId = 1L

        override fun selectAll(): Flow<List<RecurringTransactionEntity>> =
            MutableStateFlow(rows.values.toList())

        override suspend fun selectById(id: Long): RecurringTransactionEntity? = rows[id]

        override suspend fun insert(entity: RecurringTransactionEntity): Long {
            val id = nextId++
            rows[id] = entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: RecurringTransactionEntity) {
            rows[entity.id] = entity
        }

        override suspend fun updateCursor(id: Long, date: String, updatedAt: Long) {
            rows[id]?.let { rows[id] = it.copy(lastMaterializedDate = date, updatedAt = updatedAt) }
        }

        override suspend fun deleteById(id: Long) {
            rows.remove(id)
        }

        override suspend fun deleteAll() {
            rows.clear()
        }
    }

    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun rule(id: RecurringTransactionId) = RecurringTransaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(100, CurrencyCode("EUR")),
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        paymentModeId = null,
        startDate = LocalDate(2026, 5, 1),
        rule = RecurrenceRule.Daily(1),
        endCondition = EndCondition.Unlimited,
        lastMaterializedDate = null,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun insertGeneratesNonNullSyncIdAndUndeletedRow() = runTestWithDispatchers {
        val dao = FakeRecurringDao()
        val repo = RecurringTransactionRepositoryImpl(dao)

        val newId = repo.upsert(rule(UNSAVED_RECURRING_ID))

        val stored = dao.rows[newId.value]
        assertNotNull(stored)
        assertNotNull(stored.syncId)
        assertFalse(stored.deleted)
    }

    @Test
    fun updatePreservesExistingSyncId() = runTestWithDispatchers {
        val dao = FakeRecurringDao()
        val repo = RecurringTransactionRepositoryImpl(dao)

        val newId = repo.upsert(rule(UNSAVED_RECURRING_ID))
        val originalSyncId = dao.rows[newId.value]!!.syncId

        repo.upsert(rule(newId).copy(note = "edited"))

        assertEquals(originalSyncId, dao.rows[newId.value]!!.syncId)
        assertEquals("edited", dao.rows[newId.value]!!.note)
    }
}
