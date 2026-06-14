package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.currencyDisplay
import com.dv.moneym.core.ui.LocalUseCurrencySymbol
import com.dv.moneym.core.ui.MmBudgetProgressBar
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.feature.transactionedit.usecase.CategoryBudgetRemaining

@Composable
internal fun BudgetRemainingChip(
    remaining: CategoryBudgetRemaining,
    modifier: Modifier = Modifier,
    projected: CategoryBudgetRemaining? = null,
) {
    val useSymbol = LocalUseCurrencySymbol.current
    val displayCurrency = currencyDisplay(remaining.spent.currency.value, useSymbol)
    MmCard(modifier = modifier, padded = true) {
        MmBudgetProgressBar(
            budgetName = remaining.budgetName,
            spentLabel = formatAmount(remaining.spent.minorUnits / 100.0, displayCurrency),
            limitLabel = formatAmount(remaining.amount.minorUnits / 100.0, ""),
            remainingLabel = remainingLabel(remaining),
            fraction = remaining.fraction,
            isOverrun = remaining.isOverrun,
            percentLabel = percentLabel(remaining),
            projectedSpentLabel = projected?.let {
                formatAmount(it.spent.minorUnits / 100.0, displayCurrency)
            },
            projectedRemainingLabel = projected?.let { remainingLabel(it) },
            projectedPercentLabel = projected?.let { percentLabel(it) },
            projectedFraction = projected?.fraction,
            projectedIsOverrun = projected?.isOverrun ?: false,
        )
    }
}

private fun remainingLabel(b: CategoryBudgetRemaining): String =
    if (b.isOverrun)
        "${formatAmount(-b.remaining.minorUnits / 100.0, "")} over"
    else
        "${formatAmount(b.remaining.minorUnits / 100.0, "")} left"

private fun percentLabel(b: CategoryBudgetRemaining): String {
    val cap = b.amount.minorUnits
    val pct = if (cap > 0) (b.spent.minorUnits * 100.0 / cap).toInt() else 0
    return "$pct%"
}

private fun formatAmount(v: Double, currency: String): String {
    val cents = (kotlin.math.abs(v) * 100).toLong()
    val major = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    return if (currency.isNotEmpty()) "$currency $major.$frac" else "$major.$frac"
}

@Preview
@Composable
private fun BudgetRemainingChipPreview() {
    val eur = CurrencyCode("EUR")
    MoneyMTheme {
        BudgetRemainingChip(
            remaining = CategoryBudgetRemaining(
                budgetName = "Groceries",
                amount = Money(50000, eur),
                spent = Money(32000, eur),
                remaining = Money(18000, eur),
                fraction = 0.64f,
                isOverrun = false,
            ),
            modifier = Modifier.fillMaxWidth(),
            projected = CategoryBudgetRemaining(
                budgetName = "Groceries",
                amount = Money(50000, eur),
                spent = Money(41000, eur),
                remaining = Money(9000, eur),
                fraction = 0.82f,
                isOverrun = false,
            ),
        )
    }
}
