package com.dv.moneym.data.transactions.internal

import com.dv.moneym.data.transactions.TransactionSyncRow
import com.dv.moneym.data.transactions.db.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class FakeTransactionLocalDataSource : TransactionLocalDataSource {

    val rows = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private var nextId = 1L

    private fun mutate(block: (List<TransactionEntity>) -> List<TransactionEntity>) {
        rows.value = block(rows.value)
    }

    private fun List<TransactionEntity>.sorted() =
        sortedWith(compareByDescending<TransactionEntity> { it.occurredOn }.thenByDescending { it.createdAt })

    override fun observeAll(): Flow<List<TransactionEntity>> =
        rows.map { list -> list.filter { !it.deleted }.sorted() }

    override fun observeByMonth(yearMonth: String): Flow<List<TransactionEntity>> =
        rows.map { list -> list.filter { !it.deleted && it.occurredOn.startsWith("$yearMonth-") }.sorted() }

    override fun observeByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        rows.map { list -> list.filter { !it.deleted && it.categoryId == categoryId }.sorted() }

    override fun observeByType(type: String): Flow<List<TransactionEntity>> =
        rows.map { list -> list.filter { !it.deleted && it.type == type }.sorted() }

    override fun observeByCategoryAndType(categoryId: Long, type: String): Flow<List<TransactionEntity>> =
        rows.map { list -> list.filter { !it.deleted && it.categoryId == categoryId && it.type == type }.sorted() }

    override suspend fun getById(id: Long): TransactionEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(
        type: String, amountMinor: Long, currency: String, occurredOn: String,
        note: String?, categoryId: Long, accountId: Long, createdAt: Long, updatedAt: Long,
        paymentModeId: Long?, recurringId: Long?,
    ): Long {
        val id = nextId++
        mutate {
            it + TransactionEntity(
                id = id,
                type = type,
                amountMinor = amountMinor,
                currency = currency,
                occurredOn = occurredOn,
                note = note,
                categoryId = categoryId,
                accountId = accountId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                paymentModeId = paymentModeId,
                recurringId = recurringId,
                syncId = Uuid.random().toString(),
            )
        }
        return id
    }

    override suspend fun update(
        id: Long, type: String, amountMinor: Long, currency: String,
        occurredOn: String, note: String?, categoryId: Long, accountId: Long, updatedAt: Long,
        paymentModeId: Long?, recurringId: Long?,
    ) {
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(
                    type = type,
                    amountMinor = amountMinor,
                    currency = currency,
                    occurredOn = occurredOn,
                    note = note,
                    categoryId = categoryId,
                    accountId = accountId,
                    updatedAt = updatedAt,
                    paymentModeId = paymentModeId,
                    recurringId = recurringId,
                ) else it
            }
        }
    }

    override suspend fun softDelete(id: Long, now: Long) {
        mutate { list -> list.map { if (it.id == id) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun softDeleteByAccountId(accountId: Long, now: Long) {
        mutate { list -> list.map { if (it.accountId == accountId) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun reassignCategory(from: Long, to: Long, now: Long) {
        mutate { list ->
            list.map { if (it.categoryId == from && !it.deleted) it.copy(categoryId = to, updatedAt = now) else it }
        }
    }

    override suspend fun softDeleteByCategory(categoryId: Long, now: Long) {
        mutate { list ->
            list.map { if (it.categoryId == categoryId && !it.deleted) it.copy(deleted = true, updatedAt = now) else it }
        }
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

    override suspend fun convertCurrencyForAccount(accountId: Long, currency: String, rate: Double, updatedAt: Long) {
        mutate { list ->
            list.map {
                if (it.accountId == accountId && !it.deleted) it.copy(
                    amountMinor = (it.amountMinor.toDouble() * rate).roundToLong(),
                    currency = currency,
                    updatedAt = updatedAt,
                ) else it
            }
        }
    }

    override suspend fun getEarliestDate(): String? =
        rows.value.filter { !it.deleted }.minOfOrNull { it.occurredOn }

    override suspend fun getLatestDate(): String? =
        rows.value.filter { !it.deleted }.maxOfOrNull { it.occurredOn }

    override fun getDistinctTransactionDates(): Flow<List<String>> =
        rows.map { list -> list.filter { !it.deleted }.map { it.occurredOn }.distinct().sorted() }

    override suspend fun countByRecurringId(recurringId: Long): Int =
        rows.value.count { !it.deleted && it.recurringId == recurringId }

    override suspend fun exportForSync(): List<TransactionEntity> = rows.value

    override suspend fun upsertFromSync(row: TransactionSyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = rows.value.firstOrNull { it.syncId == syncId }
        return if (existing == null) {
            val id = nextId++
            mutate {
                it + TransactionEntity(
                    id = id,
                    type = row.type,
                    amountMinor = row.amountMinor,
                    currency = row.currency,
                    occurredOn = row.occurredOn,
                    note = row.note,
                    categoryId = row.categoryId,
                    accountId = row.accountId,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    paymentModeId = row.paymentModeId,
                    recurringId = row.recurringId,
                    syncId = syncId,
                    deleted = row.deleted,
                    externalId = row.externalId,
                )
            }
            id
        } else {
            mutate { list ->
                list.map {
                    if (it.syncId == syncId) it.copy(
                        type = row.type,
                        amountMinor = row.amountMinor,
                        currency = row.currency,
                        occurredOn = row.occurredOn,
                        note = row.note,
                        categoryId = row.categoryId,
                        accountId = row.accountId,
                        paymentModeId = row.paymentModeId,
                        recurringId = row.recurringId,
                        updatedAt = row.updatedAt,
                        deleted = row.deleted,
                        externalId = row.externalId ?: it.externalId,
                    ) else it
                }
            }
            existing.id
        }
    }

    override suspend fun existsByExternalId(externalId: String): Boolean =
        rows.value.any { it.externalId == externalId }

    override suspend fun setExternalId(id: Long, externalId: String, now: Long) {
        mutate { list -> list.map { if (it.id == id) it.copy(externalId = externalId, updatedAt = now) else it } }
    }

    override suspend fun getByDateAndAmount(date: String, amountMinor: Long, currency: String): List<TransactionEntity> =
        rows.value.filter {
            !it.deleted && it.occurredOn == date && it.amountMinor == amountMinor && it.currency == currency
        }.sortedByDescending { it.createdAt }
}
