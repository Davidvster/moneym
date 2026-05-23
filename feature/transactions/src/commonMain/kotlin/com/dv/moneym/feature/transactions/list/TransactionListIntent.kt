package com.dv.moneym.feature.transactions.list

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.YearMonth

internal sealed interface TransactionListIntent {
    data object PreviousMonth : TransactionListIntent
    data object NextMonth : TransactionListIntent
    data class FilterChanged(val filter: TransactionFilter) : TransactionListIntent
    data class SearchQueryChanged(val query: String) : TransactionListIntent
    data class MonthSelected(val yearMonth: YearMonth) : TransactionListIntent
    data class AccountSelected(val accountId: AccountId?) : TransactionListIntent
    data class CategoryFilterToggled(val categoryId: CategoryId) : TransactionListIntent
    data object CategoryFilterCleared : TransactionListIntent
}
