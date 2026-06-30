package com.dv.moneym.data.banksync

import com.dv.moneym.core.testing.FakeBankSyncRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class BankSyncRepositoryDeleteRejectedTest {

    @Test
    fun deleteRejectedRemovesOnlyRejectedSuggestions() = runTest {
        val repository = FakeBankSyncRepository()
        repository.insertSuggestionsIfNew(
            listOf(
                suggestion("pending"),
                suggestion("rejected"),
            )
        )
        val pendingId = repository.suggestions.first { it.externalId.endsWith("pending") }.id
        val rejectedId = repository.suggestions.first { it.externalId.endsWith("rejected") }.id
        repository.reject(rejectedId, decidedAt = 1)

        repository.deleteRejected(setOf(pendingId, rejectedId))

        assertEquals(listOf(pendingId), repository.suggestions.map { it.id })
        assertEquals(SuggestionStatus.PENDING, repository.suggestions.single().status)
    }

    private fun suggestion(id: String) = BankSuggestion(
        id = 0,
        externalId = "eb:acc-1:$id",
        bankAccountUid = "acc-1",
        amountMinor = 1250,
        currency = "EUR",
        direction = EbDirection.DEBIT,
        bookingDate = LocalDate(2026, 6, 8),
        fetchedAt = 0,
    )
}
