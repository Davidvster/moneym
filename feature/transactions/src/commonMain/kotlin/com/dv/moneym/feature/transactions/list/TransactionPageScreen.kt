package com.dv.moneym.feature.transactions.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.YearMonth
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun TransactionPageScreen(
    yearMonth: YearMonth,
    onEditTransaction: (TransactionId) -> Unit,
) {
    val vm = koinViewModel<TransactionPageViewModel>(
        key = yearMonth.toString(),
        parameters = { parametersOf(yearMonth) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    TransactionListBody(
        dayGroups = state.dayGroups,
        txDisplayPrefs = state.txDisplayPrefs,
        isLoading = state.isLoading,
        isEmpty = state.isEmpty,
        onEditTransaction = onEditTransaction,
    )
}
