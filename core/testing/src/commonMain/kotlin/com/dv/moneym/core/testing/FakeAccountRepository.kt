package com.dv.moneym.core.testing

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.accounts.AccountSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakeAccountRepository : AccountRepository {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L

    // Real syncId per logical row, keyed by local PK. Lazily seeded for rows that
    // never went through upsertFromSync so exportForSync still emits a stable id.
    private val syncIds = mutableMapOf<Long, String>()

    // Soft-deleted rows: hidden from observeAll/count, still exported with deleted=true.
    private val tombstoned = mutableSetOf<Long>()
    private val updatedAtOverrides = mutableMapOf<Long, Long>()

    val accounts: List<Account> get() = _accounts.value.filter { it.id.value !in tombstoned }

    fun addAll(accounts: List<Account>) = _accounts.update { it + accounts }

    override fun observeAll(): Flow<List<Account>> =
        _accounts.map { list -> list.filter { it.id.value !in tombstoned } }
    override fun observeDefault(): Flow<Account?> =
        _accounts.map { it.firstOrNull { a -> a.isDefault && a.id.value !in tombstoned } }
    override suspend fun getById(id: AccountId): Account? = _accounts.value.find { it.id == id }
    override suspend fun count(): Long = _accounts.value.count { it.id.value !in tombstoned }.toLong()
    override suspend fun insert(account: Account): AccountId {
        val id = AccountId(nextId++)
        _accounts.update { it + account.copy(id = id) }
        return id
    }
    override suspend fun update(account: Account) {
        _accounts.update { list -> list.map { if (it.id == account.id) account else it } }
    }
    override suspend fun delete(id: AccountId) {
        tombstoned.add(id.value)
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {
        idForSyncId(syncId)?.let { id ->
            tombstoned.add(id)
            updatedAtOverrides[id] = now
        }
    }

    override suspend fun reviveBySyncId(syncId: String, now: Long) {
        idForSyncId(syncId)?.let { id ->
            tombstoned.remove(id)
            updatedAtOverrides[id] = now
        }
    }

    override suspend fun deleteAll() {
        _accounts.value = emptyList()
        syncIds.clear()
        tombstoned.clear()
        updatedAtOverrides.clear()
    }

    override suspend fun exportForSync(): List<AccountSyncRow> =
        _accounts.value.map { a ->
            AccountSyncRow(
                id = a.id.value,
                syncId = syncIdFor(a.id.value),
                name = a.name,
                type = a.type.name,
                currency = a.currency.value,
                isDefault = a.isDefault,
                archived = a.archived,
                colorHex = a.colorHex,
                deleted = a.id.value in tombstoned,
                createdAt = a.createdAt.toEpochMilliseconds(),
                updatedAt = updatedAtOverrides[a.id.value] ?: a.updatedAt.toEpochMilliseconds(),
            )
        }

    override suspend fun upsertFromSync(row: AccountSyncRow): Long {
        val syncId = requireNotNull(row.syncId)
        val existingId = syncIds.entries.firstOrNull { it.value == syncId }?.key
        return if (existingId == null) {
            val id = nextId++
            _accounts.update { it + row.toDomain(id) }
            syncIds[id] = syncId
            id
        } else {
            _accounts.update { list -> list.map { if (it.id.value == existingId) row.toDomain(existingId) else it } }
            existingId
        }
    }

    private fun AccountSyncRow.toDomain(id: Long) = Account(
        id = AccountId(id),
        name = name,
        type = AccountType.valueOf(type),
        currency = CurrencyCode(currency),
        isDefault = isDefault,
        archived = archived,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        colorHex = colorHex,
    )

    private fun syncIdFor(id: Long): String = syncIds.getOrPut(id) { "sync-acc-$id" }

    private fun idForSyncId(syncId: String): Long? {
        _accounts.value.forEach { syncIdFor(it.id.value) }
        return syncIds.entries.firstOrNull { it.value == syncId }?.key
    }
}
