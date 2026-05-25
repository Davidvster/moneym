package com.dv.moneym.feature.budgets.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BudgetListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val budgetRepo = FakeBudgetRepository()
    private val catRepo = FakeCategoryRepository()
    private val dispatchers = TestDispatcherProvider(testDispatcher)

    private fun sample(name: String, categoryId: CategoryId? = null) = Budget(
        id = BudgetId(0),
        name = name,
        amount = Money(40000L, CurrencyCode("EUR")),
        categoryId = categoryId,
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = YearMonth(2026, 5),
        recurringMonths = Budget.UNLIMITED,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun observe_emits_rows_for_inserted_budgets() = runTestWithDispatchers(testDispatcher) {
        budgetRepo.insert(sample("Groceries"))
        budgetRepo.insert(sample("Rent"))
        val vm = BudgetListViewModel(budgetRepo, catRepo, dispatchers, SavedStateHandle())
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(2, state.rows.size)
            assertEquals(setOf("Groceries", "Rent"), state.rows.map { it.name }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_request_then_confirm_removes_budget() = runTestWithDispatchers(testDispatcher) {
        val id = budgetRepo.insert(sample("Groceries"))
        val vm = BudgetListViewModel(budgetRepo, catRepo, dispatchers, SavedStateHandle())
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(BudgetListIntent.DeleteRequested(id))
            state = awaitItem()
            assertEquals(id, state.deleteRequestId)
            assertEquals("Groceries", state.deleteRequestName)
            vm.onIntent(BudgetListIntent.ConfirmDelete)
            testDispatcher.scheduler.advanceUntilIdle()
            // collect emissions until rows are empty
            while (true) {
                val s = awaitItem()
                if (s.rows.isEmpty()) {
                    assertNull(s.deleteRequestId)
                    break
                }
            }
            assertTrue(budgetRepo.budgets.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_request_then_dismiss_keeps_budget() = runTestWithDispatchers(testDispatcher) {
        val id = budgetRepo.insert(sample("Groceries"))
        val vm = BudgetListViewModel(budgetRepo, catRepo, dispatchers, SavedStateHandle())
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            vm.onIntent(BudgetListIntent.DeleteRequested(id))
            awaitItem()
            vm.onIntent(BudgetListIntent.DismissDelete)
            state = awaitItem()
            assertNull(state.deleteRequestId)
            assertEquals(1, state.rows.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
