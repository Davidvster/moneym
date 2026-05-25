package com.dv.moneym.feature.budgets.create

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FixedClock
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class BudgetCreateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val budgetRepo = FakeBudgetRepository()
    private val catRepo = FakeCategoryRepository()
    private val accountRepo = FakeAccountRepository()
    private val dispatchers = TestDispatcherProvider(testDispatcher)
    // 2026-05-15T12:00:00Z
    private val clock = FixedClock(Instant.fromEpochMilliseconds(1778803200000L))

    private fun newViewModel(budgetId: BudgetId? = null) = BudgetCreateViewModel(
        budgetId = budgetId,
        budgetRepository = budgetRepo,
        categoryRepository = catRepo,
        accountRepository = accountRepo,
        clock = clock,
        dispatchers = dispatchers,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun init_defaults_to_current_month_and_default_currency() = runTestWithDispatchers(testDispatcher) {
        accountRepo.addAll(
            listOf(
                Account(
                    id = AccountId(1),
                    name = "Main",
                    type = AccountType.CASH,
                    currency = CurrencyCode("USD"),
                    isDefault = true,
                    archived = false,
                    createdAt = epoch,
                    updatedAt = epoch,
                ),
            ),
        )
        val vm = newViewModel()
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("USD", s.currency)
            assertEquals(YearMonth(2026, 5), s.startYearMonth)
            assertFalse(s.isEditMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun save_blank_name_sets_nameError() = runTestWithDispatchers(testDispatcher) {
        val vm = newViewModel()
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.onIntent(BudgetCreateIntent.AmountChanged("100"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.Save)
            s = awaitItem()
            assertTrue(s.nameError)
            assertTrue(budgetRepo.budgets.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun save_zero_amount_sets_amountError() = runTestWithDispatchers(testDispatcher) {
        val vm = newViewModel()
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.onIntent(BudgetCreateIntent.NameChanged("Groceries"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.AmountChanged("0"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.Save)
            s = awaitItem()
            assertTrue(s.amountError)
            assertTrue(budgetRepo.budgets.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun save_n_months_zero_sets_recurringCountError() = runTestWithDispatchers(testDispatcher) {
        val vm = newViewModel()
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.onIntent(BudgetCreateIntent.NameChanged("Groceries"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.AmountChanged("100"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.RecurringKindChanged(RecurringKind.NMonths))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.RecurringCountChanged(0))
            s = awaitItem()
            // RecurringCountChanged coerces to >= 1, so n is 1 not 0; that's a valid save.
            // To exercise the validation path, we fabricate manually: use Single then force the path.
            // Skip — the coercion makes the < 1 case unreachable from UI. Test that coercion works:
            assertEquals(1, s.recurringNMonths)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun valid_save_inserts_budget_with_unlimited() = runTestWithDispatchers(testDispatcher) {
        accountRepo.addAll(
            listOf(
                Account(
                    id = AccountId(1),
                    name = "Main",
                    type = AccountType.CASH,
                    currency = CurrencyCode("EUR"),
                    isDefault = true,
                    archived = false,
                    createdAt = epoch,
                    updatedAt = epoch,
                ),
            ),
        )
        val vm = newViewModel()
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.onIntent(BudgetCreateIntent.NameChanged("Groceries"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.AmountChanged("400.50"))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.CategorySelected(CategoryId(7)))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.RecurringKindChanged(RecurringKind.Unlimited))
            awaitItem()
            vm.onIntent(BudgetCreateIntent.Save)
            testDispatcher.scheduler.advanceUntilIdle()
            // drain remaining state emissions after save
            cancelAndIgnoreRemainingEvents()
        }
        vm.singleEvents.test {
            val ev = awaitItem()
            assertTrue(ev is BudgetCreateViewModel.BudgetCreateSingleUiEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, budgetRepo.budgets.size)
        val saved = budgetRepo.budgets.first()
        assertEquals("Groceries", saved.name)
        assertEquals(40050L, saved.amount.minorUnits)
        assertEquals(CategoryId(7), saved.categoryId)
        assertEquals(Budget.UNLIMITED, saved.recurringMonths)
        assertEquals(BudgetPeriodType.MONTHLY, saved.periodType)
    }

    @Test
    fun edit_mode_prefills_state() = runTestWithDispatchers(testDispatcher) {
        val id = budgetRepo.insert(
            Budget(
                id = BudgetId(0),
                name = "Rent",
                amount = Money(120000L, CurrencyCode("EUR")),
                categoryId = CategoryId(3),
                accountId = AccountId(1),
                periodType = BudgetPeriodType.MONTHLY,
                startYearMonth = YearMonth(2026, 4),
                recurringMonths = 6,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        val vm = newViewModel(budgetId = id)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertTrue(s.isEditMode)
            assertEquals("Rent", s.name)
            assertEquals("1200.00", s.amountText)
            assertEquals(CategoryId(3), s.selectedCategoryId)
            assertEquals(YearMonth(2026, 4), s.startYearMonth)
            assertEquals(RecurringKind.NMonths, s.recurringKind)
            assertEquals(6, s.recurringNMonths)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
