package com.dv.moneym.data.categories.internal

import app.cash.turbine.test
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.categories.CategorySyncRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class CategoryRepositoryImplTest {

    private val ds = FakeCategoryLocalDataSource()
    private val repo = CategoryRepositoryImpl(ds)
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun category(
        id: Long = 0,
        name: String = "Food",
        archived: Boolean = false,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = "food",
        colorHex = "#112233",
        isUserCreated = true,
        archived = archived,
        createdAt = epoch,
        updatedAt = epoch,
        type = type,
    )

    @Test
    fun insertReturnsTypedIdAndPersistsMappedFields() = runTestWithDispatchers {
        val id = repo.insert(category(name = "Salary", type = TransactionType.INCOME))

        assertEquals(1L, id.value)
        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals("Salary", stored.name)
        assertEquals("food", stored.iconKey)
        assertEquals("#112233", stored.colorHex)
        assertEquals(TransactionType.INCOME, stored.type)
        assertTrue(stored.isUserCreated)
    }

    @Test
    fun observeAllReturnsMappedDomainAndReflectsInserts() = runTestWithDispatchers {
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())

            repo.insert(category(name = "B"))
            assertEquals(listOf("B"), awaitItem().map { it.name })

            repo.insert(category(name = "A"))
            assertEquals(listOf("A", "B"), awaitItem().map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActiveExcludesArchived() = runTestWithDispatchers {
        repo.insert(category(name = "Active"))
        val archivedId = repo.insert(category(name = "Archived"))
        repo.update(category(id = archivedId.value, name = "Archived", archived = true))

        repo.observeActive().test {
            assertEquals(listOf("Active"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByIdMiss() = runTestWithDispatchers {
        assertNull(repo.getById(CategoryId(404)))
    }

    @Test
    fun countCountsActiveOnly() = runTestWithDispatchers {
        val id = repo.insert(category())
        repo.insert(category(name = "Two"))
        assertEquals(2L, repo.count())
        repo.delete(id)
        assertEquals(1L, repo.count())
    }

    @Test
    fun updatePropagatesFields() = runTestWithDispatchers {
        val id = repo.insert(category(name = "Old"))
        repo.update(category(id = id.value, name = "New", archived = true).copy(colorHex = "#FFFFFF"))

        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals("New", stored.name)
        assertEquals("#FFFFFF", stored.colorHex)
        assertTrue(stored.archived)
    }

    @Test
    fun deleteTombstonesRow() = runTestWithDispatchers {
        val id = repo.insert(category())
        repo.delete(id)

        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAllClearsEverything() = runTestWithDispatchers {
        repo.insert(category())
        repo.insert(category(name = "Two"))
        repo.deleteAll()
        assertEquals(0L, repo.count())
        assertTrue(ds.rows.value.isEmpty())
    }

    @Test
    fun exportForSyncMapsEntitiesToSyncRows() = runTestWithDispatchers {
        val id = repo.insert(category(name = "Exp", type = TransactionType.INCOME))

        val rows = repo.exportForSync()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(id.value, row.id)
        assertEquals("Exp", row.name)
        assertEquals("INCOME", row.categoryType)
        assertNotNull(row.syncId)
        assertFalse(row.deleted)
    }

    @Test
    fun upsertFromSyncInsertsThenUpdatesBySyncId() = runTestWithDispatchers {
        val syncId = "sync-1"
        val row = CategorySyncRow(
            id = 0,
            syncId = syncId,
            name = "Synced",
            iconKey = "icon",
            colorHex = "#000000",
            isUserCreated = false,
            archived = false,
            categoryType = "EXPENSE",
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
        val id = repo.insert(category())
        val syncId = ds.rows.value.first { it.id == id.value }.syncId!!

        repo.markDeletedBySyncId(syncId, 100)
        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)

        repo.reviveBySyncId(syncId, 200)
        assertEquals(200, ds.rows.value.first { it.id == id.value }.updatedAt)
    }
}
