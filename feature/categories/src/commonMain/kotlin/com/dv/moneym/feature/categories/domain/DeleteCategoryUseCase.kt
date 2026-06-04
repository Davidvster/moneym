package com.dv.moneym.feature.categories.domain

import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository

sealed interface DeleteStrategy {
    data object Archive : DeleteStrategy
    data class Migrate(val target: CategoryId) : DeleteStrategy
    data object DeleteWithTransactions : DeleteStrategy
}

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: CategoryId, strategy: DeleteStrategy) {
        when (strategy) {
            is DeleteStrategy.Archive -> {
                val category = categoryRepository.getById(id) ?: return
                categoryRepository.update(category.copy(archived = true))
            }

            is DeleteStrategy.Migrate -> {
                transactionRepository.reassignCategory(id, strategy.target)
                categoryRepository.delete(id)
            }

            is DeleteStrategy.DeleteWithTransactions -> {
                transactionRepository.deleteByCategory(id)
                categoryRepository.delete(id)
            }
        }
    }
}
