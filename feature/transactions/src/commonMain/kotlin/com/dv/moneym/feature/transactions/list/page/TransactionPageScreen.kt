package com.dv.moneym.feature.transactions.list.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.feature.transactions.list.DayGroup
import com.dv.moneym.feature.transactions.list.TransactionListBody
import com.dv.moneym.feature.transactions.list.TransactionUiModel
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun TransactionPageScreen(
    yearMonth: YearMonth,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit,
    onAddFirst: (() -> Unit)? = null,
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
        onAddFirst = onAddFirst,
        modifier = Modifier.fillMaxSize(),
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TransactionPageScreenPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        TransactionListBody(
            dayGroups = listOf(
                DayGroup(
                    date = LocalDate(2026, 6, 10),
                    label = "Today, Jun 10",
                    transactions = listOf(
                        TransactionUiModel(
                            id = TransactionId(1L),
                            type = TransactionType.EXPENSE,
                            amountFormatted = "42.50",
                            amountMinorUnits = 4250L,
                            currency = "EUR",
                            isExpense = true,
                            categoryName = "Groceries",
                            categoryColorHex = "#4CAF50",
                            categoryIcon = Icon.Basket,
                            note = "Weekly shop",
                            occurredOn = LocalDate(2026, 6, 10),
                            paymentModeName = "Card",
                        ),
                        TransactionUiModel(
                            id = TransactionId(2L),
                            type = TransactionType.EXPENSE,
                            amountFormatted = "12.90",
                            amountMinorUnits = 1290L,
                            currency = "EUR",
                            isExpense = true,
                            categoryName = "Restaurants",
                            categoryColorHex = "#FF7043",
                            categoryIcon = Icon.Restaurant,
                            note = "Lunch",
                            occurredOn = LocalDate(2026, 6, 10),
                            paymentModeName = "Cash",
                        ),
                    ),
                ),
                DayGroup(
                    date = LocalDate(2026, 6, 9),
                    label = "Yesterday, Jun 9",
                    transactions = listOf(
                        TransactionUiModel(
                            id = TransactionId(3L),
                            type = TransactionType.INCOME,
                            amountFormatted = "2500.00",
                            amountMinorUnits = 250000L,
                            currency = "EUR",
                            isExpense = false,
                            categoryName = "Salary",
                            categoryColorHex = "#66BB6A",
                            categoryIcon = Icon.Banknote,
                            note = "Monthly salary",
                            occurredOn = LocalDate(2026, 6, 9),
                            paymentModeName = "Bank transfer",
                        ),
                        TransactionUiModel(
                            id = TransactionId(4L),
                            type = TransactionType.EXPENSE,
                            amountFormatted = "65.00",
                            amountMinorUnits = 6500L,
                            currency = "EUR",
                            isExpense = true,
                            categoryName = "Transport",
                            categoryColorHex = "#42A5F5",
                            categoryIcon = Icon.Car,
                            note = "Fuel",
                            occurredOn = LocalDate(2026, 6, 9),
                            paymentModeName = "Card",
                        ),
                    ),
                ),
            ),
            txDisplayPrefs = com.dv.moneym.core.model.TxDisplayPrefs(),
            isLoading = false,
            isEmpty = false,
            onEditTransaction = {},
            onEditRecurring = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
