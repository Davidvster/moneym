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
        val useCase = SeedCategoriesUseCase(repo) { englishNames }

        useCase()

        assertEquals(defaultCategories.size, repo.categories.size)
    }

    @Test
    fun doesNotSeedWhenCategoriesAlreadyExist() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        val useCase = SeedCategoriesUseCase(repo) { englishNames }

        useCase() // first run
        useCase() // second run — should be idempotent

        assertEquals(defaultCategories.size, repo.categories.size)
    }
}
