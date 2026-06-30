package com.dv.moneym.feature.transactions

import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.PaymentModeSyncRow
import com.dv.moneym.feature.transactions.list.TransactionListEphemeralState
import com.dv.moneym.feature.transactions.list.page.BulkSheetState
import com.dv.moneym.feature.transactions.list.page.TransactionPageIntent
import com.dv.moneym.feature.transactions.list.page.TransactionPageViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class TransactionPageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val eur = CurrencyCode("EUR")
    private val usd = CurrencyCode("USD")
    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-10T12:00:00Z"))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePaymentModeRepository(
        initial: List<PaymentMode> = emptyList(),
    ) : PaymentModeRepository {
        private val modes = MutableStateFlow(initial)
        private val instant = Instant.fromEpochMilliseconds(0)
        override fun observeAll(): Flow<List<PaymentMode>> = modes.asStateFlow()
        override suspend fun getById(id: PaymentModeId): PaymentMode? = modes.value.firstOrNull { it.id == id }
        override suspend fun create(name: String) {
            val id = PaymentModeId((modes.value.maxOfOrNull { it.id.value } ?: 0L) + 1L)
            modes.update { it + PaymentMode(id, name, instant, instant) }
        }
        override suspend fun rename(id: PaymentModeId, name: String) {
            modes.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }
        }
        override suspend fun delete(id: PaymentModeId) {
            modes.update { list -> list.filterNot { it.id == id } }
        }
        override suspend fun markDeletedBySyncId(syncId: String, now: Long) = Unit
        override suspend fun reviveBySyncId(syncId: String, now: Long) = Unit
        override suspend fun exportForSync(): List<PaymentModeSyncRow> = emptyList()
        override suspend fun upsertFromSync(row: PaymentModeSyncRow): Long = 0L
    }

    private fun account(
        id: Long,
        name: String,
        currency: CurrencyCode,
        isDefault: Boolean = false,
    ) = Account(
        id = AccountId(id),
        name = name,
        type = AccountType.CASH,
        currency = currency,
        isDefault = isDefault,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun category(
        id: Long,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Category(
        id = CategoryId(id),
        name = "Cat$id",
        iconKey = "tag",
        colorHex = "#4CAF50",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = type,
    )

    private fun paymentMode(id: Long, name: String) =
        PaymentMode(PaymentModeId(id), name, epoch, epoch)

    private fun txn(
        amount: Long = 100,
        currency: CurrencyCode = eur,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: CategoryId = CategoryId(1),
        accountId: AccountId = AccountId(1),
        paymentModeId: PaymentModeId? = null,
    ) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = type,
        amount = Money(amount, currency),
        occurredOn = LocalDate(2026, 5, 10),
        note = null,
        categoryId = categoryId,
        accountId = accountId,
        createdAt = epoch,
        updatedAt = epoch,
        paymentModeId = paymentModeId,
    )

    private suspend fun fixture(
        paymentModeEnabled: Boolean = false,
        paymentModes: List<PaymentMode> = emptyList(),
    ): Pair<TransactionPageViewModel, FakeTransactionRepository> {
        val transactions = FakeTransactionRepository()
        val categories = FakeCategoryRepository().apply {
            addAll(
                listOf(
                    category(1, TransactionType.EXPENSE),
                    category(2, TransactionType.INCOME),
                )
            )
        }
        val accounts = FakeAccountRepository().apply {
            addAll(
                listOf(
                    account(1, "Cash", eur, isDefault = true),
                    account(2, "Bank", usd),
                )
            )
        }
        val settings = FakeAppSettingsRepository().apply {
            setPaymentModeEnabled(paymentModeEnabled)
        }
        val vm = TransactionPageViewModel(
            yearMonth = YearMonth(2026, 5),
            transactionRepository = transactions,
            recurringTransactionRepository = FakeRecurringTransactionRepository(),
            categoryRepository = categories,
            accountRepository = accounts,
            paymentModeRepository = FakePaymentModeRepository(paymentModes),
            appSettingsRepository = settings,
            clock = clock,
            ephemeralState = TransactionListEphemeralState(),
        )
        return vm to transactions
    }

    @Test
    fun selectionStartsTogglesAndClears() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val first = transactions.upsert(txn(amount = 100))
        val second = transactions.upsert(txn(amount = 200))

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()

            vm.onIntent(TransactionPageIntent.TransactionLongPressed(first))
            state = awaitItem()
            while (state.selection.selectedIds != setOf(first)) state = awaitItem()
            assertEquals(1, state.selection.selectedCount)
            assertEquals(-100L, state.selection.currencyTotals.single().minorUnits)

            vm.onIntent(TransactionPageIntent.TransactionPressed(second))
            state = awaitItem()
            while (state.selection.selectedIds != setOf(first, second)) state = awaitItem()
            assertEquals(-300L, state.selection.currencyTotals.single().minorUnits)

            vm.onIntent(TransactionPageIntent.ClearSelection)
            state = awaitItem()
            while (state.selection.selectedIds.isNotEmpty()) state = awaitItem()
            assertEquals(0, state.selection.selectedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bulkDeleteRemovesSelectedTransactions() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val first = transactions.upsert(txn(amount = 100))
        val second = transactions.upsert(txn(amount = 200))
        val untouched = transactions.upsert(txn(amount = 300))

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()

            vm.onIntent(TransactionPageIntent.TransactionLongPressed(first))
            vm.onIntent(TransactionPageIntent.TransactionPressed(second))
            vm.onIntent(TransactionPageIntent.DeleteRequested)
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.DeleteConfirm) state = awaitItem()

            vm.onIntent(TransactionPageIntent.ConfirmDelete)
            state = awaitItem()
            while (state.selection.selectedIds.isNotEmpty()) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(untouched), transactions.transactions.map { it.id })
    }

    @Test
    fun bulkCategoryChangesSelectedTransactionTypeToCategoryType() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val id = transactions.upsert(txn(type = TransactionType.EXPENSE, categoryId = CategoryId(1)))

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(id))
            vm.onIntent(TransactionPageIntent.CategoryPicked(CategoryId(2)))
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.CategoryConfirm) state = awaitItem()
            vm.onIntent(TransactionPageIntent.ConfirmCategory)
            state = awaitItem()
            while (state.selection.selectedIds.isNotEmpty()) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val changed = transactions.getById(id)!!
        assertEquals(CategoryId(2), changed.categoryId)
        assertEquals(TransactionType.INCOME, changed.type)
    }

    @Test
    fun bulkWalletMoveDefaultsCurrencyMismatchRateToOneAndAppliesIt() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val id = transactions.upsert(txn(amount = 100, currency = eur, accountId = AccountId(1)))
        val target = account(2, "Bank", usd)

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(id))
            vm.onIntent(TransactionPageIntent.WalletPicked(target))
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.WalletConfirm) state = awaitItem()
            val sheet = assertIs<BulkSheetState.WalletConfirm>(state.bulkSheet)
            assertEquals(true, sheet.requiresRate)
            assertEquals("1", state.bulkRateText)
            vm.onIntent(TransactionPageIntent.ConfirmWallet)
            state = awaitItem()
            while (state.selection.selectedIds.isNotEmpty()) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(Money(100, usd), transactions.getById(id)!!.amount)
        assertEquals(AccountId(2), transactions.getById(id)!!.accountId)
    }

    @Test
    fun pickerDismissAfterCategorySelectionDoesNotClearConfirmSheet() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val id = transactions.upsert(txn(type = TransactionType.EXPENSE, categoryId = CategoryId(1)))

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(id))
            vm.onIntent(TransactionPageIntent.CategoryPicked(CategoryId(2)))
            vm.onIntent(TransactionPageIntent.DismissBulkSheet)
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.CategoryConfirm) state = awaitItem()
            assertIs<BulkSheetState.CategoryConfirm>(state.bulkSheet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pickerDismissAfterWalletSelectionDoesNotClearConfirmSheet() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val id = transactions.upsert(txn(amount = 100, currency = eur, accountId = AccountId(1)))
        val target = account(2, "Bank", usd)

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(id))
            vm.onIntent(TransactionPageIntent.WalletPicked(target))
            vm.onIntent(TransactionPageIntent.DismissBulkSheet)
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.WalletConfirm) state = awaitItem()
            assertIs<BulkSheetState.WalletConfirm>(state.bulkSheet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invalidWalletMoveRateShowsErrorWithoutApplyingChanges() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val id = transactions.upsert(txn(amount = 100, currency = eur, accountId = AccountId(1)))
        val target = account(2, "Bank", usd)

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(id))
            vm.onIntent(TransactionPageIntent.WalletPicked(target))
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.WalletConfirm) state = awaitItem()

            vm.onIntent(TransactionPageIntent.WalletRateChanged("0"))
            vm.onIntent(TransactionPageIntent.ConfirmWallet)
            state = awaitItem()
            while (!state.bulkRateError) state = awaitItem()

            assertEquals(setOf(id), state.selection.selectedIds)
            assertIs<BulkSheetState.WalletConfirm>(state.bulkSheet)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(Money(100, eur), transactions.getById(id)!!.amount)
        assertEquals(AccountId(1), transactions.getById(id)!!.accountId)
    }

    @Test
    fun bulkWalletMoveSameCurrencyDoesNotRequireRate() = runTestWithDispatchers(testDispatcher) {
        val (vm, transactions) = fixture()
        val id = transactions.upsert(txn(amount = 100, currency = eur, accountId = AccountId(1)))
        val target = account(3, "Savings", eur)

        vm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(id))
            vm.onIntent(TransactionPageIntent.WalletPicked(target))
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.WalletConfirm) state = awaitItem()
            val sheet = assertIs<BulkSheetState.WalletConfirm>(state.bulkSheet)
            assertEquals(false, sheet.requiresRate)
            vm.onIntent(TransactionPageIntent.ConfirmWallet)
            state = awaitItem()
            while (state.selection.selectedIds.isNotEmpty()) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(Money(100, eur), transactions.getById(id)!!.amount)
        assertEquals(AccountId(3), transactions.getById(id)!!.accountId)
    }

    @Test
    fun paymentModeEligibilityRequiresEnabledAndMultipleModes() = runTestWithDispatchers(testDispatcher) {
        val cash = paymentMode(1, "Cash")
        val card = paymentMode(2, "Card")
        val (disabledVm, disabledTransactions) = fixture(
            paymentModeEnabled = false,
            paymentModes = listOf(cash, card),
        )
        val disabledId = disabledTransactions.upsert(txn())

        disabledVm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            disabledVm.onIntent(TransactionPageIntent.TransactionLongPressed(disabledId))
            state = awaitItem()
            while (state.selection.selectedIds.isEmpty()) state = awaitItem()
            assertEquals(false, state.selection.canMovePaymentMode)
            cancelAndIgnoreRemainingEvents()
        }

        val (enabledVm, enabledTransactions) = fixture(
            paymentModeEnabled = true,
            paymentModes = listOf(cash, card),
        )
        val enabledId = enabledTransactions.upsert(txn(paymentModeId = cash.id))

        enabledVm.state.test {
            var state = awaitItem()
            while (state.dayGroups.isEmpty()) state = awaitItem()
            enabledVm.onIntent(TransactionPageIntent.TransactionLongPressed(enabledId))
            state = awaitItem()
            while (!state.selection.canMovePaymentMode) state = awaitItem()
            enabledVm.onIntent(TransactionPageIntent.PaymentModePicked(card.id))
            state = awaitItem()
            while (state.bulkSheet !is BulkSheetState.PaymentModeConfirm) state = awaitItem()
            enabledVm.onIntent(TransactionPageIntent.ConfirmPaymentMode)
            state = awaitItem()
            while (state.selection.selectedIds.isNotEmpty()) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(card.id, enabledTransactions.getById(enabledId)!!.paymentModeId)
    }
}
