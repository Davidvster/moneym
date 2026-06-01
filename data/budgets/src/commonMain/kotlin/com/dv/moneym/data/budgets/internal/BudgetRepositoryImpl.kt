package com.dv.moneym.data.budgets.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.budgets.BudgetSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class BudgetRepositoryImpl(
    private val dataSource: BudgetLocalDataSource,
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> =
        dataSource.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByAccount(accountId: AccountId): Flow<List<Budget>> =
        dataSource.observeByAccount(accountId.value).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: BudgetId): Budget? =
        dataSource.getById(id.value)?.toDomain()

    override suspend fun insert(budget: Budget): BudgetId {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dataSource.insert(
            name = budget.name,
            amountMinor = budget.amount.minorUnits,
            currency = budget.amount.currency.value,
            categoryId = budget.categoryId?.value,
            accountId = budget.accountId.value,
            periodType = budget.periodType.name,
            startYearMonth = budget.startYearMonth.toString(),
            recurringMonths = budget.recurringMonths,
            createdAt = now,
            updatedAt = now,
        )
        return BudgetId(id)
    }

    override suspend fun update(budget: Budget) {
        val now = Clock.System.now().toEpochMilliseconds()
        dataSource.update(
            id = budget.id.value,
            name = budget.name,
            amountMinor = budget.amount.minorUnits,
            currency = budget.amount.currency.value,
            categoryId = budget.categoryId?.value,
            accountId = budget.accountId.value,
            periodType = budget.periodType.name,
            startYearMonth = budget.startYearMonth.toString(),
            recurringMonths = budget.recurringMonths,
            updatedAt = now,
        )
    }

    override suspend fun delete(id: BudgetId) = dataSource.delete(id.value)

    override suspend fun exportForSync(): List<BudgetSyncRow> =
        dataSource.exportForSync().map { it.toSyncRow() }
}
