package com.dv.moneym.feature.overview.page

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.usecase.BuildBudgetProgressUseCase
import com.dv.moneym.feature.overview.usecase.BuildCategoryBreakdownUseCase
import com.dv.moneym.feature.overview.usecase.BuildCategoryTrendsUseCase
import com.dv.moneym.feature.overview.usecase.BuildCumulativeSeriesUseCase
import com.dv.moneym.feature.overview.usecase.BuildOverviewPageStateUseCase
import com.dv.moneym.feature.overview.usecase.ResolvePeriodRangeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OverviewPageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-10T12:00:00Z"))
    private val txnRepo = FakeTransactionRepository()
    private val catRepo = FakeCategoryRepository()
    private val accountRepo = FakeAccountRepository()
    private val settingsRepo = FakeAppSettingsRepository()
    private val budgetRepo = FakeBudgetRepository()
    private val buildOverviewPageState = BuildOverviewPageStateUseCase(
        resolvePeriodRange = ResolvePeriodRangeUseCase(),
        buildCategoryBreakdown = BuildCategoryBreakdownUseCase(),
        buildCategoryTrends = BuildCategoryTrendsUseCase(),
        buildCumulativeSeries = BuildCumulativeSeriesUseCase(),
        buildBudgetProgress = BuildBudgetProgressUseCase(),
    )

    private fun makeVm(period: OverviewPeriod) = OverviewPageViewModel(
        period = period,
        transactionRepository = txnRepo,
        categoryRepository = catRepo,
        accountRepository = accountRepo,
        appSettingsRepository = settingsRepo,
        budgetRepository = budgetRepo,
        buildOverviewPageState = buildOverviewPageState,
        clock = clock,
    )

    private fun category(id: Long, type: TransactionType) = Category(
        id = CategoryId(id),
        name = "C$id",
        iconKey = "icon",
        colorHex = "#FFFFFF",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = type,
    )

    private fun txn(
        type: TransactionType,
        amount: Long,
        catId: Long = 1,
        accId: Long = 1,
        date: LocalDate = LocalDate(2026, 5, 10),
    ) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = type,
        amount = Money(amount, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(catId),
        accountId = AccountId(accId),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun expensesAggregatedAndCategoryBreakdownBuilt() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(category(1, TransactionType.EXPENSE)))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 4000, catId = 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 1000, catId = 1))
        val vm = makeVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertFalse(s.isEmpty)
            assertEquals(50.0, s.expenses)
            assertTrue(s.categoryBreakdown.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sliceTappedTogglesSelectedIndex() = runTestWithDispatchers(testDispatcher) {
        txnRepo.upsert(txn(TransactionType.EXPENSE, 1000))
        val vm = makeVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertNull(s.selectedSliceIndex)
            vm.onIntent(OverviewPageIntent.SliceTapped(2))
            var sel = awaitItem()
            while (sel.selectedSliceIndex != 2) sel = awaitItem()
            assertEquals(2, sel.selectedSliceIndex)
            vm.onIntent(OverviewPageIntent.SliceTapped(2))
            var cleared = awaitItem()
            while (cleared.selectedSliceIndex != null) cleared = awaitItem()
            assertNull(cleared.selectedSliceIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryFilterRestrictsTotalsAndClearsSlice() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(
            listOf(category(1, TransactionType.EXPENSE), category(2, TransactionType.EXPENSE)),
        )
        txnRepo.upsert(txn(TransactionType.EXPENSE, 3000, catId = 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 2000, catId = 2))
        val vm = makeVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(50.0, s.expenses)
            vm.onIntent(OverviewPageIntent.SliceTapped(1))
            var withSlice = awaitItem()
            while (withSlice.selectedSliceIndex != 1) withSlice = awaitItem()
            vm.onIntent(OverviewPageIntent.CategoryFilterSelected(CategoryId(1)))
            var filtered = awaitItem()
            while (filtered.expenses != 30.0) filtered = awaitItem()
            assertEquals(30.0, filtered.expenses)
            assertNull(filtered.selectedSliceIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryFilterToggleClearsWhenReselected() = runTestWithDispatchers(testDispatcher) {
        catRepo.addAll(listOf(category(1, TransactionType.EXPENSE), category(2, TransactionType.EXPENSE)))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 3000, catId = 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 2000, catId = 2))
        val vm = makeVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.onIntent(OverviewPageIntent.CategoryFilterSelected(CategoryId(1)))
            var filtered = awaitItem()
            while (filtered.expenses != 30.0) filtered = awaitItem()
            vm.onIntent(OverviewPageIntent.CategoryFilterSelected(CategoryId(1)))
            var restored = awaitItem()
            while (restored.expenses != 50.0) restored = awaitItem()
            assertEquals(50.0, restored.expenses)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectedAccountFiltersTransactions() = runTestWithDispatchers(testDispatcher) {
        settingsRepo.setSelectedAccountId(1)
        txnRepo.upsert(txn(TransactionType.EXPENSE, 4000, accId = 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 9999, accId = 2))
        val vm = makeVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.expenses == 0.0) s = awaitItem()
            assertEquals(40.0, s.expenses)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dateRangePeriodBuildsState() = runTestWithDispatchers(testDispatcher) {
        txnRepo.upsert(txn(TransactionType.EXPENSE, 2500, date = LocalDate(2026, 5, 5)))
        val vm = makeVm(
            OverviewPeriod.DateRange(2026, 5, 1, 2026, 5, 31),
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertFalse(s.isEmpty)
            assertEquals(25.0, s.expenses)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
