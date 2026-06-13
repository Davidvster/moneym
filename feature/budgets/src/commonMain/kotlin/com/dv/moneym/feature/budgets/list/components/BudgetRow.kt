package com.dv.moneym.feature.budgets.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.budgets.list.BudgetRowVm
import moneym.feature.budgets.generated.resources.Res
import moneym.feature.budgets.generated.resources.budgets_delete
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BudgetRow(
    row: BudgetRowVm,
    scopeLabel: String,
    recurringLabel: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    MmRow(onClick = onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, style = type.body, color = colors.text)
            Row(
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(scopeLabel, style = type.caption.copy(color = colors.text2))
                if (recurringLabel != null) {
                    Text("· $recurringLabel", style = type.caption.copy(color = colors.text2))
                }
            }
        }
        MmMoney(
            value = row.amount.minorUnits / 100.0,
            currency = row.amount.currency.value,
            color = colors.text,
        )
        MmIconButton(
            icon = Icon.Trash.imageVector,
            onClick = onDelete,
            variant = MmIconButtonVariant.Danger,
            contentDescription = stringResource(Res.string.budgets_delete),
        )
    }
}

@Preview
@Composable
private fun BudgetRowPreview() {
    MoneyMTheme {
        BudgetRow(
            row = BudgetRowVm(
                id = BudgetId(1L),
                name = "Groceries",
                amount = Money(70000L, CurrencyCode("EUR")),
                scopeLabel = "Groceries",
                recurringLabel = "Monthly",
            ),
            scopeLabel = "Groceries",
            recurringLabel = "Monthly",
            onClick = {},
            onDelete = {},
        )
    }
}
