package com.dv.moneym.feature.overview

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
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

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-10T12:00:00Z"))
    private val txnRepo = FakeTransactionRepository()
    private val catRepo = FakeCategoryRepository()

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
        val vm = OverviewViewModel(txnRepo, catRepo, clock)
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
        val vm = OverviewViewModel(txnRepo, catRepo, clock)
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertFalse(state.isEmpty)
            assertEquals(15000L, state.totalIncome.firstOrNull()?.minorUnits)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthViewHasDailyBars() = runTestWithDispatchers(testDispatcher) {
        val vm = OverviewViewModel(txnRepo, catRepo, clock)
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertIs<OverviewPeriod.Month>(state.period)
            assertEquals(31, state.chartBars.size) // May 2026 has 31 days
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun yearViewHasMonthlyBars() = runTestWithDispatchers(testDispatcher) {
        val vm = OverviewViewModel(txnRepo, catRepo, clock)
        vm.onIntent(OverviewIntent.TogglePeriod)
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertIs<OverviewPeriod.Year>(state.period)
            assertEquals(12, state.chartBars.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
