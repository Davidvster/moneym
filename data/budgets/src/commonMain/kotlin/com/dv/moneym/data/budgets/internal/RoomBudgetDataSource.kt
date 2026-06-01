package com.dv.moneym.data.budgets.internal

import com.dv.moneym.data.budgets.db.BudgetEntity
import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal class RoomBudgetDataSource(
    private val db: BudgetsRoomDatabase,
) : BudgetLocalDataSource {

    private val dao get() = db.budgetDao()

    override fun observeAll(): Flow<List<BudgetEntity>> = dao.selectAll()

    override fun observeByAccount(accountId: Long): Flow<List<BudgetEntity>> =
        dao.selectByAccount(accountId)

    override suspend fun getById(id: Long): BudgetEntity? = dao.selectById(id)

    @OptIn(ExperimentalUuidApi::class)
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
    ): Long = dao.insert(
        BudgetEntity(
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
    )

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
        val existing = dao.selectById(id) ?: return
        dao.update(
            existing.copy(
                name = name,
                amountMinor = amountMinor,
                currency = currency,
                categoryId = categoryId,
                accountId = accountId,
                periodType = periodType,
                startYearMonth = startYearMonth,
                recurringMonths = recurringMonths,
                updatedAt = updatedAt,
            )
        )
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
