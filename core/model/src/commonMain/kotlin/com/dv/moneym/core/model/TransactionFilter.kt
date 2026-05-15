package com.dv.moneym.core.model

sealed interface TransactionFilter {
    data object None : TransactionFilter
    data class ByCategory(val categoryId: CategoryId) : TransactionFilter
    data class ByType(val type: TransactionType) : TransactionFilter
    data class ByCategoryAndType(
        val categoryId: CategoryId,
        val type: TransactionType,
    ) : TransactionFilter
}
