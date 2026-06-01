package com.dv.moneym.data.accounts.internal

import com.dv.moneym.data.accounts.AccountSyncRow
import com.dv.moneym.data.accounts.db.AccountEntity
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal class SqlDelightAccountDataSource(
    private val db: AccountsRoomDatabase,
) : AccountLocalDataSource {

    private val dao get() = db.accountDao()

    override fun observeAll(): Flow<List<AccountEntity>> = dao.selectAll()

    override fun observeDefault(): Flow<AccountEntity?> = dao.selectDefault()

    override suspend fun getById(id: Long): AccountEntity? = dao.selectById(id)

    override suspend fun count(): Long = dao.countAll()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun insert(
        name: String, type: String, currency: String,
        isDefault: Boolean, createdAt: Long, updatedAt: Long,
        colorHex: String?,
    ): Long = dao.insert(
        AccountEntity(
            name = name,
            type = type,
            currency = currency,
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt,
            colorHex = colorHex,
            syncId = Uuid.random().toString(),
        )
    )

    override suspend fun update(
        id: Long, name: String, type: String, currency: String,
        isDefault: Boolean, archived: Boolean, updatedAt: Long,
        colorHex: String?,
    ) {
        val existing = dao.selectById(id) ?: return
        dao.update(
            existing.copy(
                name = name,
                type = type,
                currency = currency,
                isDefault = isDefault,
                archived = archived,
                updatedAt = updatedAt,
                colorHex = colorHex
            )
        )
    }

    override suspend fun softDelete(id: Long, now: Long) = dao.softDeleteById(id, now)

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) =
        dao.markDeletedBySyncId(syncId, now)

    override suspend fun reviveBySyncId(syncId: String, now: Long) =
        dao.touchBySyncId(syncId, now)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun exportForSync(): List<AccountEntity> = dao.selectAllForSync()

    override suspend fun upsertFromSync(row: AccountSyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = dao.selectBySyncId(syncId)
        return if (existing == null) {
            dao.insert(
                AccountEntity(
                    id = 0,
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
            )
        } else {
            dao.update(
                existing.copy(
                    name = row.name,
                    type = row.type,
                    currency = row.currency,
                    isDefault = row.isDefault,
                    archived = row.archived,
                    colorHex = row.colorHex,
                    updatedAt = row.updatedAt,
                    deleted = row.deleted,
                )
            )
            existing.id
        }
    }
}
