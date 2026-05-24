package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.overview.CategorySpend

class BuildCategoryBreakdownUseCase {

    internal operator fun invoke(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        type: TransactionType,
        elapsedDays: Int,
        elapsedMonths: Int,
        isMonthMode: Boolean,
    ): List<CategorySpend> {
        val typed = periodTxns.filter { it.type == type }
        val totalMinor = typed.sumOf { it.amount.minorUnits }
        val defaultColor = if (type == TransactionType.INCOME) "#4A7A56" else "#8A8A8A"
        return typed
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val amountMinor = txns.sumOf { it.amount.minorUnits }
                val amount = amountMinor.toDouble() / 100.0
                CategorySpend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: defaultColor),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    amount = amount,
                    percent = if (totalMinor > 0)
                        ((amountMinor.toDouble() / totalMinor.toDouble()) * 100).toInt()
                    else 0,
                    avgPerDay = amount / elapsedDays,
                    avgPerMonth = if (isMonthMode) 0.0 else amount / elapsedMonths,
                )
            }
            .sortedByDescending { it.amount }
    }
}
