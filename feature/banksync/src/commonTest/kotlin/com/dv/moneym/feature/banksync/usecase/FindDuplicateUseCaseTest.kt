package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.EbDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class FindDuplicateUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val useCase = FindDuplicateUseCase(transactionRepository = txRepo)

    private fun suggestion(amountMinor: Long) = BankSuggestion(
        id = 0,
        externalId = "eb:acc-1:r1",
        bankAccountUid = "acc-1",
        amountMinor = amountMinor,
        currency = "EUR",
        direction = EbDirection.DEBIT,
        bookingDate = LocalDate(2026, 6, 8),
        description = "COFFEE",
        counterparty = "Coffee Shop",
        fetchedAt = 0,
    )

    private suspend fun seedTransaction(amountMinor: Long, date: LocalDate, currency: String) {
        txRepo.upsert(
            Transaction(
                id = TransactionId(0),
                type = TransactionType.EXPENSE,
                amount = Money(amountMinor, CurrencyCode(currency)),
                occurredOn = date,
                note = null,
                categoryId = CategoryId(1),
                accountId = AccountId(1),
                createdAt = Instant.parse("2026-06-08T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-08T00:00:00Z"),
            )
        )
    }

    @Test
    fun matchesSameDateAmountCurrency() = runTest {
        seedTransaction(1250, LocalDate(2026, 6, 8), "EUR")
        assertEquals(1250, useCase(suggestion(-1250))?.amount?.minorUnits)
    }

    @Test
    fun noMatchWhenAmountDiffers() = runTest {
        seedTransaction(1250, LocalDate(2026, 6, 8), "EUR")
        assertNull(useCase(suggestion(-999)))
    }

    @Test
    fun noMatchWhenCurrencyDiffers() = runTest {
        seedTransaction(1250, LocalDate(2026, 6, 8), "USD")
        assertNull(useCase(suggestion(-1250)))
    }
}
