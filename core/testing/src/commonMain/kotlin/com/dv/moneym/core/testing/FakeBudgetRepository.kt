package com.dv.moneym.core.testing

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.budgets.BudgetSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakeBudgetRepository : BudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    private var nextId = 1L

    private val syncIds = mutableMapOf<Long, String>()

    private val tombstoned = mutableSetOf<Long>()
    private val updatedAtOverrides = mutableMapOf<Long, Long>()

    val budgets: List<Budget> get() = _budgets.value.filter { it.id.value !in tombstoned }

    fun addAll(budgets: List<Budget>) = _budgets.update { it + budgets }

    override fun observeAll(): Flow<List<Budget>> =
        _budgets.map { list -> list.filter { it.id.value !in tombstoned } }

    override fun observeByAccount(accountId: AccountId): Flow<List<Budget>> =
        _budgets.map { list ->
            list.filter {
                (it.accountId == accountId || it.accountId.value == 0L) && it.id.value !in tombstoned
            }
        }

    override suspend fun getById(id: BudgetId): Budget? = _budgets.value.find { it.id == id }

    override suspend fun insert(budget: Budget): BudgetId {
        val id = BudgetId(nextId++)
        _budgets.update { it + budget.copy(id = id) }
        return id
    }

    override suspend fun update(budget: Budget) {
        _budgets.update { list -> list.map { if (it.id == budget.id) budget else it } }
    }

    override suspend fun delete(id: BudgetId) {
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

    override suspend fun exportForSync(): List<BudgetSyncRow> =
        _budgets.value.map { b ->
            BudgetSyncRow(
                id = b.id.value,
                syncId = syncIdFor(b.id.value),
                name = b.name,
                amountMinor = b.amount.minorUnits,
                currency = b.amount.currency.value,
                categoryId = b.categoryId?.value,
                accountId = b.accountId.value,
                periodType = b.periodType.name,
                startYearMonth = b.startYearMonth.toString(),
                recurringMonths = b.recurringMonths,
                deleted = b.id.value in tombstoned,
                createdAt = b.createdAt.toEpochMilliseconds(),
                updatedAt = updatedAtOverrides[b.id.value] ?: b.updatedAt.toEpochMilliseconds(),
            )
        }

    override suspend fun upsertFromSync(row: BudgetSyncRow): Long {
        val syncId = requireNotNull(row.syncId)
        val existingId = syncIds.entries.firstOrNull { it.value == syncId }?.key
        return if (existingId == null) {
            val id = nextId++
            _budgets.update { it + row.toDomain(id) }
            syncIds[id] = syncId
            id
        } else {
            _budgets.update { list -> list.map { if (it.id.value == existingId) row.toDomain(existingId) else it } }
            existingId
        }
    }

    private fun BudgetSyncRow.toDomain(id: Long) = Budget(
        id = BudgetId(id),
        name = name,
        amount = Money(amountMinor, CurrencyCode(currency)),
        categoryId = categoryId?.let { CategoryId(it) },
        accountId = AccountId(accountId),
        periodType = BudgetPeriodType.entries.firstOrNull { it.name == periodType } ?: BudgetPeriodType.MONTHLY,
        startYearMonth = parseYearMonth(startYearMonth),
        recurringMonths = recurringMonths,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

    private fun parseYearMonth(raw: String): YearMonth {
        val parts = raw.split('-')
        return YearMonth(parts[0].toInt(), parts[1].toInt())
    }

    private fun syncIdFor(id: Long): String = syncIds.getOrPut(id) { "sync-budget-$id" }

    private fun idForSyncId(syncId: String): Long? {
        _budgets.value.forEach { syncIdFor(it.id.value) }
        return syncIds.entries.firstOrNull { it.value == syncId }?.key
    }
}
