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
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
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
    val barColor = if (p.isOverrun) colors.danger
    else p.categoryColor?.let { Color(it) } ?: colors.accent
    val trackColor = colors.divider

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.categoryColor?.let { Color(it) } ?: colors.text3),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(p.name, style = type.body, color = colors.text)
                Text(
                    text = p.categoryName ?: allCategoriesLabel,
                    style = type.caption.copy(color = colors.text2),
                )
            }
            MmMoney(
                value = p.spent.minorUnits / 100.0,
                currency = p.spent.currency.value,
                color = if (p.isOverrun) colors.danger else colors.text,
            )
            Text(
                text = " / ",
                style = type.captionMono.copy(color = colors.text3),
            )
            MmMoney(
                value = p.amount.minorUnits / 100.0,
                currency = "",
                color = colors.text2,
            )
        }
        Spacer(Modifier.height(MM.dimen.padding_1x))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(p.fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor),
            )
        }
        if (p.remaining.minorUnits != 0L) {
            Text(
                text = if (p.isOverrun)
                    stringResource(
                        Res.string.overview_budgets_overrun_suffix,
                        (-p.remaining.minorUnits) / 100.0,
                    )
                else
                    stringResource(
                        Res.string.overview_budgets_remaining_suffix,
                        p.remaining.minorUnits / 100.0,
                    ),
                style = type.caption.copy(
                    color = if (p.isOverrun) colors.danger else colors.text2,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
