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
    @Serializable
    data class BySelection(
        val type: TransactionType? = null,
        val categoryIds: Set<CategoryId> = emptySet(),
    ) : TransactionFilter
}

fun TransactionFilter.selectedType(): TransactionType? = when (this) {
    is TransactionFilter.None -> null
    is TransactionFilter.ByCategory -> null
    is TransactionFilter.ByType -> type
    is TransactionFilter.ByCategoryAndType -> type
    is TransactionFilter.BySelection -> type
}

fun TransactionFilter.selectedCategoryIds(): Set<CategoryId> = when (this) {
    is TransactionFilter.None -> emptySet()
    is TransactionFilter.ByCategory -> setOf(categoryId)
    is TransactionFilter.ByType -> emptySet()
    is TransactionFilter.ByCategoryAndType -> setOf(categoryId)
    is TransactionFilter.BySelection -> categoryIds
}

fun TransactionFilter.withType(type: TransactionType?): TransactionFilter =
    transactionFilterOf(type, selectedCategoryIds())

fun TransactionFilter.toggleCategory(categoryId: CategoryId): TransactionFilter {
    val selected = selectedCategoryIds()
    val updated = if (categoryId in selected) selected - categoryId else selected + categoryId
    return transactionFilterOf(selectedType(), updated)
}

fun TransactionFilter.clearCategories(): TransactionFilter =
    transactionFilterOf(selectedType(), emptySet())

fun TransactionFilter.matches(transaction: Transaction): Boolean {
    val type = selectedType()
    val categoryIds = selectedCategoryIds()
    return (type == null || transaction.type == type) &&
            (categoryIds.isEmpty() || transaction.categoryId in categoryIds)
}

fun transactionFilterOf(
    type: TransactionType?,
    categoryIds: Set<CategoryId>,
): TransactionFilter {
    val normalizedCategoryIds = categoryIds.toSet()
    return when {
        type == null && normalizedCategoryIds.isEmpty() -> TransactionFilter.None
        type != null && normalizedCategoryIds.isEmpty() -> TransactionFilter.ByType(type)
        type == null && normalizedCategoryIds.size == 1 -> TransactionFilter.ByCategory(normalizedCategoryIds.first())
        type != null && normalizedCategoryIds.size == 1 ->
            TransactionFilter.ByCategoryAndType(normalizedCategoryIds.first(), type)

        else -> TransactionFilter.BySelection(type, normalizedCategoryIds)
    }
}
