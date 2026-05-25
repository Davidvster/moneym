package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.overview.OverviewPeriod

data class BudgetProgress(
    val budgetId: Long,
    val name: String,
    val amount: Money,
    val spent: Money,
    val remaining: Money,
    val fraction: Float,
    val isOverrun: Boolean,
    val categoryName: String?,
    val categoryColor: Long?,
)

class BuildBudgetProgressUseCase {
    operator fun invoke(
        budgets: List<Budget>,
        periodTxns: List<Transaction>,
        period: OverviewPeriod,
        catMap: Map<CategoryId, Category>,
    ): List<BudgetProgress> {
        if (period !is OverviewPeriod.Month) return emptyList()
        val ym = period.yearMonth
        val active = budgets.filter { it.isActiveIn(ym) }
        if (active.isEmpty()) return emptyList()
        val expenses = periodTxns.filter { it.type == TransactionType.EXPENSE }
        return active
            .map { it.toProgress(expenses, catMap) }
            .sortedByDescending { it.fraction }
    }

    private fun Budget.toProgress(
        expenses: List<Transaction>,
        catMap: Map<CategoryId, Category>,
    ): BudgetProgress {
        val matching = if (categoryId == null) expenses
        else expenses.filter { it.categoryId == categoryId }
        val spentMinor = matching.sumOf { it.amount.minorUnits }
        val cap = amount.minorUnits
        val cat = categoryId?.let { catMap[it] }
        val rawFraction = if (cap > 0) spentMinor.toFloat() / cap.toFloat() else 0f
        val isOverrun = spentMinor > cap
        return BudgetProgress(
            budgetId = id.value,
            name = name,
            amount = amount,
            spent = Money(spentMinor, amount.currency),
            remaining = Money(cap - spentMinor, amount.currency),
            fraction = rawFraction.coerceIn(0f, 1f),
            isOverrun = isOverrun,
            categoryName = cat?.name,
            categoryColor = cat?.colorHex?.let { hexToLong(it) },
        )
    }

    private fun hexToLong(hex: String): Long? = try {
        val s = hex.trimStart('#')
        when (s.length) {
            6 -> ("FF$s").toLong(16)
            8 -> s.toLong(16)
            else -> null
        }
    } catch (_: NumberFormatException) {
        null
    }
}

