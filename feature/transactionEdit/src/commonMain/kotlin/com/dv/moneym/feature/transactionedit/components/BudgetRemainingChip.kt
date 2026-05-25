package com.dv.moneym.feature.transactionedit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.ui.MmBudgetProgressBar
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.feature.transactionedit.usecase.CategoryBudgetRemaining

@Composable
internal fun BudgetRemainingChip(
    remaining: CategoryBudgetRemaining,
    modifier: Modifier = Modifier,
) {
    MmCard(modifier = modifier, padded = true) {
        MmBudgetProgressBar(
            budgetName = remaining.budgetName,
            spentLabel = formatAmount(remaining.spent.minorUnits / 100.0, remaining.spent.currency.value),
            limitLabel = formatAmount(remaining.amount.minorUnits / 100.0, ""),
            remainingLabel = if (remaining.isOverrun)
                "${formatAmount(-remaining.remaining.minorUnits / 100.0, "")} over"
            else
                "${formatAmount(remaining.remaining.minorUnits / 100.0, "")} left",
            fraction = remaining.fraction,
            isOverrun = remaining.isOverrun,
        )
    }
}

private fun formatAmount(v: Double, currency: String): String {
    val cents = (kotlin.math.abs(v) * 100).toLong()
    val major = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    return if (currency.isNotEmpty()) "$currency $major.$frac" else "$major.$frac"
}
