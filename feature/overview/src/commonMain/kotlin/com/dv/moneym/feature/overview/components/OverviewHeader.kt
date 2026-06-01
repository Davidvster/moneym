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
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.WalletSelector
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.overview.OverviewPeriod
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_all
import moneym.feature.overview.generated.resources.overview_analyze_cd
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
    accounts: List<Account> = emptyList(),
    selectedAccountId: AccountId? = null,
    onAccountSelected: (AccountId) -> Unit = {},
    aiAvailable: Boolean = false,
    onAnalyzeClick: () -> Unit = {},
) {
    val colors = MM.colors
    val type = MM.type

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
            start = MM.dimen.padding_2x,
            end = MM.dimen.padding_2x,
            top = MM.dimen.padding_0_5x,
            bottom = MM.dimen.padding_1x,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.overview_title),
                style = type.title1,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            if (accounts.size > 1) {
                WalletSelector(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onSelect = onAccountSelected,
                    modifier = Modifier.padding(end = MM.dimen.padding_1x),
                )
            }
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
            size = MmSegmentedSize.Md,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth().padding(top = MM.dimen.padding_1x, bottom = MM.dimen.padding_0_5x),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = MM.dimen.padding_1x),
        ) {
            if (!isRangeMode && canGoBack) {
                MmIconButton(
                    icon = Icon.ChevronLeft.imageVector,
                    size = MM.dimen.padding_4x,
                    onClick = onPreviousPeriod,
                )
            } else {
                Spacer(Modifier.width(MM.dimen.padding_4x))
            }
            Box(
                modifier = Modifier
                    .widthIn(min = 96.dp)
                    .clip(MM.dimen.radius_1x)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (isRangeMode) onShowDateRangePicker() else onShowPeriodPicker()
                    }
                    .padding(horizontal = MM.dimen.padding_0_5x, vertical = MM.dimen.padding_0_25x),
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
                    size = MM.dimen.padding_4x,
                    onClick = onNextPeriod,
                )
            } else {
                Spacer(Modifier.width(MM.dimen.padding_4x))
            }
            if (aiAvailable) {
                Spacer(Modifier.weight(1f))
                MmIconButton(
                    icon = Icon.Sparkles.imageVector,
                    size = MM.dimen.padding_4x,
                    onClick = onAnalyzeClick,
                    variant = MmIconButtonVariant.Accent,
                    contentDescription = stringResource(Res.string.overview_analyze_cd),
                )
            }
        }
    }
}

@Preview
@Composable
private fun OverviewHeaderPreview() {
    MoneyMTheme {
        OverviewHeader(
            period = OverviewPeriod.Month(YearMonth(2024, 3)),
            periodLabel = "March 2024",
            spendingFilter = SpendingFilter.Expenses,
            onTogglePeriod = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onShowPeriodPicker = {},
            onShowDateRangePicker = {},
            onSpendingFilterChanged = {},
        )
    }
}
