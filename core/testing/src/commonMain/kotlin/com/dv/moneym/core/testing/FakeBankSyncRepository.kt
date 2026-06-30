package com.dv.moneym.core.testing

import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.SuggestionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeBankSyncRepository : BankSyncRepository {
    private val _accounts = MutableStateFlow<List<BankAccountLink>>(emptyList())
    private val _suggestions = MutableStateFlow<List<BankSuggestion>>(emptyList())
    private var nextId = 1L

    val accounts: List<BankAccountLink> get() = _accounts.value
    val suggestions: List<BankSuggestion> get() = _suggestions.value

    override fun observeAccounts(): Flow<List<BankAccountLink>> = _accounts

    override suspend fun getEnabledAccounts(): List<BankAccountLink> =
        _accounts.value.filter { it.enabled }

    override suspend fun upsertAccounts(accounts: List<BankAccountLink>) {
        _accounts.update { existing ->
            val byUid = existing.associateBy { it.uid }.toMutableMap()
            for (account in accounts) {
                val prior = byUid[account.uid]
                byUid[account.uid] = if (prior == null) account else prior.copy(
                    bankName = account.bankName,
                    displayName = account.displayName,
                    iban = account.iban,
                    currency = account.currency,
                )
            }
            byUid.values.toList()
        }
    }

    override suspend fun setLocalAccountMapping(uid: String, localAccountId: Long?) {
        _accounts.update { list ->
            list.map { if (it.uid == uid) it.copy(localAccountId = localAccountId) else it }
        }
    }

    override suspend fun setAccountEnabled(uid: String, enabled: Boolean) {
        _accounts.update { list -> list.map { if (it.uid == uid) it.copy(enabled = enabled) else it } }
    }

    override suspend fun advanceCursor(uid: String, syncedThrough: LocalDate, syncedAt: Long) {
        _accounts.update { list ->
            list.map {
                if (it.uid == uid) it.copy(lastSyncedDate = syncedThrough, lastSyncedAt = syncedAt) else it
            }
        }
    }

    override fun observePending(): Flow<List<SuggestionRecord>> =
        combine(_suggestions, _accounts) { list, accounts ->
            list.filter { it.status == SuggestionStatus.PENDING }.map { it.toRecord(accounts) }
        }

    override fun observeRejected(): Flow<List<SuggestionRecord>> =
        combine(_suggestions, _accounts) { list, accounts ->
            list.filter { it.status == SuggestionStatus.REJECTED }.map { it.toRecord(accounts) }
        }

    override fun observePendingCount(): Flow<Int> =
        _suggestions.map { list -> list.count { it.status == SuggestionStatus.PENDING } }

    override suspend fun getRecord(id: Long): SuggestionRecord? =
        _suggestions.value.firstOrNull { it.id == id }?.toRecord(_accounts.value)

    private fun BankSuggestion.toRecord(links: List<BankAccountLink>): SuggestionRecord {
        val link = links.firstOrNull { it.uid == bankAccountUid }
        return SuggestionRecord(
            id = id,
            externalId = externalId,
            amountMinor = amountMinor,
            currency = currency,
            direction = when (direction) {
                EbDirection.DEBIT -> SyncDirection.DEBIT
                EbDirection.CREDIT -> SyncDirection.CREDIT
            },
            date = bookingDate,
            description = description,
            counterparty = counterparty,
            sourceLabel = link?.bankName,
            suggestedAccountId = link?.localAccountId,
        )
    }

    override suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String> {
        val known = _suggestions.value.mapTo(mutableSetOf()) { it.externalId }
        return externalIds.filterTo(mutableSetOf()) { it in known }
    }

    override suspend fun insertSuggestionsIfNew(suggestions: List<BankSuggestion>): Int {
        val known = filterKnownExternalIds(suggestions.map { it.externalId })
        val fresh = suggestions.filter { it.externalId !in known }
        _suggestions.update { it + fresh.map { s -> s.copy(id = nextId++) } }
        return fresh.size
    }

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
        _suggestions.update { list ->
            list.filterNot { it.id in ids && it.status == SuggestionStatus.REJECTED }
        }
    }

    override suspend fun clearAll() {
        _accounts.value = emptyList()
        _suggestions.value = emptyList()
    }

    private fun setStatus(id: Long, status: SuggestionStatus, transactionId: Long?, decidedAt: Long?) {
        _suggestions.update { list ->
            list.map {
                if (it.id == id) it.copy(status = status, createdTransactionId = transactionId, decidedAt = decidedAt)
                else it
            }
        }
    }
}
