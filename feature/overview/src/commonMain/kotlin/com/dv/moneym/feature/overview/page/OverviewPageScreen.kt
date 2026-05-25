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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MM.dimen.padding_2x),
    ) {
        item {
            OverviewPeriodBody(
                state = state,
                spendingFilter = spendingFilter,
                currencyCode = currencyCode,
                onIntent = { vm.onIntent(it) },
            )
        }
    }
}
