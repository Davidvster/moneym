package com.dv.moneym.feature.transactions

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.feature.transactions.presentation.TransactionListIntent
import com.dv.moneym.feature.transactions.presentation.TransactionListViewModel
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private val eur = CurrencyCode("EUR")
    // Use a non-epoch instant so clock.today() returns a real recent date
    private val clockInstant = Instant.parse("2026-05-10T12:00:00Z")
    private val clock = FixedClock(clockInstant)
    private val today = clock.today()   // derived from clock — always in sync with ViewModel's month
    private val epoch = Instant.fromEpochMilliseconds(0)
    private val catId = CategoryId(1)
    private val accId = AccountId(1)

    private fun makeTxn(date: LocalDate = today, amount: Long = 500) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = TransactionType.EXPENSE,
        amount = Money(amount, eur),
        occurredOn = date,
        note = null,
        categoryId = catId,
        accountId = accId,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun initialStateIsLoading() {
        val vm = TransactionListViewModel(
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            appSettingsRepository = FakeAppSettingsRepository(),
            clock = clock,
        )
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun transactionsForCurrentMonthAppearInState() = runTestWithDispatchers {
        val txnRepo = FakeTransactionRepository()
        val catRepo = FakeCategoryRepository()
        val vm = TransactionListViewModel(txnRepo, catRepo, FakeAppSettingsRepository(), clock)

        val id = txnRepo.upsert(makeTxn(today))

        vm.state.test {
            // skip loading state
            val loaded = awaitItem().takeIf { !it.isLoading } ?: awaitItem()
            assertEquals(1, loaded.dayGroups.sumOf { it.transactions.size })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filterByTypeRemovesNonMatchingTransactions() = runTestWithDispatchers {
        val txnRepo = FakeTransactionRepository()
        val catRepo = FakeCategoryRepository()
        val vm = TransactionListViewModel(txnRepo, catRepo, FakeAppSettingsRepository(), clock)

        txnRepo.upsert(makeTxn())
        txnRepo.upsert(makeTxn().copy(type = TransactionType.INCOME))

        vm.state.test {
            // skip to loaded state
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            // apply expense filter
            vm.onIntent(TransactionListIntent.FilterChanged(TransactionFilter.ByType(TransactionType.EXPENSE)))
            val filtered = awaitItem()
            val txns = filtered.dayGroups.flatMap { it.transactions }
            assertTrue(txns.all { it.isExpense })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousMonthNavigationDecreasesMonth() = runTestWithDispatchers {
        val vm = TransactionListViewModel(FakeTransactionRepository(), FakeCategoryRepository(), FakeAppSettingsRepository(), clock)
        vm.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            val before = state.currentMonth
            vm.onIntent(TransactionListIntent.PreviousMonth)
            val after = awaitItem()
            assertEquals(before.previous(), after.currentMonth)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
