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
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmBudgetProgressBar(
    budgetName: String,
    spentLabel: String,
    limitLabel: String,
    remainingLabel: String,
    fraction: Float,
    isOverrun: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val barColor = if (isOverrun) colors.danger else colors.accent
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
            Text(
                text = spentLabel,
                style = type.captionMono,
                color = if (isOverrun) colors.danger else colors.text,
                softWrap = false,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = remainingLabel,
            style = type.caption.copy(color = if (isOverrun) colors.danger else colors.text2),
        )
    }
}
