package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.feature.transactionedit.usecase.CategoryBudgetRemaining

@Composable
internal fun BudgetRemainingChip(
    remaining: CategoryBudgetRemaining,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val barColor = if (remaining.isOverrun) colors.danger else colors.accent
    val trackColor = colors.divider

    MmCard(modifier = modifier, padded = true) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                Text(
                    text = remaining.budgetName,
                    style = type.caption,
                    color = colors.text2,
                    modifier = Modifier.padding(end = MM.dimen.padding_1x),
                )
                MmMoney(
                    value = remaining.spent.minorUnits / 100.0,
                    currency = remaining.spent.currency.value,
                    color = if (remaining.isOverrun) colors.danger else colors.text,
                )
                Text(
                    text = " / ",
                    style = type.captionMono.copy(color = colors.text3),
                )
                MmMoney(
                    value = remaining.amount.minorUnits / 100.0,
                    currency = "",
                    color = colors.text2,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(remaining.fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (remaining.isOverrun)
                    "${formatAmount(-remaining.remaining.minorUnits / 100.0)} over"
                else
                    "${formatAmount(remaining.remaining.minorUnits / 100.0)} left",
                style = type.caption.copy(
                    color = if (remaining.isOverrun) colors.danger else colors.text2,
                ),
            )
        }
    }
}

private fun formatAmount(v: Double): String {
    val cents = (v * 100).toLong()
    val major = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    return "$major.$frac"
}
