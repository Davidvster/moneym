package com.dv.moneym.data.categories

class SeedCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke() {
        if (repository.count() == 0L) {
            defaultCategories.forEach { repository.insert(it) }
        }
    }
}
