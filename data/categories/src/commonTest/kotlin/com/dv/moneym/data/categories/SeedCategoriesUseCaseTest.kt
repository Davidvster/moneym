package com.dv.moneym.data.categories

import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

class SeedCategoriesUseCaseTest {

    @Test
    fun seedsDefaultCategoriesOnFirstRun() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        val useCase = SeedCategoriesUseCase(repo)

        useCase()

        assertEquals(defaultCategories.size, repo.categories.size)
    }

    @Test
    fun doesNotSeedWhenCategoriesAlreadyExist() = runTestWithDispatchers {
        val repo = FakeCategoryRepository()
        val useCase = SeedCategoriesUseCase(repo)

        useCase() // first run
        useCase() // second run — should be idempotent

        assertEquals(defaultCategories.size, repo.categories.size)
    }
}
