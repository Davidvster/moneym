package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WalletManageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun account(id: Long, name: String = "W$id") = Account(
        id = AccountId(id),
        name = name,
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun txn(id: Long, accId: Long) = Transaction(
        id = TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(1000, CurrencyCode("EUR")),
        occurredOn = LocalDate(2026, 1, 1),
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(accId),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun vm(
        accountRepo: FakeAccountRepository = FakeAccountRepository(),
        txRepo: FakeTransactionRepository = FakeTransactionRepository(),
        settings: FakeAppSettingsRepository = FakeAppSettingsRepository(),
    ) = WalletManageViewModel(accountRepo, txRepo, settings, SavedStateHandle())

    @Test
    fun stateExposesAccountsAndSelectedId() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1), account(2))) }
        val settings = FakeAppSettingsRepository().apply { setSelectedAccountId(2) }
        val vm = vm(accountRepo = accountRepo, settings = settings)
        vm.state.test {
            var s = awaitItem()
            while (s.accounts.size != 2 || s.selectedAccountId != 2L) s = awaitItem()
            assertEquals(2, s.accounts.size)
            assertEquals(2L, s.selectedAccountId)
            assertNull(s.pendingDeleteId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectAccountPersistsToSettings() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1), account(2))) }
        val settings = FakeAppSettingsRepository()
        val vm = vm(accountRepo = accountRepo, settings = settings)
        vm.state.test {
            var s = awaitItem()
            while (s.accounts.size != 2) s = awaitItem()
            vm.onIntent(WalletManageIntent.SelectAccount(2))
            var after = awaitItem()
            while (after.selectedAccountId != 2L) after = awaitItem()
            assertEquals(2L, after.selectedAccountId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addWalletInsertsAccountFirstBecomesDefault() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository()
        val vm = vm(accountRepo = accountRepo)
        vm.state.test {
            awaitItem()
            vm.onIntent(WalletManageIntent.AddWallet("Cash", "USD"))
            var s = awaitItem()
            while (s.accounts.isEmpty()) s = awaitItem()
            assertEquals("Cash", s.accounts.single().name)
            assertEquals(CurrencyCode("USD"), s.accounts.single().currency)
            assertTrue(s.accounts.single().isDefault)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteRequestedForLastWalletShowsBlockedDialog() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val vm = vm(accountRepo = accountRepo)
        vm.state.test {
            var s = awaitItem()
            while (s.accounts.isEmpty()) s = awaitItem()
            vm.onIntent(WalletManageIntent.DeleteRequested(1))
            var blocked = awaitItem()
            while (!blocked.showLastWalletDeleteBlockedDialog) blocked = awaitItem()
            assertNull(blocked.pendingDeleteId)
            assertTrue(blocked.showLastWalletDeleteBlockedDialog)
            vm.onIntent(WalletManageIntent.LastWalletDeleteBlockedDismissed)
            var dismissed = awaitItem()
            while (dismissed.showLastWalletDeleteBlockedDialog) dismissed = awaitItem()
            assertNull(dismissed.pendingDeleteId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteRequestedWithMultipleWalletsCanBeCancelled() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1), account(2))) }
        val vm = vm(accountRepo = accountRepo)
        vm.state.test {
            var s = awaitItem()
            while (s.accounts.size != 2) s = awaitItem()
            vm.onIntent(WalletManageIntent.DeleteRequested(1))
            var pending = awaitItem()
            while (pending.pendingDeleteId != 1L) pending = awaitItem()
            assertEquals(1L, pending.pendingDeleteId)
            assertEquals(false, pending.showLastWalletDeleteBlockedDialog)
            vm.onIntent(WalletManageIntent.DeleteCancelled)
            var cancelled = awaitItem()
            while (cancelled.pendingDeleteId != null) cancelled = awaitItem()
            assertNull(cancelled.pendingDeleteId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteConfirmedRemovesAccountAndItsTransactions() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1), account(2))) }
        val txRepo = FakeTransactionRepository().apply { addAll(listOf(txn(10, 1), txn(11, 2))) }
        val vm = vm(accountRepo = accountRepo, txRepo = txRepo)
        vm.state.test {
            var s = awaitItem()
            while (s.accounts.size != 2) s = awaitItem()
            vm.onIntent(WalletManageIntent.DeleteRequested(1))
            assertEquals(1L, awaitItem().pendingDeleteId)
            vm.onIntent(WalletManageIntent.DeleteConfirmed)
            assertNull(awaitItem().pendingDeleteId)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
        assertEquals(listOf(2L), accountRepo.accounts.map { it.id.value })
        assertEquals(listOf(11L), txRepo.transactions.map { it.id.value })
    }

    @Test
    fun deleteConfirmedWithoutPendingIsNoOp() = runTest(testDispatcher) {
        val accountRepo = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val vm = vm(accountRepo = accountRepo)
        vm.state.test {
            var s = awaitItem()
            while (s.accounts.isEmpty()) s = awaitItem()
            vm.onIntent(WalletManageIntent.DeleteConfirmed)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, accountRepo.accounts.size)
    }
}
