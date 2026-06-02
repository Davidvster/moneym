package com.dv.moneym.data.accounts.internal

import com.dv.moneym.data.accounts.AccountSyncRow
import com.dv.moneym.data.accounts.db.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class FakeAccountLocalDataSource : AccountLocalDataSource {

    val rows = MutableStateFlow<List<AccountEntity>>(emptyList())
    private var nextId = 1L

    private fun mutate(block: (List<AccountEntity>) -> List<AccountEntity>) {
        rows.value = block(rows.value)
    }

    override fun observeAll(): Flow<List<AccountEntity>> =
        rows.map { list -> list.filter { !it.deleted }.sortedBy { it.name } }

    override fun observeDefault(): Flow<AccountEntity?> =
        rows.map { list -> list.firstOrNull { it.isDefault && !it.deleted } }

    override suspend fun getById(id: Long): AccountEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun count(): Long = rows.value.count { !it.deleted }.toLong()

    override suspend fun insert(
        name: String, type: String, currency: String,
        isDefault: Boolean, createdAt: Long, updatedAt: Long,
        colorHex: String?,
    ): Long {
        val id = nextId++
        mutate {
            it + AccountEntity(
                id = id,
                name = name,
                type = type,
                currency = currency,
                isDefault = isDefault,
                createdAt = createdAt,
                updatedAt = updatedAt,
                colorHex = colorHex,
                syncId = Uuid.random().toString(),
            )
        }
        return id
    }

    override suspend fun update(
        id: Long, name: String, type: String, currency: String,
        isDefault: Boolean, archived: Boolean, updatedAt: Long,
        colorHex: String?,
    ) {
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(
                    name = name,
                    type = type,
                    currency = currency,
                    isDefault = isDefault,
                    archived = archived,
                    updatedAt = updatedAt,
                    colorHex = colorHex,
                ) else it
            }
        }
    }

    override suspend fun softDelete(id: Long, now: Long) {
        mutate { list -> list.map { if (it.id == id) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {
        mutate { list -> list.map { if (it.syncId == syncId) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun reviveBySyncId(syncId: String, now: Long) {
        mutate { list -> list.map { if (it.syncId == syncId) it.copy(updatedAt = now) else it } }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun exportForSync(): List<AccountEntity> = rows.value

    override suspend fun upsertFromSync(row: AccountSyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = rows.value.firstOrNull { it.syncId == syncId }
        return if (existing == null) {
            val id = nextId++
            mutate {
                it + AccountEntity(
                    id = id,
                    name = row.name,
                    type = row.type,
                    currency = row.currency,
                    isDefault = row.isDefault,
                    archived = row.archived,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    colorHex = row.colorHex,
                    syncId = syncId,
                    deleted = row.deleted,
                )
            }
            id
        } else {
            mutate { list ->
                list.map {
                    if (it.syncId == syncId) it.copy(
                        name = row.name,
                        type = row.type,
                        currency = row.currency,
                        isDefault = row.isDefault,
                        archived = row.archived,
                        colorHex = row.colorHex,
                        updatedAt = row.updatedAt,
                        deleted = row.deleted,
                    ) else it
                }
            }
            existing.id
        }
    }
}
