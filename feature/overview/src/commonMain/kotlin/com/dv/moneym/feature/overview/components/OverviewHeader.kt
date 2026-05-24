package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.SpendingFilter
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_all
import moneym.feature.overview.generated.resources.overview_expenses
import moneym.feature.overview.generated.resources.overview_income
import moneym.feature.overview.generated.resources.overview_period_custom
import moneym.feature.overview.generated.resources.overview_period_month
import moneym.feature.overview.generated.resources.overview_period_year
import moneym.feature.overview.generated.resources.overview_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OverviewHeader(
    period: OverviewPeriod,
    periodLabel: String,
    spendingFilter: SpendingFilter,
    onTogglePeriod: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onShowPeriodPicker: () -> Unit,
    onShowDateRangePicker: () -> Unit,
    onSpendingFilterChanged: (SpendingFilter) -> Unit,
    canGoBack: Boolean = true,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    val isMonthMode = period is OverviewPeriod.Month
    val isYearMode = period is OverviewPeriod.Year
    val isRangeMode = period is OverviewPeriod.DateRange

    val segmentIndex = when {
        isMonthMode -> 0
        isYearMode -> 1
        else -> 2
    }

    Column(
        Modifier.statusBarsPadding().padding(
            start = space.padding_2x,
            end = space.padding_2x,
            top = space.padding_0_5x,
            bottom = space.padding_1x,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.overview_title),
                style = type.title1,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            MmSegmented(
                options = listOf(
                    stringResource(Res.string.overview_period_month),
                    stringResource(Res.string.overview_period_year),
                    stringResource(Res.string.overview_period_custom),
                ),
                selectedIndex = segmentIndex,
                onOptionSelected = { idx ->
                    when (idx) {
                        0 -> { if (!isMonthMode) onTogglePeriod() }
                        1 -> { if (!isYearMode) onTogglePeriod() }
                        2 -> { onShowDateRangePicker() }
                    }
                },
            )
        }

        // All / Expenses / Income filter — top of screen
        MmSegmented(
            options = listOf(
                stringResource(Res.string.overview_all),
                stringResource(Res.string.overview_expenses),
                stringResource(Res.string.overview_income),
            ),
            selectedIndex = when (spendingFilter) {
                SpendingFilter.All -> 0
                SpendingFilter.Expenses -> 1
                SpendingFilter.Income -> 2
            },
            onOptionSelected = { idx ->
                onSpendingFilterChanged(
                    when (idx) {
                        0 -> SpendingFilter.All
                        1 -> SpendingFilter.Expenses
                        else -> SpendingFilter.Income
                    }
                )
            },
            size = MmSegmentedSize.Sm,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = space.padding_1x),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = space.padding_2x),
        ) {
            if (!isRangeMode && canGoBack) {
                MmIconButton(
                    icon = Icon.ChevronLeft.imageVector,
                    size = space.padding_4x,
                    onClick = onPreviousPeriod,
                )
            } else {
                Spacer(Modifier.width(space.padding_4x))
            }
            Box(
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .clip(space.radius_1x)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (isRangeMode) onShowDateRangePicker() else onShowPeriodPicker()
                    }
                    .padding(horizontal = space.padding_0_5x, vertical = space.padding_0_25x),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = periodLabel,
                    style = type.body,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
            }
            if (!isRangeMode) {
                MmIconButton(
                    icon = Icon.ChevronRight.imageVector,
                    size = space.padding_4x,
                    onClick = onNextPeriod,
                )
            } else {
                Spacer(Modifier.width(space.padding_4x))
            }
        }
    }
}
