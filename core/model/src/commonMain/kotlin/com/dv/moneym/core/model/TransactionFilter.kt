package com.dv.moneym.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface TransactionFilter {
    @Serializable
    data object None : TransactionFilter
    @Serializable
    data class ByCategory(val categoryId: CategoryId) : TransactionFilter
    @Serializable
    data class ByType(val type: TransactionType) : TransactionFilter
    @Serializable
    data class ByCategoryAndType(
        val categoryId: CategoryId,
        val type: TransactionType,
    ) : TransactionFilter
}
