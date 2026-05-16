package com.dv.moneym.feature.transactions.presentation

import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.YearMonth

sealed interface TransactionListIntent {
    data object PreviousMonth : TransactionListIntent
    data object NextMonth : TransactionListIntent
    data class FilterChanged(val filter: TransactionFilter) : TransactionListIntent
    data class SearchQueryChanged(val query: String) : TransactionListIntent
    data class MonthSelected(val yearMonth: YearMonth) : TransactionListIntent
}
