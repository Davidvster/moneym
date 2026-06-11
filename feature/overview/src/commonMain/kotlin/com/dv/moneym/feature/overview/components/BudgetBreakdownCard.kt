package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.common.formatNumber
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.ui.MmBudgetProgressBar
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.feature.overview.usecase.BudgetProgress
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_budgets_all_categories
import moneym.feature.overview.generated.resources.overview_budgets_overrun_suffix
import moneym.feature.overview.generated.resources.overview_budgets_remaining_suffix
import moneym.feature.overview.generated.resources.overview_budgets_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BudgetBreakdownCard(
    progress: List<BudgetProgress>,
    modifier: Modifier = Modifier,
) {
    if (progress.isEmpty()) return
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    MmCard(modifier = modifier, padded = true, shape = MM.dimen.radius_2x) {
        Text(
            text = stringResource(Res.string.overview_budgets_title),
            style = type.title3,
            color = colors.text,
        )
        Spacer(Modifier.height(space.padding_2x))
        Column(verticalArrangement = Arrangement.spacedBy(space.padding_2x)) {
            progress.forEach { p -> BudgetProgressRow(p) }
        }
    }
}

@Composable
private fun BudgetProgressRow(p: BudgetProgress) {
    val colors = MM.colors
    val type = MM.type
    val allCategoriesLabel = stringResource(Res.string.overview_budgets_all_categories)
    val remainingLabel = if (p.remaining.minorUnits == 0L) ""
    else if (p.isOverrun)
        stringResource(Res.string.overview_budgets_overrun_suffix, formatNumber(-p.remaining.minorUnits / 100.0, 2))
    else
        stringResource(Res.string.overview_budgets_remaining_suffix, formatNumber(p.remaining.minorUnits / 100.0, 2))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MM.dimen.padding_0_5x),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Box(
                modifier = Modifier
                    .size(MM.dimen.padding_1x)
                    .clip(RoundedCornerShape(MM.dimen.padding_0_5x))
                    .background(p.categoryColor?.let { Color(it) } ?: colors.text3),
            )
            Text(
                text = p.categoryName ?: allCategoriesLabel,
                style = type.caption.copy(color = colors.text2),
                modifier = Modifier.weight(1f),
            )
        }
        MmBudgetProgressBar(
            budgetName = p.name,
            spentLabel = formatBudgetAmount(p.spent.minorUnits / 100.0, p.spent.currency.value),
            limitLabel = formatBudgetAmount(p.amount.minorUnits / 100.0, ""),
            remainingLabel = remainingLabel,
            fraction = p.fraction,
            isOverrun = p.isOverrun,
        )
    }
}

private fun formatBudgetAmount(v: Double, currency: String): String {
    val formatted = formatNumber(v, 2)
    return if (currency.isNotEmpty()) "$currency $formatted" else formatted
}

@Preview
@Composable
private fun BudgetBreakdownCardPreview() {
    val eur = CurrencyCode("EUR")
    MoneyMTheme {
        BudgetBreakdownCard(
            progress = listOf(
                BudgetProgress(
                    budgetId = 1L,
                    name = "Groceries",
                    amount = Money(40000L, eur),
                    spent = Money(31250L, eur),
                    remaining = Money(8750L, eur),
                    fraction = 0.78f,
                    isOverrun = false,
                    categoryName = "Groceries",
                    categoryColor = 0xFF4CAF50,
                ),
                BudgetProgress(
                    budgetId = 2L,
                    name = "Eating out",
                    amount = Money(15000L, eur),
                    spent = Money(18900L, eur),
                    remaining = Money(-3900L, eur),
                    fraction = 1f,
                    isOverrun = true,
                    categoryName = "Restaurants",
                    categoryColor = 0xFFFF7043,
                ),
                BudgetProgress(
                    budgetId = 3L,
                    name = "Everything else",
                    amount = Money(100000L, eur),
                    spent = Money(46200L, eur),
                    remaining = Money(53800L, eur),
                    fraction = 0.46f,
                    isOverrun = false,
                    categoryName = null,
                    categoryColor = null,
                ),
            ),
        )
    }
}
