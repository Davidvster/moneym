package com.dv.moneym.data.categories

import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

private val englishNames = listOf(
    "Groceries", "Eating out", "Rent", "Transport", "Utilities",
    "Health", "Entertainment", "Shopping", "Other",
    "Salary", "Payment", "Gift", "Other",
)

class SeedCategoriesUseCaseTest {

    @Test
    fun seedsDefaultCategoriesOnFirstRun() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        val useCase = SeedCategoriesUseCase(repo, { englishNames }, nowMs = { 100L })

        useCase()

        assertEquals(defaultCategories.size, repo.categories.size)
    }

    @Test
    fun doesNotSeedWhenCategoriesAlreadyExist() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        val useCase = SeedCategoriesUseCase(repo, { englishNames }, nowMs = { 100L })

        useCase() // first run
        useCase() // second run — should be idempotent

        assertEquals(defaultCategories.size, repo.categories.size)
    }

    @Test
    fun seedsDeterministicLanguageIndependentSyncIds() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        SeedCategoriesUseCase(repo, { englishNames }, nowMs = { 100L })()

        val syncIds = repo.exportForSync().map { it.syncId }
        assertEquals(
            defaultCategorySpecs.indices.map { "seed-category-$it" },
            syncIds,
        )
    }

    @Test
    fun reSeedIsIdempotentBySyncId() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        // Same indices, different localized names (DE device) → must NOT create duplicates.
        val germanNames = englishNames.map { "$it-de" }
        SeedCategoriesUseCase(repo, { englishNames }, nowMs = { 100L })()
        SeedCategoriesUseCase(repo, { germanNames }, nowMs = { 200L })()

        assertEquals(defaultCategorySpecs.size, repo.categories.size)
        assertEquals(
            defaultCategorySpecs.indices.map { "seed-category-$it" },
            repo.exportForSync().map { it.syncId },
        )
    }
}
