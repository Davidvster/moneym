package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

data class CategoryBudgetRemaining(
    val budgetName: String,
    val amount: Money,
    val spent: Money,
    val remaining: Money,
    val fraction: Float,
    val isOverrun: Boolean,
)

class ComputeCategoryBudgetRemainingUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        categoryId: CategoryId,
        date: LocalDate,
        excludingTransactionId: TransactionId? = null,
    ): CategoryBudgetRemaining? {
        @Suppress("DEPRECATION")
        val ym = YearMonth(date.year, date.monthNumber)
        val budgets = budgetRepository.observeAll().first()
            .filter { it.isActiveIn(ym) }
        val matching = budgets.firstOrNull { it.categoryId == categoryId }
            ?: budgets.firstOrNull { it.categoryId == null }
            ?: return null

        @Suppress("DEPRECATION")
        val monthTxns = transactionRepository
            .observeByMonth(ym.year, ym.monthNumber)
            .first()
            .filter { it.type == TransactionType.EXPENSE }
            .filter { excludingTransactionId == null || it.id != excludingTransactionId }
            .filter { matching.categoryId == null || it.categoryId == matching.categoryId }

        val spentMinor = monthTxns.sumOf { it.amount.minorUnits }
        val cap = matching.amount.minorUnits
        val rawFraction = if (cap > 0) spentMinor.toFloat() / cap.toFloat() else 0f
        return CategoryBudgetRemaining(
            budgetName = matching.name,
            amount = matching.amount,
            spent = Money(spentMinor, matching.amount.currency),
            remaining = Money(cap - spentMinor, matching.amount.currency),
            fraction = rawFraction.coerceIn(0f, 1f),
            isOverrun = spentMinor > cap,
        )
    }
}
