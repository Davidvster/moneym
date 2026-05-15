package com.dv.moneym.feature.categories.domain

import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first

enum class ArchiveResult { Archived, Deleted, NotFound }

class ArchiveCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(categoryId: CategoryId): ArchiveResult {
        val category = categoryRepository.getById(categoryId) ?: return ArchiveResult.NotFound
        val hasTransactions = transactionRepository
            .observeFiltered(TransactionFilter.ByCategory(categoryId))
            .first()
            .isNotEmpty()
        return if (hasTransactions) {
            categoryRepository.update(category.copy(archived = true))
            ArchiveResult.Archived
        } else {
            categoryRepository.delete(categoryId)
            ArchiveResult.Deleted
        }
    }
}
