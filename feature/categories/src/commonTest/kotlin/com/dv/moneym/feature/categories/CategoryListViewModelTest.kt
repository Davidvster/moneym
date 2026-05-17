package com.dv.moneym.feature.categories

import app.cash.turbine.test
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.feature.categories.domain.ArchiveCategoryUseCase
import com.dv.moneym.feature.categories.list.CategoryListIntent
import com.dv.moneym.feature.categories.list.CategoryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CategoryListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val catRepo = FakeCategoryRepository()
    private val txnRepo = FakeTransactionRepository()
    private val dispatchers = TestDispatcherProvider(testDispatcher)
    private val archiveUseCase = ArchiveCategoryUseCase(catRepo, txnRepo)

    private fun makeCategory(id: Long, archived: Boolean = false) = Category(
        id = CategoryId(id),
        name = "Cat$id",
        iconKey = "dots",
        colorHex = "#8A8A8A",
        isUserCreated = true,
        archived = archived,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun activeAndArchivedCategoriesSeparated() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(
            makeCategory(1, archived = false),
            makeCategory(2, archived = true),
        ))
        val vm = CategoryListViewModel(catRepo, archiveUseCase, dispatchers)
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(1, state.active.size)
            assertEquals(1, state.archived.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun archiveIntentDeletesCategoryWithNoTransactions() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(makeCategory(1)))
        val vm = CategoryListViewModel(catRepo, archiveUseCase, dispatchers)
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.ArchiveRequested(CategoryId(1)))
            testDispatcher.scheduler.advanceUntilIdle()
            // no transactions → deleted
            assertTrue(catRepo.categories.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
