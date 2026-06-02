package com.dv.moneym.data.accounts.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.db.AccountEntity
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountMappersTest {

    private fun entity(
        id: Long = 1,
        name: String = "Main",
        type: String = "CASH",
        currency: String = "EUR",
        isDefault: Boolean = false,
        archived: Boolean = false,
        colorHex: String? = "#112233",
        syncId: String? = "sync-1",
        deleted: Boolean = false,
        createdAt: Long = 1000,
        updatedAt: Long = 2000,
    ) = AccountEntity(
        id = id,
        name = name,
        type = type,
        currency = currency,
        isDefault = isDefault,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        colorHex = colorHex,
        syncId = syncId,
        deleted = deleted,
    )

    @Test
    fun toDomainMapsAllFields() {
        val domain = entity(type = "BANK", isDefault = true).toDomain()

        assertEquals(AccountId(1), domain.id)
        assertEquals("Main", domain.name)
        assertEquals(AccountType.BANK, domain.type)
        assertEquals(CurrencyCode("EUR"), domain.currency)
        assertEquals(true, domain.isDefault)
        assertEquals(false, domain.archived)
        assertEquals(Instant.fromEpochMilliseconds(1000), domain.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(2000), domain.updatedAt)
        assertEquals("#112233", domain.colorHex)
    }

    @Test
    fun toDomainNullColorHex() {
        assertNull(entity(colorHex = null).toDomain().colorHex)
    }

    @Test
    fun toDomainArchivedDefaultCombos() {
        val archivedDefault = entity(isDefault = true, archived = true).toDomain()
        assertEquals(true, archivedDefault.isDefault)
        assertEquals(true, archivedDefault.archived)

        val plain = entity(isDefault = false, archived = false).toDomain()
        assertEquals(false, plain.isDefault)
        assertEquals(false, plain.archived)
    }

    @Test
    fun toDomainEachType() {
        assertEquals(AccountType.CASH, entity(type = "CASH").toDomain().type)
        assertEquals(AccountType.BANK, entity(type = "BANK").toDomain().type)
        assertEquals(AccountType.CARD, entity(type = "CARD").toDomain().type)
        assertEquals(AccountType.OTHER, entity(type = "OTHER").toDomain().type)
    }

    @Test
    fun toSyncRowMapsAllFields() {
        val row = entity(archived = true, deleted = true).toSyncRow()

        assertEquals(1, row.id)
        assertEquals("sync-1", row.syncId)
        assertEquals("Main", row.name)
        assertEquals("CASH", row.type)
        assertEquals("EUR", row.currency)
        assertEquals(false, row.isDefault)
        assertEquals(true, row.archived)
        assertEquals("#112233", row.colorHex)
        assertEquals(true, row.deleted)
        assertEquals(1000, row.createdAt)
        assertEquals(2000, row.updatedAt)
    }

    @Test
    fun toSyncRowNullColorAndSyncId() {
        val row = entity(colorHex = null, syncId = null).toSyncRow()
        assertNull(row.colorHex)
        assertNull(row.syncId)
    }
}
