package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EditWalletCurrencyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun account(id: Long, currency: String) = Account(
        id = AccountId(id),
        name = "W",
        type = AccountType.CASH,
        currency = CurrencyCode(currency),
        isDefault = true,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun txn(id: Long, accId: Long, minor: Long, currency: String) = Transaction(
        id = if (id == 0L) UNSAVED_TRANSACTION_ID else com.dv.moneym.core.model.TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(minor, CurrencyCode(currency)),
        occurredOn = LocalDate(2026, 1, 1),
        note = null,
        categoryId = com.dv.moneym.core.model.CategoryId(1),
        accountId = AccountId(accId),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun vm(
        id: Long = 1,
        currentCurrency: String = "EUR",
        accountRepo: FakeAccountRepository = FakeAccountRepository(),
        txRepo: FakeTransactionRepository = FakeTransactionRepository(),
    ) = EditWalletCurrencyViewModel(
        accountId = id,
        currentCurrency = currentCurrency,
        accountRepository = accountRepo,
        transactionRepository = txRepo,
        dispatchers = TestDispatcherProvider(testDispatcher),
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun selectingSameCurrencyIsIgnored() = runTest(testDispatcher) {
        val vm = vm(currentCurrency = "EUR")
        vm.state.test {
            val initial = awaitItem()
            assertEquals("EUR", initial.currentCurrency)
            vm.onIntent(EditWalletCurrencyIntent.CurrencySelected("EUR"))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectingNewCurrencyOpensConfirmSheet() = runTest(testDispatcher) {
        val vm = vm(currentCurrency = "EUR")
        vm.state.test {
            skipItems(1)
            vm.onIntent(EditWalletCurrencyIntent.CurrencySelected("USD"))
            val s = awaitItem()
            assertEquals("USD", s.selectedCurrency)
            assertTrue(s.showConfirmSheet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rateChangeFiltersToDigitsAndDot() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            vm.onIntent(EditWalletCurrencyIntent.RateChanged("1a.2b3"))
            assertEquals("1.23", awaitItem().conversionRate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchQueryUpdates() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            vm.onIntent(EditWalletCurrencyIntent.SearchQueryChanged("dollar"))
            assertEquals("dollar", awaitItem().searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cancelClearsSheetAndSelection() = runTest(testDispatcher) {
        val vm = vm(currentCurrency = "EUR")
        vm.state.test {
            skipItems(1)
            vm.onIntent(EditWalletCurrencyIntent.CurrencySelected("USD"))
            awaitItem()
            vm.onIntent(EditWalletCurrencyIntent.CancelConversion)
            val s = awaitItem()
            assertFalse(s.showConfirmSheet)
            assertNull(s.selectedCurrency)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmConversionConvertsTransactionsUpdatesAccountAndEmitsDone() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1, "EUR"))) }
        val txRepo = FakeTransactionRepository().apply { addAll(listOf(txn(5, 1, 1000, "EUR"))) }
        val vm = vm(id = 1, currentCurrency = "EUR", accountRepo = accountRepo, txRepo = txRepo)

        vm.onIntent(EditWalletCurrencyIntent.CurrencySelected("USD"))
        vm.onIntent(EditWalletCurrencyIntent.RateChanged("2"))

        vm.effects.test {
            vm.onIntent(EditWalletCurrencyIntent.ConfirmConversion)
            assertIs<EditWalletCurrencyEffect.Done>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(CurrencyCode("USD"), accountRepo.getById(AccountId(1))!!.currency)
        val converted = txRepo.transactions.single()
        assertEquals(2000, converted.amount.minorUnits)
        assertEquals(CurrencyCode("USD"), converted.amount.currency)
    }

    @Test
    fun confirmWithInvalidRateDoesNotConvert() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1, "EUR"))) }
        val txRepo = FakeTransactionRepository().apply { addAll(listOf(txn(5, 1, 1000, "EUR"))) }
        val vm = vm(id = 1, currentCurrency = "EUR", accountRepo = accountRepo, txRepo = txRepo)

        vm.onIntent(EditWalletCurrencyIntent.CurrencySelected("USD"))
        vm.onIntent(EditWalletCurrencyIntent.RateChanged("0"))
        vm.effects.test {
            vm.onIntent(EditWalletCurrencyIntent.ConfirmConversion)
            expectNoEvents()
        }
        assertEquals(CurrencyCode("EUR"), accountRepo.getById(AccountId(1))!!.currency)
        assertEquals(1000, txRepo.transactions.single().amount.minorUnits)
    }
}
