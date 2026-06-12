package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.SuggestionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class AcceptSuggestionUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val bankRepo = FakeBankSyncRepository()
    private val accept = AcceptSuggestionUseCase(
        transactionRepository = txRepo,
        bankSyncRepository = bankRepo,
        clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z")),
    )

    private suspend fun pendingSuggestion(direction: EbDirection = EbDirection.DEBIT): BankSuggestion {
        bankRepo.insertSuggestionsIfNew(
            listOf(
                BankSuggestion(
                    id = 0,
                    externalId = "eb:acc-1:r1",
                    bankAccountUid = "acc-1",
                    amountMinor = 1250,
                    currency = "EUR",
                    direction = direction,
                    bookingDate = LocalDate(2026, 6, 8),
                    description = "COFFEE",
                    counterparty = "Coffee Shop",
                    fetchedAt = 0,
                )
            )
        )
        return bankRepo.suggestions.single()
    }

    @Test
    fun acceptCreatesLinkedTransactionAndMarksSuggestion() = runTest {
        val suggestion = pendingSuggestion()

        val txId = accept(suggestion, accountId = 7, categoryId = 3)

        val tx = txRepo.transactions.single()
        assertEquals(txId, tx.id)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Money(1250, CurrencyCode("EUR")), tx.amount)
        assertEquals(LocalDate(2026, 6, 8), tx.occurredOn)
        assertEquals("COFFEE", tx.note)
        assertEquals(AccountId(7), tx.accountId)
        assertEquals(CategoryId(3), tx.categoryId)
        assertTrue(txRepo.existsByExternalId("eb:acc-1:r1"))

        val decided = bankRepo.suggestions.single()
        assertEquals(SuggestionStatus.ACCEPTED, decided.status)
        assertEquals(txId.value, decided.createdTransactionId)
    }

    @Test
    fun creditDirectionBecomesIncome() = runTest {
        val suggestion = pendingSuggestion(direction = EbDirection.CREDIT)

        accept(suggestion, accountId = 1, categoryId = 1)

        assertEquals(TransactionType.INCOME, txRepo.transactions.single().type)
    }
}
