package com.dv.moneym.feature.settings.recurring

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecurringListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun rule(id: Long, categoryId: Long) = RecurringTransaction(
        id = RecurringTransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(1000, CurrencyCode("EUR")),
        note = "Rent",
        categoryId = CategoryId(categoryId),
        accountId = AccountId(1),
        paymentModeId = null,
        startDate = LocalDate(2026, 1, 1),
        rule = RecurrenceRule.Monthly(1, com.dv.moneym.core.model.MonthlyDayKind.OnDay(1)),
        endCondition = EndCondition.Unlimited,
        lastMaterializedDate = null,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
    )

    private fun category(id: Long, name: String) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = "tag",
        colorHex = "#FFFFFF",
        isUserCreated = true,
        archived = false,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
    )

    @Test
    fun stateJoinsRulesWithCategories() = runTest(testDispatcher) {
        val recurringRepo = FakeRecurringTransactionRepository()
        val categoryRepo = FakeCategoryRepository()
        recurringRepo.addAll(listOf(rule(1, categoryId = 5)))
        categoryRepo.addAll(listOf(category(5, "Housing")))
        val vm = RecurringListViewModel(recurringRepo, categoryRepo)

        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isLoading || s.rules.isEmpty()) s = awaitItem()
            assertFalse(s.isLoading)
            assertEquals(1, s.rules.size)
            assertEquals("Housing", s.categories[CategoryId(5)]?.name)
        }
    }

    @Test
    fun emptyReposYieldEmptyState() = runTest(testDispatcher) {
        val vm = RecurringListViewModel(FakeRecurringTransactionRepository(), FakeCategoryRepository())
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertTrue(s.rules.isEmpty())
            assertTrue(s.categories.isEmpty())
        }
    }

    @Test
    fun ruleWithoutMatchingCategoryStillRenders() = runTest(testDispatcher) {
        val recurringRepo = FakeRecurringTransactionRepository()
        val categoryRepo = FakeCategoryRepository()
        recurringRepo.addAll(listOf(rule(1, categoryId = 99)))
        val vm = RecurringListViewModel(recurringRepo, categoryRepo)

        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isLoading || s.rules.isEmpty()) s = awaitItem()
            assertEquals(1, s.rules.size)
            assertTrue(s.categories[CategoryId(99)] == null)
        }
    }
}
