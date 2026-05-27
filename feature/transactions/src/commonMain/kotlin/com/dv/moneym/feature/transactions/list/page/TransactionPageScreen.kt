package com.dv.moneym.feature.transactions.list.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.feature.transactions.list.TransactionListBody
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun TransactionPageScreen(
    yearMonth: YearMonth,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit,
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
        onEditRecurring = onEditRecurring,
        modifier = Modifier.fillMaxSize(),
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TransactionPageScreenPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        TransactionListBody(
            dayGroups = emptyList(),
            txDisplayPrefs = com.dv.moneym.core.model.TxDisplayPrefs(),
            isLoading = false,
            isEmpty = true,
            onEditTransaction = {},
            onEditRecurring = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
