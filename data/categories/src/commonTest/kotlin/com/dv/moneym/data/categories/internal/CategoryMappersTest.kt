package com.dv.moneym.data.categories.internal

import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.categories.db.CategoryEntity
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryMappersTest {

    private fun entity(
        id: Long = 1,
        name: String = "Food",
        iconKey: String = "fork",
        colorHex: String = "#FF0000",
        isUserCreated: Boolean = true,
        archived: Boolean = false,
        categoryType: String = "EXPENSE",
        syncId: String? = "sync-1",
        deleted: Boolean = false,
        createdAt: Long = 1000,
        updatedAt: Long = 2000,
    ) = CategoryEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        colorHex = colorHex,
        isUserCreated = isUserCreated,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        categoryType = categoryType,
        syncId = syncId,
        deleted = deleted,
    )

    @Test
    fun toDomainMapsAllFields() {
        val domain = entity(archived = true).toDomain()

        assertEquals(CategoryId(1), domain.id)
        assertEquals("Food", domain.name)
        assertEquals("fork", domain.iconKey)
        assertEquals("#FF0000", domain.colorHex)
        assertEquals(true, domain.isUserCreated)
        assertEquals(true, domain.archived)
        assertEquals(Instant.fromEpochMilliseconds(1000), domain.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(2000), domain.updatedAt)
        assertEquals(TransactionType.EXPENSE, domain.type)
    }

    @Test
    fun toDomainIncomeType() {
        assertEquals(TransactionType.INCOME, entity(categoryType = "INCOME").toDomain().type)
    }

    @Test
    fun toDomainUnknownTypeFallsBackToExpense() {
        assertEquals(TransactionType.EXPENSE, entity(categoryType = "SOMETHING").toDomain().type)
    }

    @Test
    fun toSyncRowMapsAllFields() {
        val row = entity(categoryType = "INCOME", deleted = true).toSyncRow()

        assertEquals(1, row.id)
        assertEquals("sync-1", row.syncId)
        assertEquals("Food", row.name)
        assertEquals("fork", row.iconKey)
        assertEquals("#FF0000", row.colorHex)
        assertEquals(true, row.isUserCreated)
        assertEquals(false, row.archived)
        assertEquals("INCOME", row.categoryType)
        assertEquals(true, row.deleted)
        assertEquals(1000, row.createdAt)
        assertEquals(2000, row.updatedAt)
    }

    @Test
    fun toSyncRowNullSyncId() {
        assertNull(entity(syncId = null).toSyncRow().syncId)
    }
}
