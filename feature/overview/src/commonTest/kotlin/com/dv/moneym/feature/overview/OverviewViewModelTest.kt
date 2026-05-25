package com.dv.moneym.feature.overview

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
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
import com.dv.moneym.feature.overview.page.OverviewPageViewModel
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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class OverviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    private fun makePageVm(period: OverviewPeriod) = OverviewPageViewModel(
        period = period,
        transactionRepository = txnRepo,
        categoryRepository = catRepo,
        accountRepository = accountRepo,
        appSettingsRepository = settingsRepo,
        budgetRepository = budgetRepo,
        buildOverviewPageState = buildOverviewPageState,
        clock = clock,
    )

    private fun makeTxn(
        type: TransactionType,
        amount: Long,
        date: LocalDate = LocalDate(2026, 5, 10),
    ) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = type,
        amount = Money(amount, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun emptyMonthShowsEmptyState() = runTestWithDispatchers(testDispatcher) {
        val vm = makePageVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertTrue(state.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun incomeTotalsAggregatedCorrectly() = runTestWithDispatchers(testDispatcher) {
        txnRepo.upsert(makeTxn(TransactionType.INCOME, 10000))
        txnRepo.upsert(makeTxn(TransactionType.INCOME, 5000))
        val vm = makePageVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertFalse(state.isEmpty)
            assertEquals(150.0, state.income)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthViewHasDailyBars() = runTestWithDispatchers(testDispatcher) {
        val vm = makePageVm(OverviewPeriod.Month(YearMonth(2026, 5)))
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertIs<OverviewPeriod.Month>(state.period)
            assertEquals(31, state.dailyTotals.size) // May 2026 has 31 days
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun yearViewHasMonthlyBars() = runTestWithDispatchers(testDispatcher) {
        val vm = makePageVm(OverviewPeriod.Year(2026))
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertIs<OverviewPeriod.Year>(state.period)
            assertEquals(12, state.monthlyTotals.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
