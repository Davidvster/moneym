package com.dv.moneym.data.budgets.internal

import com.dv.moneym.data.budgets.BudgetSyncRow
import com.dv.moneym.data.budgets.db.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class FakeBudgetLocalDataSource : BudgetLocalDataSource {

    val rows = MutableStateFlow<List<BudgetEntity>>(emptyList())
    private var nextId = 1L

    private fun mutate(block: (List<BudgetEntity>) -> List<BudgetEntity>) {
        rows.value = block(rows.value)
    }

    override fun observeAll(): Flow<List<BudgetEntity>> =
        rows.map { list -> list.filter { !it.deleted }.sortedByDescending { it.createdAt } }

    override fun observeByAccount(accountId: Long): Flow<List<BudgetEntity>> =
        rows.map { list ->
            list.filter { !it.deleted && (it.accountId == accountId || it.accountId == 0L) }
                .sortedByDescending { it.createdAt }
        }

    override suspend fun getById(id: Long): BudgetEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        periodType: String,
        startYearMonth: String,
        recurringMonths: Int?,
        createdAt: Long,
        updatedAt: Long,
    ): Long {
        val id = nextId++
        mutate {
            it + BudgetEntity(
                id = id,
                name = name,
                amountMinor = amountMinor,
                currency = currency,
                categoryId = categoryId,
                accountId = accountId,
                periodType = periodType,
                startYearMonth = startYearMonth,
                recurringMonths = recurringMonths,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncId = Uuid.random().toString(),
            )
        }
        return id
    }

    override suspend fun update(
        id: Long,
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        periodType: String,
        startYearMonth: String,
        recurringMonths: Int?,
        updatedAt: Long,
    ) {
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(
                    name = name,
                    amountMinor = amountMinor,
                    currency = currency,
                    categoryId = categoryId,
                    accountId = accountId,
                    periodType = periodType,
                    startYearMonth = startYearMonth,
                    recurringMonths = recurringMonths,
                    updatedAt = updatedAt,
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

    override suspend fun exportForSync(): List<BudgetEntity> = rows.value

    override suspend fun upsertFromSync(row: BudgetSyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = rows.value.firstOrNull { it.syncId == syncId }
        return if (existing == null) {
            val id = nextId++
            mutate {
                it + BudgetEntity(
                    id = id,
                    name = row.name,
                    amountMinor = row.amountMinor,
                    currency = row.currency,
                    categoryId = row.categoryId,
                    accountId = row.accountId,
                    periodType = row.periodType,
                    startYearMonth = row.startYearMonth,
                    recurringMonths = row.recurringMonths,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
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
                        amountMinor = row.amountMinor,
                        currency = row.currency,
                        categoryId = row.categoryId,
                        accountId = row.accountId,
                        periodType = row.periodType,
                        startYearMonth = row.startYearMonth,
                        recurringMonths = row.recurringMonths,
                        updatedAt = row.updatedAt,
                        deleted = row.deleted,
                    ) else it
                }
            }
            existing.id
        }
    }
}
