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
) {
    val useSymbol = LocalUseCurrencySymbol.current
    val displayCurrency = currencyDisplay(remaining.spent.currency.value, useSymbol)
    MmCard(modifier = modifier, padded = true) {
        MmBudgetProgressBar(
            budgetName = remaining.budgetName,
            spentLabel = formatAmount(remaining.spent.minorUnits / 100.0, displayCurrency),
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
        )
    }
}
