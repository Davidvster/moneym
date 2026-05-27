package com.dv.moneym.feature.overview.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.components.OverviewPeriodBody
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun OverviewPageScreen(
    period: OverviewPeriod,
    spendingFilter: SpendingFilter,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val vm = koinViewModel<OverviewPageViewModel>(
        key = period.toString(),
        parameters = { parametersOf(period) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    OverviewPageContent(
        state = state,
        spendingFilter = spendingFilter,
        currencyCode = currencyCode,
        onIntent = vm::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun OverviewPageContent(
    state: OverviewPageUiState,
    spendingFilter: SpendingFilter,
    currencyCode: String,
    onIntent: (OverviewPageIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MM.dimen.padding_2x),
    ) {
        item {
            OverviewPeriodBody(
                state = state,
                spendingFilter = spendingFilter,
                currencyCode = currencyCode,
                onIntent = onIntent,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun OverviewPageContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        OverviewPageContent(
            state = OverviewPageUiState(
                isLoading = false,
                isEmpty = false,
                income = 2500.0,
                expenses = 1850.0,
                dailyTotals = listOf(20.0, 45.0, 80.0, 30.0, 60.0, 110.0, 70.0),
                cumulativeTotals = listOf(20.0, 65.0, 145.0, 175.0, 235.0, 345.0, 415.0),
                todayIndex = 6,
                avgDailyExpense = 59.3,
            ),
            spendingFilter = SpendingFilter.All,
            currencyCode = "€",
            onIntent = {},
        )
    }
}
