package com.dv.moneym.feature.overview.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.SpendingFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.SectionLabel
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_label_balance
import moneym.feature.overview.generated.resources.overview_label_expenses
import moneym.feature.overview.generated.resources.overview_label_income
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IncomeExpensesCard(
    income: Double,
    expenses: Double,
    currencyCode: String,
    filter: SpendingFilter = SpendingFilter.All,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(
        modifier = modifier,
        padded = true,
        shape = MM.dimen.radius_1_5x,
    ) {
        when (filter) {
            SpendingFilter.Income -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Icon(
                        imageVector = Icon.ArrowDown.imageVector,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(MM.dimen.padding_1_5x),
                    )
                    SectionLabel(
                        text = stringResource(Res.string.overview_label_income),
                        modifier = Modifier.weight(1f),
                    )
                    MmMoney(
                        value = income,
                        style = MM.type.amountLarge,
                        color = colors.accent,
                        currency = currencyCode,
                    )
                }
            }
            SpendingFilter.Expenses -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Icon(
                        imageVector = Icon.ArrowUp.imageVector,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(MM.dimen.padding_1_5x),
                    )
                    SectionLabel(
                        text = stringResource(Res.string.overview_label_expenses),
                        modifier = Modifier.weight(1f),
                    )
                    MmMoney(
                        value = expenses,
                        style = MM.type.amountLarge,
                        currency = currencyCode,
                    )
                }
            }
            SpendingFilter.All -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Icon(
                        imageVector = Icon.ArrowDown.imageVector,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(MM.dimen.padding_1_5x),
                    )
                    SectionLabel(
                        text = stringResource(Res.string.overview_label_income),
                        modifier = Modifier.weight(1f),
                    )
                    MmMoney(
                        value = income,
                        style = MM.type.amountLarge,
                        color = colors.accent,
                        currency = currencyCode,
                    )
                }
                Spacer(Modifier.height(space.padding_1_25x))
                HorizontalDivider(color = colors.divider, thickness = MM.dimen.strokeHairline)
                Spacer(Modifier.height(space.padding_1_25x))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Icon(
                        imageVector = Icon.ArrowUp.imageVector,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(MM.dimen.padding_1_5x),
                    )
                    SectionLabel(
                        text = stringResource(Res.string.overview_label_expenses),
                        modifier = Modifier.weight(1f),
                    )
                    MmMoney(
                        value = expenses,
                        style = MM.type.amountLarge,
                        currency = currencyCode,
                    )
                }
                Spacer(Modifier.height(space.padding_1_25x))
                HorizontalDivider(color = colors.divider, thickness = MM.dimen.strokeHairline)
                Spacer(Modifier.height(space.padding_1_25x))
                val balance = income - expenses
                val balanceColor = if (balance >= 0) colors.accent else colors.danger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    SectionLabel(
                        text = stringResource(Res.string.overview_label_balance),
                        modifier = Modifier.weight(1f),
                    )
                    MmMoney(
                        value = kotlin.math.abs(balance),
                        style = MM.type.amountLarge,
                        color = balanceColor,
                        currency = currencyCode,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun IncomeExpensesCardPreview() {
    MoneyMTheme {
        IncomeExpensesCard(
            income = 2500.0,
            expenses = 1850.0,
            currencyCode = "EUR",
            filter = SpendingFilter.All,
        )
    }
}
