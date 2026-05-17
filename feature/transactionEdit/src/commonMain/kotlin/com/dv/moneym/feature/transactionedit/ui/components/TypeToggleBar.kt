package com.dv.moneym.feature.transactionedit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
internal fun TypeToggleBar(
    isExpense: Boolean,
    expenseLabel: String,
    incomeLabel: String,
    onExpenseSelected: () -> Unit,
    onIncomeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen

    val expenseActiveColor = colors.danger
    val incomeActiveColor = colors.accent
    val inactiveBg = colors.surface2
    val inactiveFg = colors.text2

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(radius.radius_2_5x)
            .background(inactiveBg),
    ) {
        // Expense tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(radius.radius_2_5x)
                .background(if (isExpense) expenseActiveColor else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpenseSelected() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = expenseLabel,
                style = type.caption.copy(
                    fontWeight = if (isExpense) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isExpense) Color.White else inactiveFg,
                ),
            )
        }
        // Income tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(radius.radius_2_5x)
                .background(if (!isExpense) incomeActiveColor else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onIncomeSelected() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = incomeLabel,
                style = type.caption.copy(
                    fontWeight = if (!isExpense) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (!isExpense) Color.White else inactiveFg,
                ),
            )
        }
    }
}
