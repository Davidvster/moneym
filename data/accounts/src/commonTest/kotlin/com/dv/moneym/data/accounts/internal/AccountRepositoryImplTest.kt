package com.dv.moneym.data.accounts.internal

import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.accounts.AccountSyncRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AccountRepositoryImplTest {

    private val ds = FakeAccountLocalDataSource()
    private val repo = AccountRepositoryImpl(ds)
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun account(
        id: Long = 0,
        name: String = "Main",
        isDefault: Boolean = false,
    ) = Account(
        id = AccountId(id),
        name = name,
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = isDefault,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        colorHex = "#FFAA00",
    )

    @Test
    fun insertReturnsTypedIdAndPersistsMappedFields() = runTestWithDispatchers {
        val id = repo.insert(account(name = "Wallet"))

        assertEquals(1L, id.value)
        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals("Wallet", stored.name)
        assertEquals(AccountType.CASH, stored.type)
        assertEquals(CurrencyCode("EUR"), stored.currency)
        assertEquals("#FFAA00", stored.colorHex)
    }

    @Test
    fun observeAllReturnsMappedDomainAndReflectsUpdates() = runTestWithDispatchers {
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())

            repo.insert(account(name = "B"))
            assertEquals(listOf("B"), awaitItem().map { it.name })

            repo.insert(account(name = "A"))
            assertEquals(listOf("A", "B"), awaitItem().map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeDefaultReflectsDefaultAccount() = runTestWithDispatchers {
        repo.observeDefault().test {
            assertNull(awaitItem())

            repo.insert(account(name = "Def", isDefault = true))
            val def = awaitItem()
            assertNotNull(def)
            assertEquals("Def", def.name)
            assertTrue(def.isDefault)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByIdMiss() = runTestWithDispatchers {
        assertNull(repo.getById(AccountId(404)))
    }

    @Test
    fun countCountsActiveOnly() = runTestWithDispatchers {
        val id = repo.insert(account())
        repo.insert(account(name = "Two"))
        assertEquals(2L, repo.count())
        repo.delete(id)
        assertEquals(1L, repo.count())
    }

    @Test
    fun updatePropagatesFields() = runTestWithDispatchers {
        val id = repo.insert(account(name = "Old"))
        repo.update(account(id = id.value, name = "New").copy(currency = CurrencyCode("USD")))

        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals("New", stored.name)
        assertEquals(CurrencyCode("USD"), stored.currency)
    }

    @Test
    fun deleteTombstonesRow() = runTestWithDispatchers {
        val id = repo.insert(account())
        repo.delete(id)

        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)

        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAllClearsEverything() = runTestWithDispatchers {
        repo.insert(account())
        repo.insert(account(name = "Two"))
        repo.deleteAll()
        assertEquals(0L, repo.count())
        assertTrue(ds.rows.value.isEmpty())
    }

    @Test
    fun exportForSyncMapsEntitiesToSyncRows() = runTestWithDispatchers {
        val id = repo.insert(account(name = "Exp"))

        val rows = repo.exportForSync()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(id.value, row.id)
        assertEquals("Exp", row.name)
        assertEquals("CASH", row.type)
        assertNotNull(row.syncId)
        assertFalse(row.deleted)
    }

    @Test
    fun upsertFromSyncInsertsThenUpdatesBySyncId() = runTestWithDispatchers {
        val syncId = "sync-1"
        val row = AccountSyncRow(
            id = 0,
            syncId = syncId,
            name = "Synced",
            type = "BANK",
            currency = "USD",
            isDefault = false,
            archived = false,
            colorHex = null,
            deleted = false,
            createdAt = 10,
            updatedAt = 20,
        )

        val insertedId = repo.upsertFromSync(row)
        assertEquals(1, ds.rows.value.size)

        val updatedId = repo.upsertFromSync(row.copy(name = "Renamed", updatedAt = 30))
        assertEquals(insertedId, updatedId)
        assertEquals(1, ds.rows.value.size)
        assertEquals("Renamed", ds.rows.value.first().name)
    }

    @Test
    fun markDeletedAndReviveBySyncIdRoundTrip() = runTestWithDispatchers {
        val id = repo.insert(account())
        val syncId = ds.rows.value.first { it.id == id.value }.syncId!!

        repo.markDeletedBySyncId(syncId, 100)
        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)

        repo.reviveBySyncId(syncId, 200)
        assertEquals(200, ds.rows.value.first { it.id == id.value }.updatedAt)
    }
}
