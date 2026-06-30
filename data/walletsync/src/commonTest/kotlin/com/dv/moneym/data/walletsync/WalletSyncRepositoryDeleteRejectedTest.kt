package com.dv.moneym.data.walletsync

import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SyncDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class WalletSyncRepositoryDeleteRejectedTest {

    @Test
    fun deleteRejectedRemovesOnlyRejectedSuggestions() = runTest {
        val repository = InMemoryWalletSyncRepository()
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

    private fun suggestion(id: String) = WalletSuggestion(
        id = 0,
        externalId = "wallet:$id",
        amountMinor = 1250,
        currency = "EUR",
        direction = SyncDirection.DEBIT,
        date = LocalDate(2026, 6, 8),
        sourcePackage = "com.example.wallet",
        capturedAt = 0,
    )

    private class InMemoryWalletSyncRepository : WalletSyncRepository {
        private val rows = MutableStateFlow<List<WalletSuggestion>>(emptyList())
        private var nextId = 1L

        val suggestions: List<WalletSuggestion> get() = rows.value

        override fun observePending(): Flow<List<SuggestionRecord>> =
            rows.map { list -> list.filter { it.status == SuggestionStatus.PENDING }.map { it.toRecord() } }

        override fun observeRejected(): Flow<List<SuggestionRecord>> =
            rows.map { list -> list.filter { it.status == SuggestionStatus.REJECTED }.map { it.toRecord() } }

        override fun observePendingCount(): Flow<Int> =
            rows.map { list -> list.count { it.status == SuggestionStatus.PENDING } }

        override suspend fun getRecord(id: Long): SuggestionRecord? =
            rows.value.firstOrNull { it.id == id }?.toRecord()

        override suspend fun accept(id: Long, createdTransactionId: Long, decidedAt: Long) {
            setStatus(id, SuggestionStatus.ACCEPTED, createdTransactionId, decidedAt)
        }

        override suspend fun reject(id: Long, decidedAt: Long) {
            setStatus(id, SuggestionStatus.REJECTED, null, decidedAt)
        }

        override suspend fun restoreToPending(id: Long) {
            setStatus(id, SuggestionStatus.PENDING, null, null)
        }

        override suspend fun deleteRejected(ids: Set<Long>) {
            rows.update { list ->
                list.filterNot { it.id in ids && it.status == SuggestionStatus.REJECTED }
            }
        }

        override suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String> {
            val known = rows.value.mapTo(mutableSetOf()) { it.externalId }
            return externalIds.filterTo(mutableSetOf()) { it in known }
        }

        override suspend fun insertSuggestionsIfNew(suggestions: List<WalletSuggestion>): Int {
            val known = filterKnownExternalIds(suggestions.map { it.externalId })
            val fresh = suggestions.filter { it.externalId !in known }
            rows.update { list -> list + fresh.map { it.copy(id = nextId++) } }
            return fresh.size
        }

        override suspend fun clearAll() {
            rows.value = emptyList()
        }

        private fun setStatus(
            id: Long,
            status: SuggestionStatus,
            transactionId: Long?,
            decidedAt: Long?,
        ) {
            rows.update { list ->
                list.map {
                    if (it.id == id) {
                        it.copy(status = status, createdTransactionId = transactionId, decidedAt = decidedAt)
                    } else {
                        it
                    }
                }
            }
        }

        private fun WalletSuggestion.toRecord() = SuggestionRecord(
            id = id,
            externalId = externalId,
            amountMinor = amountMinor,
            currency = currency,
            direction = direction,
            date = date,
            description = description,
            counterparty = counterparty,
            sourceLabel = sourceAppLabel ?: sourcePackage,
            suggestedAccountId = suggestedAccountId,
            suggestedCategoryId = suggestedCategoryId,
        )
    }
}
