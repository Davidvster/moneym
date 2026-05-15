package com.dv.moneym.feature.transactions.presentation

import com.dv.moneym.core.model.TransactionFilter

sealed interface TransactionListIntent {
    data object PreviousMonth : TransactionListIntent
    data object NextMonth : TransactionListIntent
    data class FilterChanged(val filter: TransactionFilter) : TransactionListIntent
}
