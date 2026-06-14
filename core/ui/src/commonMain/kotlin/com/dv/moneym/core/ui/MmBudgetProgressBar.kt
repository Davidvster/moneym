package com.dv.moneym.core.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmBudgetProgressBar(
    budgetName: String,
    spentLabel: String,
    limitLabel: String,
    remainingLabel: String,
    fraction: Float,
    isOverrun: Boolean,
    modifier: Modifier = Modifier,
    percentLabel: String? = null,
    projectedSpentLabel: String? = null,
    projectedRemainingLabel: String? = null,
    projectedPercentLabel: String? = null,
    projectedFraction: Float? = null,
    projectedIsOverrun: Boolean = false,
) {
    val colors = MM.colors
    val type = MM.type
    val barColor = if (isOverrun) colors.danger else colors.accent
    val changeColor = if (projectedIsOverrun) colors.danger else colors.warning
    val trackColor = colors.divider

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Text(
                text = budgetName,
                style = type.caption,
                color = colors.text2,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MM.dimen.padding_1x),
            )
            OldNewValue(
                old = spentLabel,
                new = projectedSpentLabel,
                style = type.captionMono,
                oldColor = if (isOverrun) colors.danger else colors.text,
                newColor = if (projectedIsOverrun) colors.danger else colors.text,
                arrowColor = colors.text3,
            )
            Text(
                text = " / ",
                style = type.captionMono,
                color = colors.text3,
            )
            Text(
                text = limitLabel,
                style = type.captionMono,
                color = colors.text2,
                softWrap = false,
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
            if (projectedFraction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(projectedFraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(changeColor),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OldNewValue(
                old = remainingLabel,
                new = projectedRemainingLabel,
                style = type.caption,
                oldColor = if (isOverrun) colors.danger else colors.text2,
                newColor = if (projectedIsOverrun) colors.danger else colors.text2,
                arrowColor = colors.text3,
                modifier = Modifier.weight(1f),
            )
            if (percentLabel != null) {
                OldNewValue(
                    old = percentLabel,
                    new = projectedPercentLabel,
                    style = type.captionMono,
                    oldColor = if (isOverrun) colors.danger else colors.text3,
                    newColor = if (projectedIsOverrun) colors.danger else colors.text2,
                    arrowColor = colors.text3,
                )
            }
        }
    }
}

@Composable
private fun OldNewValue(
    old: String,
    new: String?,
    style: TextStyle,
    oldColor: Color,
    newColor: Color,
    arrowColor: Color,
    modifier: Modifier = Modifier,
) {
    if (new == null) {
        Text(text = old, style = style, color = oldColor, softWrap = false, modifier = modifier)
        return
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = old,
            style = style.copy(textDecoration = TextDecoration.LineThrough),
            color = oldColor.copy(alpha = 0.6f),
            softWrap = false,
        )
        Text(text = "→", style = style, color = arrowColor, softWrap = false)
        Text(text = new, style = style, color = newColor, softWrap = false)
    }
}

@Preview
@Composable
private fun MmBudgetProgressBarPreview() {
    MoneyMTheme {
        Column(
            Modifier.padding(MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            MmBudgetProgressBar(
                budgetName = "Groceries",
                spentLabel = "€ 180",
                limitLabel = "€ 400",
                remainingLabel = "€ 220 left",
                fraction = 0.45f,
                isOverrun = false,
            )
            MmBudgetProgressBar(
                budgetName = "Groceries",
                spentLabel = "€ 180",
                limitLabel = "€ 400",
                remainingLabel = "€ 220 left",
                fraction = 0.45f,
                isOverrun = false,
                percentLabel = "45%",
                projectedSpentLabel = "€ 240",
                projectedRemainingLabel = "€ 160 left",
                projectedPercentLabel = "60%",
                projectedFraction = 0.60f,
                projectedIsOverrun = false,
            )
            MmBudgetProgressBar(
                budgetName = "Eating Out",
                spentLabel = "€ 230",
                limitLabel = "€ 250",
                remainingLabel = "€ 20 left",
                fraction = 0.92f,
                isOverrun = false,
                percentLabel = "92%",
                projectedSpentLabel = "€ 320",
                projectedRemainingLabel = "€ 70 over",
                projectedPercentLabel = "128%",
                projectedFraction = 1f,
                projectedIsOverrun = true,
            )
        }
    }
}
