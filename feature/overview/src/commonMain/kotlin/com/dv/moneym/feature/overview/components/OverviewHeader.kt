package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.selectedType
import com.dv.moneym.core.model.withType
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.WalletSelector
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.overview.OverviewPeriod
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_all
import moneym.feature.overview.generated.resources.overview_analyze_cd
import moneym.feature.overview.generated.resources.overview_customize_cd
import moneym.feature.overview.generated.resources.overview_expenses
import moneym.feature.overview.generated.resources.overview_income
import moneym.feature.overview.generated.resources.overview_period_custom
import moneym.feature.overview.generated.resources.overview_period_month
import moneym.feature.overview.generated.resources.overview_period_year
import moneym.feature.overview.generated.resources.overview_spending_by_category
import moneym.feature.overview.generated.resources.overview_title
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun OverviewHeader(
    period: OverviewPeriod,
    periodLabel: String,
    transactionFilter: TransactionFilter,
    onTogglePeriod: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onShowPeriodPicker: () -> Unit,
    onShowDateRangePicker: () -> Unit,
    onShowCategoryFilter: () -> Unit,
    onTransactionFilterChanged: (TransactionFilter) -> Unit,
    canGoBack: Boolean = true,
    availableCategories: List<Category> = emptyList(),
    selectedCategoryIds: Set<CategoryId> = emptySet(),
    accounts: List<Account> = emptyList(),
    selectedAccountId: AccountId? = null,
    onAccountSelected: (AccountId) -> Unit = {},
    aiAvailable: Boolean = false,
    onAnalyzeClick: () -> Unit = {},
    onCustomizeOverview: () -> Unit = {},
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
                )
            }
            if (availableCategories.isNotEmpty()) {
                Box {
                    MmIconButton(
                        icon = Icon.Sliders.imageVector,
                        size = MM.dimen.padding_4x,
                        onClick = onShowCategoryFilter,
                        contentDescription = stringResource(Res.string.overview_spending_by_category),
                    )
                    if (selectedCategoryIds.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(MM.dimen.padding_1x)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .align(Alignment.TopEnd),
                        )
                    }
                }
            }
            MmIconButton(
                icon = Icon.Edit.imageVector,
                size = MM.dimen.padding_4x,
                onClick = onCustomizeOverview,
                contentDescription = stringResource(Res.string.overview_customize_cd),
            )
        }

        // Month / Year / Custom period selector — own row below title
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
            size = MmSegmentedSize.Md,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth().padding(top = MM.dimen.padding_1x),
        )

        // All / Expenses / Income filter
        MmSegmented(
            options = listOf(
                stringResource(Res.string.overview_all),
                stringResource(Res.string.overview_expenses),
                stringResource(Res.string.overview_income),
            ),
            selectedIndex = when (transactionFilter.selectedType()) {
                TransactionType.EXPENSE -> 1
                TransactionType.INCOME -> 2
                null -> 0
            },
            onOptionSelected = { idx ->
                onTransactionFilterChanged(
                    transactionFilter.withType(
                        when (idx) {
                            1 -> TransactionType.EXPENSE
                            2 -> TransactionType.INCOME
                            else -> null
                        }
                    )
                )
            },
            size = MmSegmentedSize.Md,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth().padding(top = MM.dimen.padding_1x, bottom = MM.dimen.padding_0_5x),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MM.dimen.padding_1x),
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

            Spacer(Modifier.weight(1f))

            if (aiAvailable) {
                MmButton(
                    text = stringResource(Res.string.overview_analyze_cd),
                    onClick = onAnalyzeClick,
                    variant = MmButtonVariant.Accent,
                    size = MmButtonSize.Sm,
                    leadingIcon = Icon.Sparkles.imageVector,
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
            transactionFilter = TransactionFilter.ByType(TransactionType.EXPENSE),
            onTogglePeriod = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onShowPeriodPicker = {},
            onShowDateRangePicker = {},
            onShowCategoryFilter = {},
            onTransactionFilterChanged = {},
            availableCategories = listOf(
                Category(
                    id = CategoryId(1),
                    name = "Groceries",
                    iconKey = Icon.Basket.key,
                    colorHex = "#4CAF50",
                    isUserCreated = false,
                    archived = false,
                    createdAt = Instant.fromEpochMilliseconds(0),
                    updatedAt = Instant.fromEpochMilliseconds(0),
                    type = TransactionType.EXPENSE,
                ),
                Category(
                    id = CategoryId(2),
                    name = "Salary",
                    iconKey = Icon.Banknote.key,
                    colorHex = "#66BB6A",
                    isUserCreated = false,
                    archived = false,
                    createdAt = Instant.fromEpochMilliseconds(0),
                    updatedAt = Instant.fromEpochMilliseconds(0),
                    type = TransactionType.INCOME,
                ),
            ),
            selectedCategoryIds = setOf(CategoryId(1)),
            aiAvailable = true,
            onAnalyzeClick = {},
        )
    }
}

@Preview
@Composable
private fun OverviewHeaderPreview_Dark() {
    MoneyMTheme(darkTheme = true) {
        OverviewHeader(
            period = OverviewPeriod.Month(YearMonth(2024, 3)),
            periodLabel = "March 2024",
            transactionFilter = TransactionFilter.ByType(TransactionType.EXPENSE),
            onTogglePeriod = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onShowPeriodPicker = {},
            onShowDateRangePicker = {},
            onShowCategoryFilter = {},
            onTransactionFilterChanged = {},
            availableCategories = listOf(
                Category(
                    id = CategoryId(1),
                    name = "Groceries",
                    iconKey = Icon.Basket.key,
                    colorHex = "#4CAF50",
                    isUserCreated = false,
                    archived = false,
                    createdAt = Instant.fromEpochMilliseconds(0),
                    updatedAt = Instant.fromEpochMilliseconds(0),
                    type = TransactionType.EXPENSE,
                ),
                Category(
                    id = CategoryId(2),
                    name = "Salary",
                    iconKey = Icon.Banknote.key,
                    colorHex = "#66BB6A",
                    isUserCreated = false,
                    archived = false,
                    createdAt = Instant.fromEpochMilliseconds(0),
                    updatedAt = Instant.fromEpochMilliseconds(0),
                    type = TransactionType.INCOME,
                ),
            ),
            selectedCategoryIds = setOf(CategoryId(1)),
            aiAvailable = true,
            onAnalyzeClick = {},
        )
    }
}
