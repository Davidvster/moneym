package com.dv.moneym.feature.transactions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.banksync.BankSyncFailure
import com.dv.moneym.data.banksync.BankSyncFailureReason
import com.dv.moneym.data.banksync.BankSyncStatusProvider
import com.dv.moneym.data.sync.SyncConflict
import com.dv.moneym.data.sync.SyncFailure
import com.dv.moneym.data.sync.SyncFailureReason
import com.dv.moneym.data.sync.SyncStatusProvider
import com.dv.moneym.feature.transactions.list.TransactionListEphemeralState
import com.dv.moneym.feature.transactions.list.TransactionListIntent
import com.dv.moneym.feature.transactions.list.TransactionListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private class FakeSyncStatusProvider(
    syncing: Boolean = false,
    pendingCount: Int = 0,
    enabled: Boolean = true,
) : SyncStatusProvider {
    val syncingFlow = MutableStateFlow(syncing)
    val pendingCountFlow = MutableStateFlow(pendingCount)
    val failureFlow = MutableStateFlow<SyncFailure?>(null)
    override val isEnabled: Flow<Boolean> = MutableStateFlow(enabled)
    override val isSyncing: Flow<Boolean> = syncingFlow
    override val failure: Flow<SyncFailure?> = failureFlow
    override val pendingDeletionCount: Flow<Int> = pendingCountFlow
    override val conflict: Flow<SyncConflict?> = MutableStateFlow(null)
    override val lastSyncedMs: Flow<Long> = MutableStateFlow(0L)
}

private class FakeBankSyncStatusProvider(
    syncing: Boolean = false,
    enabled: Boolean = true,
) : BankSyncStatusProvider {
    val syncingFlow = MutableStateFlow(syncing)
    val failureFlow = MutableStateFlow<BankSyncFailure?>(null)
    override val isEnabled: Flow<Boolean> = MutableStateFlow(enabled)
    override val isSyncing: Flow<Boolean> = syncingFlow
    override val failure: Flow<BankSyncFailure?> = failureFlow
    override val pendingCount: Flow<Int> = MutableStateFlow(0)
    override val lastSyncedMs: Flow<Long> = MutableStateFlow(0L)
    var requestCount = 0
        private set

    override suspend fun requestSync() {
        requestCount++
    }
}

class TransactionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val eur = CurrencyCode("EUR")
    private val clockInstant = Instant.parse("2026-05-10T12:00:00Z")
    private val clock = FixedClock(clockInstant)
    private val today = clock.today()
    private val epoch = Instant.fromEpochMilliseconds(0)
    private val catId = CategoryId(1)
    private val accId = AccountId(1)

    private fun makeTxn(
        date: LocalDate = today,
        amount: Long = 500,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = type,
        amount = Money(amount, eur),
        occurredOn = date,
        note = null,
        categoryId = catId,
        accountId = accId,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun makeVm(
        txnRepo: FakeTransactionRepository = FakeTransactionRepository(),
        catRepo: FakeCategoryRepository = FakeCategoryRepository(),
        accRepo: FakeAccountRepository = FakeAccountRepository(),
        settings: FakeAppSettingsRepository = FakeAppSettingsRepository(),
        syncStatus: SyncStatusProvider = FakeSyncStatusProvider(),
        bankSyncStatus: BankSyncStatusProvider? = null,
    ) = TransactionListViewModel(
        transactionRepository = txnRepo,
        categoryRepository = catRepo,
        accountRepository = accRepo,
        appSettingsRepository = settings,
        ephemeralState = TransactionListEphemeralState(),
        syncStatus = syncStatus,
        bankSyncStatus = bankSyncStatus,
        clock = clock,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun initialStateHasNullCurrentMonth() {
        val vm = makeVm()
        assertNull(vm.state.value.currentMonth)
    }

    @Test
    fun loadedStateUsesClockMonth() = runTestWithDispatchers {
        val vm = makeVm()
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            assertEquals(today.year, s.currentMonth.year)
            assertEquals(today.month.ordinal + 1, s.currentMonth.monthNumber)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun totalsSplitIncomeAndExpense() = runTestWithDispatchers {
        val txnRepo = FakeTransactionRepository()
        val vm = makeVm(txnRepo = txnRepo)

        txnRepo.upsert(makeTxn(amount = 300, type = TransactionType.EXPENSE))
        txnRepo.upsert(makeTxn(amount = 700, type = TransactionType.INCOME))

        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null || (s.totalExpenses == 0L && s.totalIncome == 0L)) {
                s = awaitItem()
            }
            assertEquals(300L, s.totalExpenses)
            assertEquals(700L, s.totalIncome)
            assertEquals(400L, s.netAmount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filterChangedUpdatesActiveFilter() = runTestWithDispatchers {
        val vm = makeVm()
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            vm.onIntent(TransactionListIntent.FilterChanged(TransactionFilter.ByType(TransactionType.EXPENSE)))
            var after = awaitItem()
            while (after.activeFilter !is TransactionFilter.ByType) after = awaitItem()
            assertEquals(TransactionFilter.ByType(TransactionType.EXPENSE), after.activeFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousMonthDecreasesCurrentMonth() = runTestWithDispatchers {
        val vm = makeVm()
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            val before = s.currentMonth
            vm.onIntent(TransactionListIntent.PreviousMonth)
            var after = awaitItem()
            while (after.currentMonth == before) after = awaitItem()
            assertEquals(before.previous(), after.currentMonth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isSyncInProgressReflectsEngineRuntime() = runTestWithDispatchers {
        val sync = FakeSyncStatusProvider(syncing = false)
        val vm = makeVm(syncStatus = sync)
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            assertEquals(false, s.isSyncInProgress)
            sync.syncingFlow.value = true
            var after = awaitItem()
            while (!after.isSyncInProgress) after = awaitItem()
            assertEquals(true, after.isSyncInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pendingDeletionCountReflectsCountFlow() = runTestWithDispatchers {
        val sync = FakeSyncStatusProvider(pendingCount = 0)
        val vm = makeVm(syncStatus = sync)
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            assertEquals(0, s.pendingDeletionCount)
            sync.pendingCountFlow.value = 3
            var after = awaitItem()
            while (after.pendingDeletionCount == 0) after = awaitItem()
            assertEquals(3, after.pendingDeletionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cloudSyncFailureReflectsFailureFlow() = runTestWithDispatchers {
        val sync = FakeSyncStatusProvider()
        val vm = makeVm(syncStatus = sync)
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            assertNull(s.syncFailure)
            sync.failureFlow.value = SyncFailure(SyncFailureReason.Network)
            var after = awaitItem()
            while (after.syncFailure == null) after = awaitItem()
            assertEquals(SyncFailureReason.Network, after.syncFailure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bankSyncFailureReflectsFailureFlow() = runTestWithDispatchers {
        val bank = FakeBankSyncStatusProvider()
        val vm = makeVm(bankSyncStatus = bank)
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            assertNull(s.bankSyncFailure)
            bank.failureFlow.value = BankSyncFailure(BankSyncFailureReason.Network)
            var after = awaitItem()
            while (after.bankSyncFailure == null) after = awaitItem()
            assertEquals(BankSyncFailureReason.Network, after.bankSyncFailure)
            assertEquals(false, after.bankSyncReconnectRequired)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bankSyncReconnectRequiredReflectsFailureFlow() = runTestWithDispatchers {
        val bank = FakeBankSyncStatusProvider()
        val vm = makeVm(bankSyncStatus = bank)
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            bank.failureFlow.value = BankSyncFailure(
                reason = BankSyncFailureReason.Auth,
                reconnectRequired = true,
            )
            var after = awaitItem()
            while (!after.bankSyncReconnectRequired) after = awaitItem()
            assertEquals(BankSyncFailureReason.Auth, after.bankSyncFailure)
            assertEquals(true, after.bankSyncReconnectRequired)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
