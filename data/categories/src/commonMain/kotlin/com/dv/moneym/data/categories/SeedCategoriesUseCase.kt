package com.dv.moneym.data.categories

class SeedCategoriesUseCase(
    private val repository: CategoryRepository,
    private val nameProvider: suspend () -> List<String>,
) {
    suspend operator fun invoke() {
        if (repository.count() == 0L) {
            val names = nameProvider()
            defaultCategorySpecs.zip(names) { spec, name ->
                spec.toCategory(name)
            }.forEach { repository.insert(it) }
        }
    }
}
