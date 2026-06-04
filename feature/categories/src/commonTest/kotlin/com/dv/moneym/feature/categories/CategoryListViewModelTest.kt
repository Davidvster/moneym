package com.dv.moneym.feature.categories

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeCategoryRepository
import kotlinx.datetime.LocalDate
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.feature.categories.domain.DeleteCategoryUseCase
import com.dv.moneym.feature.categories.list.CategoryListIntent
import androidx.lifecycle.SavedStateHandle
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
    private val deleteUseCase = DeleteCategoryUseCase(catRepo, txnRepo)

    private fun vm() = CategoryListViewModel(catRepo, txnRepo, deleteUseCase, dispatchers, SavedStateHandle())

    private fun makeCategory(id: Long, archived: Boolean = false, sortOrder: Int = 0) = Category(
        id = CategoryId(id),
        name = "Cat$id",
        iconKey = Icon.Dots.key,
        colorHex = "#8A8A8A",
        isUserCreated = true,
        archived = archived,
        createdAt = epoch,
        updatedAt = epoch,
        sortOrder = sortOrder,
    )

    private var nextTxId = 100L
    private fun makeTransaction(categoryId: Long) = Transaction(
        id = TransactionId(nextTxId++),
        type = TransactionType.EXPENSE,
        amount = Money(1000, CurrencyCode("EUR")),
        occurredOn = LocalDate(2026, 5, 1),
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun activeAndArchivedCategoriesSeparated() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(
            makeCategory(1, archived = false),
            makeCategory(2, archived = true),
        ))
        vm().state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(1, state.active.size)
            assertEquals(1, state.archived.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reorderPersistsSortOrder() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(
            makeCategory(1, sortOrder = 0),
            makeCategory(2, sortOrder = 1),
            makeCategory(3, sortOrder = 2),
        ))
        val vm = vm()
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.Reorder(listOf(CategoryId(3), CategoryId(1), CategoryId(2))))
            testDispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, catRepo.categories.first { it.id == CategoryId(3) }.sortOrder)
        assertEquals(1, catRepo.categories.first { it.id == CategoryId(1) }.sortOrder)
        assertEquals(2, catRepo.categories.first { it.id == CategoryId(2) }.sortOrder)
    }

    @Test
    fun requestDeleteWithNoTransactionsShowsSimpleConfirmThenDeletes() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(makeCategory(1)))
        val vm = vm()
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.StartEditCategory(makeCategory(1)))
            vm.onIntent(CategoryListIntent.DeleteCategory(CategoryId(1)))
            testDispatcher.scheduler.advanceUntilIdle()
            vm.onIntent(CategoryListIntent.ConfirmSimpleDelete)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(catRepo.categories.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestDeleteWithTransactionsOpensOptions() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(makeCategory(1)))
        txnRepo.addAll(listOf(makeTransaction(categoryId = 1)))
        val vm = vm()
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.DeleteCategory(CategoryId(1)))
            testDispatcher.scheduler.advanceUntilIdle()
            var s = awaitItem()
            while (s.deleteOptionsFor == null) s = awaitItem()
            assertEquals(CategoryId(1), s.deleteOptionsFor.id)
            assertEquals(1, s.deleteTxCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmDeleteAllRemovesCategoryAndTransactions() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(makeCategory(1)))
        txnRepo.addAll(listOf(makeTransaction(categoryId = 1)))
        val vm = vm()
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.OpenDeleteOptions(makeCategory(1)))
            testDispatcher.scheduler.advanceUntilIdle()
            vm.onIntent(CategoryListIntent.OpenDeleteAllConfirm)
            vm.onIntent(CategoryListIntent.TypeConfirmChanged("Cat1"))
            vm.onIntent(CategoryListIntent.ConfirmDeleteAll)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(catRepo.categories.isEmpty())
            assertTrue(txnRepo.transactions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmDeleteAllRejectsWrongName() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(makeCategory(1)))
        txnRepo.addAll(listOf(makeTransaction(categoryId = 1)))
        val vm = vm()
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.OpenDeleteOptions(makeCategory(1)))
            vm.onIntent(CategoryListIntent.OpenDeleteAllConfirm)
            vm.onIntent(CategoryListIntent.TypeConfirmChanged("wrong"))
            vm.onIntent(CategoryListIntent.ConfirmDeleteAll)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(catRepo.categories.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun migrateReassignsTransactionsAndDeletesCategory() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(makeCategory(1), makeCategory(2)))
        txnRepo.addAll(listOf(makeTransaction(categoryId = 1)))
        val vm = vm()
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(CategoryListIntent.OpenDeleteOptions(makeCategory(1)))
            vm.onIntent(CategoryListIntent.MigrateTo(CategoryId(2)))
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(catRepo.categories.none { it.id == CategoryId(1) })
            assertEquals(CategoryId(2), txnRepo.transactions.first().categoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
