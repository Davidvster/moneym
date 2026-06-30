package com.dv.moneym.feature.transactions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.selectedCategoryIds
import com.dv.moneym.core.model.withType
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
    val conflictFlow = MutableStateFlow<SyncConflict?>(null)
    override val isEnabled: Flow<Boolean> = MutableStateFlow(enabled)
    override val isSyncing: Flow<Boolean> = syncingFlow
    override val failure: Flow<SyncFailure?> = failureFlow
    override val pendingDeletionCount: Flow<Int> = pendingCountFlow
    override val conflict: Flow<SyncConflict?> = conflictFlow
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
    fun typeFilterChangesRequestScrollToTop() = runTestWithDispatchers {
        val vm = makeVm()
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            assertEquals(0, s.typeFilterScrollRequest)

            vm.onIntent(TransactionListIntent.FilterChanged(TransactionFilter.ByType(TransactionType.EXPENSE)))
            var afterExpense = awaitItem()
            while (
                afterExpense.typeFilterScrollRequest != 1 ||
                afterExpense.activeFilter != TransactionFilter.ByType(TransactionType.EXPENSE)
            ) {
                afterExpense = awaitItem()
            }
            assertEquals(TransactionFilter.ByType(TransactionType.EXPENSE), afterExpense.activeFilter)

            vm.onIntent(TransactionListIntent.FilterChanged(TransactionFilter.ByType(TransactionType.INCOME)))
            var afterIncome = awaitItem()
            while (
                afterIncome.typeFilterScrollRequest != 2 ||
                afterIncome.activeFilter != TransactionFilter.ByType(TransactionType.INCOME)
            ) {
                afterIncome = awaitItem()
            }
            assertEquals(TransactionFilter.ByType(TransactionType.INCOME), afterIncome.activeFilter)

            vm.onIntent(TransactionListIntent.FilterChanged(TransactionFilter.None))
            var afterAll = awaitItem()
            while (
                afterAll.typeFilterScrollRequest != 3 ||
                afterAll.activeFilter != TransactionFilter.None
            ) {
                afterAll = awaitItem()
            }
            assertEquals(TransactionFilter.None, afterAll.activeFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryFilterPersistsAndSurvivesViewModelRecreation() = runTestWithDispatchers {
        val settings = FakeAppSettingsRepository()
        val firstVm = makeVm(settings = settings)

        firstVm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null) s = awaitItem()
            firstVm.onIntent(TransactionListIntent.CategoryFilterToggled(CategoryId(7)))
            var filtered = awaitItem()
            while (CategoryId(7) !in filtered.selectedCategoryIds) filtered = awaitItem()
            assertEquals(setOf(CategoryId(7)), filtered.selectedCategoryIds)
            cancelAndIgnoreRemainingEvents()
        }

        val recreatedVm = makeVm(settings = settings)
        recreatedVm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null || CategoryId(7) !in s.selectedCategoryIds) s = awaitItem()
            assertEquals(setOf(CategoryId(7)), s.selectedCategoryIds)
            assertEquals(setOf(CategoryId(7)), s.activeFilter.selectedCategoryIds())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun typeFilterPreservesPersistedCategorySelection() = runTestWithDispatchers {
        val settings = FakeAppSettingsRepository()
        settings.setLastTransactionFilter(TransactionFilter.ByCategory(CategoryId(3)))
        val vm = makeVm(settings = settings)

        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null || CategoryId(3) !in s.selectedCategoryIds) s = awaitItem()
            vm.onIntent(
                TransactionListIntent.FilterChanged(
                    s.activeFilter.withType(TransactionType.INCOME)
                )
            )
            var updated = awaitItem()
            while (updated.activeFilter !is TransactionFilter.ByCategoryAndType) updated = awaitItem()
            assertEquals(
                TransactionFilter.ByCategoryAndType(CategoryId(3), TransactionType.INCOME),
                updated.activeFilter,
            )
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
    fun displayPrefsAndAttentionCountReflectSettingsAndSyncState() = runTestWithDispatchers {
        val settings = FakeAppSettingsRepository()
        settings.setTxDisplayPrefs(TxDisplayPrefs(showSyncSuggestionBanner = false))
        val sync = FakeSyncStatusProvider(pendingCount = 2)
        sync.conflictFlow.value = SyncConflict(remoteEncrypted = true)
        val vm = makeVm(settings = settings, syncStatus = sync)
        vm.state.test {
            var s = awaitItem()
            while (s.currentMonth == null || s.syncAttentionCount == 0) s = awaitItem()
            assertEquals(false, s.txDisplayPrefs.showSyncSuggestionBanner)
            assertEquals(3, s.syncAttentionCount)
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
