package com.dv.moneym.feature.transactionedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SuggestionSource
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import com.dv.moneym.feature.transactionedit.usecase.ComputeCategoryBudgetRemainingUseCase
import com.dv.moneym.feature.transactionedit.usecase.SuggestNotesUseCase
import com.dv.moneym.feature.transactionedit.usecase.ValidateAndBuildTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TransactionEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-10T12:00:00Z"))
    private val today = LocalDate(2026, 5, 10)

    private fun account(id: Long, isDefault: Boolean = false) = Account(
        id = AccountId(id),
        name = "W$id",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = isDefault,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun category(id: Long, type: TransactionType) = Category(
        id = CategoryId(id),
        name = "C$id",
        iconKey = "i",
        colorHex = "#FFFFFF",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = type,
    )

    private class Deps {
        val txRepo = FakeTransactionRepository()
        val catRepo = FakeCategoryRepository()
        val accountRepo = FakeAccountRepository()
        val recurringRepo = FakeRecurringTransactionRepository()
        val budgetRepo = FakeBudgetRepository()
        val settings = FakeAppSettingsRepository()
        val paymentModes = FakePaymentModeRepository()
    }

    private fun vm(
        editingId: TransactionId?,
        deps: Deps,
        draft: TransactionEditDraft? = null,
        suggestionSources: Map<String, SuggestionSource> = emptyMap(),
    ): TransactionEditViewModel {
        val dispatchers = TestDispatcherProvider(testDispatcher)
        return TransactionEditViewModel(
            editingId = editingId,
            draft = draft,
            getTransaction = GetTransactionUseCase(deps.txRepo),
            upsertTransaction = UpsertTransactionUseCase(deps.txRepo),
            deleteTransaction = DeleteTransactionUseCase(deps.txRepo),
            validateAndBuildTransaction = ValidateAndBuildTransactionUseCase(),
            categoryRepository = deps.catRepo,
            accountRepository = deps.accountRepo,
            transactionRepository = deps.txRepo,
            recurringTransactionRepository = deps.recurringRepo,
            appSettingsRepository = deps.settings,
            paymentModeRepository = deps.paymentModes,
            computeBudgetRemaining = ComputeCategoryBudgetRemainingUseCase(deps.budgetRepo, deps.txRepo),
            suggestNotes = SuggestNotesUseCase(),
            dispatchers = dispatchers,
            clock = clock,
            suggestionSources = suggestionSources,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private class FakeSuggestionSource : SuggestionSource {
        private val pending = MutableStateFlow<List<SuggestionRecord>>(emptyList())
        private val rejected = MutableStateFlow<List<SuggestionRecord>>(emptyList())

        var acceptedId: Long? = null
        var acceptedTransactionId: Long? = null
        var acceptedAt: Long? = null

        override fun observePending(): Flow<List<SuggestionRecord>> = pending
        override fun observeRejected(): Flow<List<SuggestionRecord>> = rejected
        override fun observePendingCount(): Flow<Int> = MutableStateFlow(pending.value.size)
        override suspend fun getRecord(id: Long): SuggestionRecord? =
            pending.value.firstOrNull { it.id == id }

        override suspend fun accept(id: Long, createdTransactionId: Long, decidedAt: Long) {
            acceptedId = id
            acceptedTransactionId = createdTransactionId
            acceptedAt = decidedAt
        }

        override suspend fun reject(id: Long, decidedAt: Long) = Unit
        override suspend fun restoreToPending(id: Long) = Unit
    }

    private fun seedCatsAccounts(deps: Deps) {
        deps.catRepo.addAll(
            listOf(category(1, TransactionType.EXPENSE), category(2, TransactionType.INCOME)),
        )
        deps.accountRepo.addAll(listOf(account(1, isDefault = true), account(2)))
    }

    @Test
    fun newTransactionPreSelectsDefaultAccountAndFirstCategory() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedAccountId == null || s.selectedCategoryId == null) s = awaitItem()
            assertFalse(s.isEditMode)
            assertEquals(AccountId(1), s.selectedAccountId)
            assertEquals(CategoryId(1), s.selectedCategoryId)
            assertEquals(today, s.date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editLoadsExistingTransaction() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val id = deps.txRepo.upsert(
            Transaction(
                id = UNSAVED_TRANSACTION_ID,
                type = TransactionType.EXPENSE,
                amount = Money(2599, CurrencyCode("EUR")),
                occurredOn = LocalDate(2026, 5, 3),
                note = "Lunch",
                categoryId = CategoryId(1),
                accountId = AccountId(2),
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        val vm = vm(id, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading || s.existingId == null) s = awaitItem()
            assertTrue(s.isEditMode)
            assertEquals("25.99", s.amountText)
            assertEquals(LocalDate(2026, 5, 3), s.date)
            assertEquals(CategoryId(1), s.selectedCategoryId)
            assertEquals(AccountId(2), s.selectedAccountId)
            assertEquals("Lunch", s.note)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun newTransactionPrefillsFromDraftAfterReferencesLoad() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val draft = suggestionDraft()
        val vm = vm(null, deps, draft = draft)
        vm.state.test {
            var s = awaitItem()
            while (!s.draftApplied) s = awaitItem()
            assertFalse(s.isEditMode)
            assertEquals(TransactionType.INCOME, s.type)
            assertEquals("98.76", s.amountText)
            assertEquals(LocalDate(2026, 5, 5), s.date)
            assertEquals(AccountId(2), s.selectedAccountId)
            assertEquals(CategoryId(2), s.selectedCategoryId)
            assertEquals("Wallet refund", s.note)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun typeChangeResetsCategoryAndAmountFiltering() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME))
            var afterType = awaitItem()
            while (afterType.type != TransactionType.INCOME) afterType = awaitItem()
            assertEquals(TransactionType.INCOME, afterType.type)
            vm.onIntent(TransactionEditIntent.AmountChanged("12.3a45"))
            var amt = awaitItem()
            while (amt.amountText != "12.34") amt = awaitItem()
            assertEquals("12.34", amt.amountText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun yesterdayTodayTogglesDate() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.date == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.YesterdayTodayClicked)
            var yesterday = awaitItem()
            while (yesterday.date == today) yesterday = awaitItem()
            assertEquals(LocalDate(2026, 5, 9), yesterday.date)
            assertFalse(yesterday.isToday == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun accountSelectedPersistsToSettings() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedAccountId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.AccountSelected(AccountId(2)))
            var sel = awaitItem()
            while (sel.selectedAccountId != AccountId(2)) sel = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
        assertEquals(2L, deps.settings.observeSelectedAccountId().first())
    }

    @Test
    fun paymentModeShownWhenEnabled() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        deps.settings.setPaymentModeEnabled(true)
        deps.paymentModes.create("Card")
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (!s.showPaymentMode || s.paymentModes.isEmpty()) s = awaitItem()
            assertTrue(s.showPaymentMode)
            assertEquals("Card", s.paymentModes.single().name)
            vm.onIntent(TransactionEditIntent.PaymentModeSelected(PaymentModeId(1)))
            var sel = awaitItem()
            while (sel.selectedPaymentModeId != PaymentModeId(1)) sel = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noteSuggestionsDerivedFromHistory() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        deps.txRepo.addAll(
            listOf(
                Transaction(
                    id = TransactionId(99),
                    type = TransactionType.EXPENSE,
                    amount = Money(500, CurrencyCode("EUR")),
                    occurredOn = today,
                    note = "Coffee",
                    categoryId = CategoryId(1),
                    accountId = AccountId(1),
                    createdAt = epoch,
                    updatedAt = epoch,
                ),
            ),
        )
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.NoteChanged("Cof"))
            var withSugg = awaitItem()
            while (withSugg.noteSuggestions.isEmpty()) withSugg = awaitItem()
            assertEquals(listOf("Coffee"), withSugg.noteSuggestions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noteSelectedAdoptsCategoryOfMatchingTransaction() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        deps.txRepo.addAll(
            listOf(
                Transaction(
                    id = TransactionId(99),
                    type = TransactionType.EXPENSE,
                    amount = Money(500, CurrencyCode("EUR")),
                    occurredOn = today,
                    note = "Groceries",
                    categoryId = CategoryId(1),
                    accountId = AccountId(1),
                    createdAt = epoch,
                    updatedAt = epoch,
                ),
            ),
        )
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.NoteSelected("Groceries"))
            var sel = awaitItem()
            while (sel.note != "Groceries") sel = awaitItem()
            assertEquals(CategoryId(1), sel.selectedCategoryId)
            assertTrue(sel.noteSuggestions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetRemainingComputedForExpenseWithBudget() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        deps.budgetRepo.addAll(
            listOf(
                Budget(
                    id = BudgetId(1),
                    name = "Food",
                    amount = Money(10000, CurrencyCode("EUR")),
                    categoryId = CategoryId(1),
                    accountId = AccountId(0),
                    periodType = BudgetPeriodType.MONTHLY,
                    startYearMonth = YearMonth(2026, 5),
                    recurringMonths = Budget.UNLIMITED,
                    createdAt = epoch,
                    updatedAt = epoch,
                ),
            ),
        )
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.budgetRemaining == null) s = awaitItem()
            val remaining = assertNotNull(s.budgetRemaining)
            assertEquals("Food", remaining.budgetName)
            assertEquals(10000, remaining.remaining.minorUnits)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveInvalidAmountSetsAmountError() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.SaveRequested)
            var err = awaitItem()
            while (!err.amountError) err = awaitItem()
            assertTrue(err.amountError)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(deps.txRepo.transactions.isEmpty())
    }

    @Test
    fun saveMissingCategorySetsCategoryError() = runTest(testDispatcher) {
        val deps = Deps()
        deps.accountRepo.addAll(listOf(account(1, isDefault = true)))
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedAccountId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.AmountChanged("10.00"))
            awaitItem()
            vm.onIntent(TransactionEditIntent.SaveRequested)
            var err = awaitItem()
            while (!err.categoryError) err = awaitItem()
            assertTrue(err.categoryError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveValidUpsertsAndEmitsSaved() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.onIntent(TransactionEditIntent.AmountChanged("12.50"))
        vm.onIntent(TransactionEditIntent.NoteChanged("Test"))
        vm.effects.test {
            vm.onIntent(TransactionEditIntent.SaveRequested)
            assertIs<TransactionEditEffect.Saved>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val saved = deps.txRepo.transactions.single()
        assertEquals(1250, saved.amount.minorUnits)
        assertEquals("Test", saved.note)
        assertEquals(TransactionType.EXPENSE, saved.type)
    }

    @Test
    fun saveSuggestionDraftStoresExternalIdAndAcceptsSuggestion() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val source = FakeSuggestionSource()
        val vm = vm(
            editingId = null,
            deps = deps,
            draft = suggestionDraft(),
            suggestionSources = mapOf("WALLET" to source),
        )
        vm.state.test {
            var s = awaitItem()
            while (!s.draftApplied) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        vm.effects.test {
            vm.onIntent(TransactionEditIntent.SaveRequested)
            assertIs<TransactionEditEffect.Saved>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val saved = deps.txRepo.transactions.single()
        assertEquals(9876, saved.amount.minorUnits)
        assertEquals(TransactionType.INCOME, saved.type)
        assertTrue(deps.txRepo.existsByExternalId("wallet:notification:1"))
        assertEquals(42L, source.acceptedId)
        assertEquals(saved.id.value, source.acceptedTransactionId)
        assertEquals(clock.now().toEpochMilliseconds(), source.acceptedAt)
    }

    @Test
    fun saveRecurringCreatesRuleAndMaterializesPastTransaction() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.onIntent(TransactionEditIntent.AmountChanged("5.00"))
        vm.onIntent(TransactionEditIntent.RecurringToggled(true))
        vm.onIntent(TransactionEditIntent.FreqUnitChanged(FreqUnit.MONTHS))
        vm.effects.test {
            vm.onIntent(TransactionEditIntent.SaveRequested)
            assertIs<TransactionEditEffect.Saved>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, deps.recurringRepo.rules.size)
        val tx = deps.txRepo.transactions.single()
        assertNotNull(tx.recurringId)
    }

    @Test
    fun deleteRemovesTransactionAndEmitsDeleted() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val id = deps.txRepo.upsert(
            Transaction(
                id = UNSAVED_TRANSACTION_ID,
                type = TransactionType.EXPENSE,
                amount = Money(1000, CurrencyCode("EUR")),
                occurredOn = today,
                note = null,
                categoryId = CategoryId(1),
                accountId = AccountId(1),
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        val vm = vm(id, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.existingId == null) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test {
            vm.onIntent(TransactionEditIntent.DeleteConfirmed)
            assertIs<TransactionEditEffect.Deleted>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(deps.txRepo.transactions.isEmpty())
    }

    @Test
    fun deleteDialogVisibilityIntents() = runTest(testDispatcher) {
        val deps = Deps()
        seedCatsAccounts(deps)
        val vm = vm(null, deps)
        vm.state.test {
            var s = awaitItem()
            while (s.selectedCategoryId == null) s = awaitItem()
            vm.onIntent(TransactionEditIntent.DeleteRequested)
            var shown = awaitItem()
            while (!shown.showDeleteConfirm) shown = awaitItem()
            assertTrue(shown.showDeleteConfirm)
            vm.onIntent(TransactionEditIntent.DeleteCancelled)
            var hidden = awaitItem()
            while (hidden.showDeleteConfirm) hidden = awaitItem()
            assertFalse(hidden.showDeleteConfirm)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun suggestionDraft() = TransactionEditDraft(
        amountMinor = 9876,
        currency = "EUR",
        type = TransactionType.INCOME.name,
        dateIso = "2026-05-05",
        note = "Wallet refund",
        accountId = 2,
        categoryId = 2,
        suggestionSourceName = "Wallet",
        suggestionSourceType = "WALLET",
        suggestionId = 42,
        externalId = "wallet:notification:1",
    )
}
