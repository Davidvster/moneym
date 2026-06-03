package com.dv.moneym.feature.transactionedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecurringEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-10T12:00:00Z"))

    private fun account(id: Long, isDefault: Boolean = false) = Account(
        id = AccountId(id),
        name = "W$id",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = isDefault,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun category(id: Long, type: TransactionType) = Category(
        id = CategoryId(id),
        name = "C$id",
        iconKey = "i",
        colorHex = "#FFFFFF",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = type,
    )

    private class Deps {
        val catRepo = FakeCategoryRepository()
        val accountRepo = FakeAccountRepository()
        val recurringRepo = FakeRecurringTransactionRepository()
        val settings = FakeAppSettingsRepository()
        val paymentModes = FakePaymentModeRepository()
    }

    private fun vm(ruleId: Long, deps: Deps) = RecurringEditViewModel(
        ruleId = RecurringTransactionId(ruleId),
        recurringRepo = deps.recurringRepo,
        categoryRepository = deps.catRepo,
        accountRepository = deps.accountRepo,
        paymentModeRepository = deps.paymentModes,
        appSettingsRepository = deps.settings,
        dispatchers = TestDispatcherProvider(testDispatcher),
        clock = clock,
        savedStateHandle = SavedStateHandle(),
    )

    private fun seed(deps: Deps) {
        deps.catRepo.addAll(
            listOf(category(1, TransactionType.EXPENSE), category(2, TransactionType.INCOME)),
        )
        deps.accountRepo.addAll(listOf(account(1, isDefault = true)))
    }

    private fun rule(id: Long) = RecurringTransaction(
        id = RecurringTransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(2500, CurrencyCode("EUR")),
        note = "Rent",
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        paymentModeId = null,
        startDate = LocalDate(2026, 1, 1),
        rule = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(1)),
        endCondition = EndCondition.Count(6),
        lastMaterializedDate = null,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun newRulePreSelectsDefaults() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        val vm = vm(0, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.selectedCategoryId == null) s = awaitItem()
            assertFalse(s.isEditMode)
            assertTrue(s.isRecurring)
            assertEquals(CategoryId(1), s.selectedCategoryId)
            assertEquals(AccountId(1), s.selectedAccountId)
            assertEquals(LocalDate(2026, 5, 10), s.date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadsExistingRuleIntoState() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        deps.recurringRepo.addAll(listOf(rule(1)))
        val vm = vm(1, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.availableCategories.isEmpty()) s = awaitItem()
            assertTrue(s.isEditMode)
            assertEquals("25.00", s.amountText)
            assertEquals(LocalDate(2026, 1, 1), s.date)
            assertEquals(CategoryId(1), s.selectedCategoryId)
            assertEquals("Rent", s.note)
            assertEquals(FreqUnit.MONTHS, s.freqUnit)
            assertEquals(EndKind.COUNT, s.endKind)
            assertEquals(6, s.endCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingRuleEmitsDeleted() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        val vm = vm(42, deps)
        vm.effects.test {
            vm.state.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertIs<TransactionEditEffect.Deleted>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editRecurrenceFieldsUpdateState() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        deps.recurringRepo.addAll(listOf(rule(1)))
        val vm = vm(1, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.availableCategories.isEmpty()) s = awaitItem()
            vm.onIntent(TransactionEditIntent.FreqUnitChanged(FreqUnit.WEEKS))
            var fu = awaitItem()
            while (fu.freqUnit != FreqUnit.WEEKS) fu = awaitItem()
            vm.onIntent(TransactionEditIntent.FreqIntervalChanged(99))
            var fi = awaitItem()
            while (fi.freqInterval != 30) fi = awaitItem()
            assertEquals(30, fi.freqInterval) // coerced to max 30
            vm.onIntent(TransactionEditIntent.WeekDayChanged(3))
            var wd = awaitItem()
            while (wd.weekDay != 3) wd = awaitItem()
            assertEquals(3, wd.weekDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveNewRuleUpserts() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        val vm = vm(0, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.selectedCategoryId == null) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.onIntent(TransactionEditIntent.AmountChanged("9.99"))
        vm.onIntent(TransactionEditIntent.FreqUnitChanged(FreqUnit.DAYS))
        vm.effects.test {
            vm.onIntent(TransactionEditIntent.SaveRequested)
            assertIs<TransactionEditEffect.Saved>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val saved = deps.recurringRepo.rules.single()
        assertEquals(999, saved.amount.minorUnits)
        assertIs<RecurrenceRule.Daily>(saved.rule)
    }

    @Test
    fun saveExistingRuleUpdates() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        deps.recurringRepo.addAll(listOf(rule(1)))
        val vm = vm(1, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.availableCategories.isEmpty()) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.onIntent(TransactionEditIntent.AmountChanged("30.00"))
        vm.effects.test {
            vm.onIntent(TransactionEditIntent.SaveRequested)
            assertIs<TransactionEditEffect.Saved>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(3000, deps.recurringRepo.rules.single { it.id.value == 1L }.amount.minorUnits)
    }

    @Test
    fun saveInvalidAmountSetsError() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        val vm = vm(0, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.selectedCategoryId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.AmountChanged("0"))
            awaitItem()
            vm.onIntent(TransactionEditIntent.SaveRequested)
            var err = awaitItem()
            while (!err.amountError) err = awaitItem()
            assertTrue(err.amountError)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(deps.recurringRepo.rules.isEmpty())
    }

    @Test
    fun deleteExistingRuleRemovesAndEmitsDeleted() = runTest(testDispatcher) {
        val deps = Deps()
        seed(deps)
        deps.recurringRepo.addAll(listOf(rule(1)))
        val vm = vm(1, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.availableCategories.isEmpty()) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test {
            vm.onIntent(TransactionEditIntent.DeleteConfirmed)
            assertIs<TransactionEditEffect.Deleted>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(deps.recurringRepo.rules.isEmpty())
    }
}
