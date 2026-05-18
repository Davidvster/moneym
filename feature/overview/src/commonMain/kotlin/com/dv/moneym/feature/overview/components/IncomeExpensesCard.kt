package com.dv.moneym.feature.overview.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.SectionLabel
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_label_expenses
import moneym.feature.overview.generated.resources.overview_label_income
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IncomeExpensesCard(
    income: Double,
    expenses: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(
        modifier = modifier,
        padded = true,
        shape = MM.dimen.radius_1_5x,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                size = 17.sp,
                weight = FontWeight.SemiBold,
                color = colors.accent,
                currency = currencyCode,
            )
        }
        Spacer(Modifier.height(space.padding_1_25x))
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.height(space.padding_1_25x))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                size = 17.sp,
                weight = FontWeight.SemiBold,
                currency = currencyCode,
            )
        }
    }
}
