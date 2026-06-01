package com.dv.moneym.core.testing

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.budgets.BudgetSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeBudgetRepository : BudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    private var nextId = 1L

    val budgets: List<Budget> get() = _budgets.value

    fun addAll(budgets: List<Budget>) = _budgets.update { it + budgets }

    override fun observeAll(): Flow<List<Budget>> = _budgets

    override fun observeByAccount(accountId: AccountId): Flow<List<Budget>> =
        _budgets.map { list -> list.filter { it.accountId == accountId || it.accountId.value == 0L } }

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
        _budgets.update { list -> list.filter { it.id != id } }
    }

    override suspend fun exportForSync(): List<BudgetSyncRow> =
        _budgets.value.map { b ->
            BudgetSyncRow(
                id = b.id.value,
                syncId = "sync-budget-${b.id.value}",
                name = b.name,
                amountMinor = b.amount.minorUnits,
                currency = b.amount.currency.value,
                categoryId = b.categoryId?.value,
                accountId = b.accountId.value,
                periodType = b.periodType.name,
                startYearMonth = b.startYearMonth.toString(),
                recurringMonths = b.recurringMonths,
                deleted = false,
                createdAt = b.createdAt.toEpochMilliseconds(),
                updatedAt = b.updatedAt.toEpochMilliseconds(),
            )
        }
}
