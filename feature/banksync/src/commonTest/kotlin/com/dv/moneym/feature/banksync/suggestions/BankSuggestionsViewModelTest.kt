package com.dv.moneym.feature.banksync.suggestions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.SuggestionStatus
import com.dv.moneym.feature.banksync.usecase.AcceptSuggestionUseCase
import com.dv.moneym.feature.banksync.usecase.FindDuplicateUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BankSuggestionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val bankRepo = FakeBankSyncRepository()
    private val txRepo = FakeTransactionRepository()
    private val categoryRepo = FakeCategoryRepository()
    private val clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z"))

    private fun vm() = BankSuggestionsViewModel(
        bankSyncRepository = bankRepo,
        categoryRepository = categoryRepo,
        acceptSuggestion = AcceptSuggestionUseCase(txRepo, bankRepo, clock),
        findDuplicate = FindDuplicateUseCase(txRepo),
        clock = clock,
        savedStateHandle = SavedStateHandle(),
    )

    private suspend fun seed(): BankSuggestion {
        categoryRepo.insert(category(1, "Food", TransactionType.EXPENSE))
        bankRepo.upsertAccounts(
            listOf(BankAccountLink(uid = "acc-1", bankName = "Tatra", currency = "EUR", localAccountId = 7))
        )
        bankRepo.insertSuggestionsIfNew(
            listOf(
                BankSuggestion(
                    id = 0,
                    externalId = "eb:acc-1:r1",
                    bankAccountUid = "acc-1",
                    amountMinor = 1250,
                    currency = "EUR",
                    direction = EbDirection.DEBIT,
                    bookingDate = LocalDate(2026, 6, 8),
                    description = "COFFEE",
                    fetchedAt = 0,
                )
            )
        )
        return bankRepo.suggestions.single()
    }

    private fun category(id: Long, name: String, type: TransactionType) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = "tag",
        colorHex = "#FFFFFF",
        isUserCreated = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        type = type,
    )

    @Test
    fun pendingRowGetsDefaultCategoryAndAccount() = runTest(testDispatcher) {
        seed()
        val vm = vm()
        vm.state.test {
            var s = awaitItem()
            while (s.pending.isEmpty()) s = awaitItem()
            val row = s.pending.single()
            assertEquals(7L, row.targetAccountId)
            assertEquals(1L, row.categoryId)
            assertEquals("Food", row.categoryName)
            assertNull(row.duplicate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun duplicateByDateAndAmountIsSurfaced() = runTest(testDispatcher) {
        seed()
        txRepo.upsert(
            Transaction(
                id = UNSAVED_TRANSACTION_ID,
                type = TransactionType.EXPENSE,
                amount = Money(1250, CurrencyCode("EUR")),
                occurredOn = LocalDate(2026, 6, 8),
                note = "coffee at shop",
                categoryId = CategoryId(1),
                accountId = AccountId(7),
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
            )
        )
        val vm = vm()
        vm.state.test {
            var s = awaitItem()
            while (s.pending.isEmpty() || s.pending.single().duplicate == null) s = awaitItem()
            val duplicate = s.pending.single().duplicate
            assertNotNull(duplicate)
            assertEquals("coffee at shop", duplicate.note)
            assertEquals(1250L, duplicate.amountMinor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun acceptCreatesTransactionAndMovesOutOfPending() = runTest(testDispatcher) {
        val suggestion = seed()
        val vm = vm()
        vm.state.test {
            var s = awaitItem()
            while (s.pending.isEmpty()) s = awaitItem()

            vm.onIntent(BankSuggestionsIntent.Accept(suggestion.id))
            while (s.pending.isNotEmpty()) s = awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, txRepo.transactions.size)
        assertTrue(txRepo.existsByExternalId("eb:acc-1:r1"))
        assertEquals(SuggestionStatus.ACCEPTED, bankRepo.suggestions.single().status)
    }

    @Test
    fun rejectMovesToRejectedTabAndRestoreBrigsBack() = runTest(testDispatcher) {
        val suggestion = seed()
        val vm = vm()
        vm.state.test {
            var s = awaitItem()
            while (s.pending.isEmpty()) s = awaitItem()

            vm.onIntent(BankSuggestionsIntent.Reject(suggestion.id))
            while (s.rejected.isEmpty()) s = awaitItem()
            assertTrue(s.pending.isEmpty())

            vm.onIntent(BankSuggestionsIntent.RestoreToPending(suggestion.id))
            while (s.pending.isEmpty()) s = awaitItem()
            assertTrue(s.rejected.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, txRepo.transactions.size)
    }

    @Test
    fun batchAcceptHandlesSelection() = runTest(testDispatcher) {
        seed()
        bankRepo.insertSuggestionsIfNew(
            listOf(
                BankSuggestion(
                    id = 0,
                    externalId = "eb:acc-1:r2",
                    bankAccountUid = "acc-1",
                    amountMinor = 900,
                    currency = "EUR",
                    direction = EbDirection.DEBIT,
                    bookingDate = LocalDate(2026, 6, 9),
                    description = "LUNCH",
                    fetchedAt = 0,
                )
            )
        )
        val vm = vm()
        vm.state.test {
            var s = awaitItem()
            while (s.pending.size < 2) s = awaitItem()

            vm.onIntent(BankSuggestionsIntent.SelectAll)
            while (s.selectedIds.size < 2) s = awaitItem()

            vm.onIntent(BankSuggestionsIntent.AcceptSelected)
            while (s.pending.isNotEmpty()) s = awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, txRepo.transactions.size)
    }
}
