package com.dv.moneym.feature.walletsync.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.data.walletsync.SuggestionStatus
import com.dv.moneym.data.walletsync.WalletSuggestion
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.time.LocalDate as JavaLocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnrichWalletSuggestionUseCaseTest {

    private val today = LocalDate(2026, 6, 15)
    private val sixMonthsAgo = JavaLocalDate.of(2026, 6, 15).minusMonths(6)

    private fun createAccount(
        id: Long,
        name: String,
        currency: String = "EUR",
        isDefault: Boolean = false,
    ): Account {
        val now = Instant.fromEpochMilliseconds(1_000_000_000_000)
        return Account(
            id = AccountId(id),
            name = name,
            type = AccountType.CASH,
            currency = CurrencyCode(currency),
            isDefault = isDefault,
            archived = false,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun createTransaction(
        id: Long,
        categoryId: Long,
        note: String,
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDate = today,
    ): Transaction {
        val now = Instant.fromEpochMilliseconds(1_000_000_000_000)
        return Transaction(
            id = com.dv.moneym.core.model.TransactionId(id),
            type = type,
            amount = Money(1000, CurrencyCode("EUR")),
            occurredOn = date,
            note = note,
            categoryId = CategoryId(categoryId),
            accountId = AccountId(1),
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun createSuggestion(
        currency: String = "EUR",
        direction: SyncDirection = SyncDirection.DEBIT,
        counterparty: String? = null,
        description: String? = null,
    ): WalletSuggestion {
        return WalletSuggestion(
            id = 0,
            externalId = "test-$currency-$direction",
            amountMinor = 1000,
            currency = currency,
            direction = direction,
            date = today,
            description = description,
            counterparty = counterparty,
            sourcePackage = "com.test.app",
            sourceAppLabel = "Test App",
            status = SuggestionStatus.PENDING,
            capturedAt = System.currentTimeMillis(),
        )
    }

    @Test
    fun walletSuggestion_defaultWalletMatchingCurrency_isSelected() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(
                createAccount(1, "Default EUR", "EUR", isDefault = true),
                createAccount(2, "Other USD", "USD"),
            ))
        }
        val txnRepo = FakeTransactionRepository()
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(currency = "EUR")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(1L, result.suggestedAccountId)
    }

    @Test
    fun walletSuggestion_firstMatchingCurrencyWhenNoDefault() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(
                createAccount(1, "Default USD", "USD", isDefault = true),
                createAccount(2, "EUR Wallet EUR Wallet", "EUR"),
                createAccount(3, "Another EUR", "EUR"),
            ))
        }
        val txnRepo = FakeTransactionRepository()
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(currency = "EUR")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(2L, result.suggestedAccountId)
    }

    @Test
    fun walletSuggestion_noMatchWhenNoWalletWithCurrency() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(
                createAccount(1, "Default USD", "USD", isDefault = true),
                createAccount(2, "GBP Wallet", "GBP"),
            ))
        }
        val txnRepo = FakeTransactionRepository()
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(currency = "EUR")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertNull(result.suggestedAccountId)
    }

    @Test
    fun walletSuggestion_caseInsensitiveCurrencyMatch() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(
                createAccount(1, "eur wallet", "eur"),
            ))
        }
        val txnRepo = FakeTransactionRepository()
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(currency = "EUR")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(1L, result.suggestedAccountId)
    }

    @Test
    fun categorySuggestion_matchesByNoteSubstring() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Coffee at Starbucks"),
                createTransaction(2, 10, "Lunch at Starbucks"),
                createTransaction(3, 20, "Groceries at Walmart"),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = "Starbucks")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_matchesByDescriptionWhenNoCounterparty() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Payment to Amazon"),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(description = "Amazon")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_respectsDirectionDebitExpense() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Starbucks", TransactionType.EXPENSE),
                createTransaction(2, 20, "Starbucks", TransactionType.INCOME),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(direction = SyncDirection.DEBIT, counterparty = "Starbucks")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_respectsDirectionCreditIncome() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Salary", TransactionType.EXPENSE),
                createTransaction(2, 20, "Salary", TransactionType.INCOME),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(direction = SyncDirection.CREDIT, counterparty = "Salary")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(20L, result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_returnsMostFrequentCategory() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Starbucks coffee"),
                createTransaction(2, 10, "Starbucks latte"),
                createTransaction(3, 20, "Starbucks muffin"),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = "Starbucks")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_returnsNullWhenNoMatchingNote() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Coffee at Starbucks"),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = "Walmart")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertNull(result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_returnsNullWhenEmptyCounterpartyAndDescription() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Some note"),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = null, description = null)

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertNull(result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_returnsNullWhenNoTransactions() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository()
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = "Starbucks")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertNull(result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_caseInsensitiveMatch() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "COFFEE AT STARBUCKS"),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = "starbucks")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun categorySuggestion_filtersBySixMonthWindow() {
        val accountRepo = FakeAccountRepository().apply {
            addAll(listOf(createAccount(1, "Default", "EUR", isDefault = true)))
        }
        val txnRepo = FakeTransactionRepository().apply {
            addAll(listOf(
                createTransaction(1, 10, "Starbucks", TransactionType.EXPENSE, today),
                createTransaction(2, 20, "Starbucks", TransactionType.EXPENSE, LocalDate(2025, 11, 15)),
            ))
        }
        val useCase = EnrichWalletSuggestionUseCase()
        val suggestion = createSuggestion(counterparty = "Starbucks")

        val result = useCase(suggestion, accountRepo.accounts, txnRepo.transactions)

        assertEquals(10L, result.suggestedCategoryId)
    }
}