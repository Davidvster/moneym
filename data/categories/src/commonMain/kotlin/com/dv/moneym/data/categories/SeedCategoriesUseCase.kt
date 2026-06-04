package com.dv.moneym.data.categories

import kotlin.time.Clock

class SeedCategoriesUseCase(
    private val repository: CategoryRepository,
    private val nameProvider: suspend () -> List<String>,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend operator fun invoke() {
        if (repository.count() > 0L) return
        val names = nameProvider()
        val now = nowMs()
        defaultCategorySpecs.forEachIndexed { index, spec ->
            val name = names.getOrNull(index) ?: return@forEachIndexed
            repository.upsertFromSync(
                CategorySyncRow(
                    id = 0,
                    syncId = "seed-category-$index",
                    name = name,
                    iconKey = spec.icon.key,
                    colorHex = spec.color,
                    isUserCreated = false,
                    archived = false,
                    categoryType = spec.type.name,
                    deleted = false,
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = index,
                )
            )
        }
    }
}
