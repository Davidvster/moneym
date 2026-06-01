package com.dv.moneym.data.accounts.internal

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.accounts.AccountSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class AccountRepositoryImpl(
    private val dataSource: AccountLocalDataSource,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        dataSource.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeDefault(): Flow<Account?> =
        dataSource.observeDefault().map { it?.toDomain() }

    override suspend fun getById(id: AccountId): Account? =
        dataSource.getById(id.value)?.toDomain()

    override suspend fun count(): Long = dataSource.count()

    override suspend fun insert(account: Account): AccountId {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dataSource.insert(
            name = account.name,
            type = account.type.name,
            currency = account.currency.value,
            isDefault = account.isDefault,
            createdAt = now,
            updatedAt = now,
            colorHex = account.colorHex,
        )
        return AccountId(id)
    }

    override suspend fun update(account: Account) {
        val now = Clock.System.now().toEpochMilliseconds()
        dataSource.update(
            id = account.id.value,
            name = account.name,
            type = account.type.name,
            currency = account.currency.value,
            isDefault = account.isDefault,
            archived = account.archived,
            updatedAt = now,
            colorHex = account.colorHex,
        )
    }

    override suspend fun delete(id: AccountId) =
        dataSource.softDelete(id.value, Clock.System.now().toEpochMilliseconds())

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) =
        dataSource.markDeletedBySyncId(syncId, now)

    override suspend fun reviveBySyncId(syncId: String, now: Long) =
        dataSource.reviveBySyncId(syncId, now)

    override suspend fun deleteAll() = dataSource.deleteAll()

    override suspend fun exportForSync(): List<AccountSyncRow> =
        dataSource.exportForSync().map { it.toSyncRow() }

    override suspend fun upsertFromSync(row: AccountSyncRow): Long = dataSource.upsertFromSync(row)
}
