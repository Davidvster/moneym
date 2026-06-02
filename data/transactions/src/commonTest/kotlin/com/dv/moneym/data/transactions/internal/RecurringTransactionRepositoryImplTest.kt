package com.dv.moneym.data.transactions.internal

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.transactions.RecurringSyncRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RecurringTransactionRepositoryImplTest {

    private val dao = FakeRecurringTransactionDao()
    private val repo = RecurringTransactionRepositoryImpl(dao)
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun rule(
        id: RecurringTransactionId = UNSAVED_RECURRING_ID,
        rule: RecurrenceRule = RecurrenceRule.Daily(1),
        end: EndCondition = EndCondition.Unlimited,
        lastMaterialized: LocalDate? = null,
    ) = RecurringTransaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(100, CurrencyCode("EUR")),
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        paymentModeId = null,
        startDate = LocalDate(2026, 5, 1),
        rule = rule,
        endCondition = end,
        lastMaterializedDate = lastMaterialized,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun upsertInsertsWhenUnsavedAndReturnsNewId() = runTestWithDispatchers {
        val id = repo.upsert(rule())

        assertEquals(1L, id.value)
        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals(Money(100, CurrencyCode("EUR")), stored.amount)
        assertEquals(LocalDate(2026, 5, 1), stored.startDate)
    }

    @Test
    fun upsertUpdatesWhenIdPresentPreservingSyncId() = runTestWithDispatchers {
        val id = repo.upsert(rule())
        val syncId = dao.rows.value.first { it.id == id.value }.syncId

        repo.upsert(rule(id = id).copy(note = "edited"))

        val stored = dao.rows.value.first { it.id == id.value }
        assertEquals(syncId, stored.syncId)
        assertEquals("edited", stored.note)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun observeAllMapsToDomainAndReflectsInserts() = runTestWithDispatchers {
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())

            repo.upsert(rule(rule = RecurrenceRule.Weekly(2, dayOfWeek = 3)))
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(RecurrenceRule.Weekly(2, dayOfWeek = 3), list.first().rule)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthlyLastDayRoundTripsThroughDomain() = runTestWithDispatchers {
        val id = repo.upsert(
            rule(rule = RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay)),
        )
        val stored = repo.getById(id)!!
        assertEquals(RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay), stored.rule)
    }

    @Test
    fun getByIdMiss() = runTestWithDispatchers {
        assertNull(repo.getById(RecurringTransactionId(404)))
    }

    @Test
    fun updateCursorSetsLastMaterialized() = runTestWithDispatchers {
        val id = repo.upsert(rule())
        repo.updateCursor(id, LocalDate(2026, 7, 9))

        assertEquals(LocalDate(2026, 7, 9), repo.getById(id)!!.lastMaterializedDate)
        assertEquals("2026-07-09", dao.rows.value.first { it.id == id.value }.lastMaterializedDate)
    }

    @Test
    fun deleteTombstonesRow() = runTestWithDispatchers {
        val id = repo.upsert(rule())
        repo.delete(id)

        assertTrue(dao.rows.value.first { it.id == id.value }.deleted)
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAllClearsEverything() = runTestWithDispatchers {
        repo.upsert(rule())
        repo.deleteAll()
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun exportForSyncMapsEntitiesToSyncRows() = runTestWithDispatchers {
        val id = repo.upsert(rule(end = EndCondition.Count(5)))

        val rows = repo.exportForSync()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(id.value, row.id)
        assertEquals("COUNT", row.endKind)
        assertEquals(5, row.endCount)
        assertNotNull(row.syncId)
        assertFalse(row.deleted)
    }

    @Test
    fun upsertFromSyncInsertsThenUpdatesBySyncId() = runTestWithDispatchers {
        val syncId = "sync-1"
        val row = RecurringSyncRow(
            id = 0,
            syncId = syncId,
            type = "EXPENSE",
            amountMinor = 100,
            currency = "EUR",
            note = null,
            categoryId = 1,
            accountId = 1,
            paymentModeId = null,
            startDate = "2026-05-01",
            freqUnit = "DAILY",
            freqInterval = 1,
            dayOfWeek = null,
            dayOfMonth = null,
            useLastDay = false,
            endKind = "UNLIMITED",
            endCount = null,
            endDate = null,
            lastMaterializedDate = null,
            deleted = false,
            createdAt = 10,
            updatedAt = 20,
        )

        val insertedId = repo.upsertFromSync(row)
        assertEquals(1, dao.rows.value.size)

        val updatedId = repo.upsertFromSync(row.copy(note = "synced", updatedAt = 30))
        assertEquals(insertedId, updatedId)
        assertEquals(1, dao.rows.value.size)
        assertEquals("synced", dao.rows.value.first().note)
    }

    @Test
    fun markDeletedAndReviveBySyncIdRoundTrip() = runTestWithDispatchers {
        val id = repo.upsert(rule())
        val syncId = dao.rows.value.first { it.id == id.value }.syncId!!

        repo.markDeletedBySyncId(syncId, 100)
        assertTrue(dao.rows.value.first { it.id == id.value }.deleted)

        repo.reviveBySyncId(syncId, 200)
        assertEquals(200, dao.rows.value.first { it.id == id.value }.updatedAt)
    }
}
