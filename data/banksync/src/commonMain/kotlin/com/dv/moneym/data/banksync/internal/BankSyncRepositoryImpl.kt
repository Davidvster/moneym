package com.dv.moneym.data.banksync.internal

import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.SuggestionStatus
import com.dv.moneym.data.banksync.db.BankSyncRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class BankSyncRepositoryImpl(
    private val db: BankSyncRoomDatabase,
) : BankSyncRepository {

    private val accountDao get() = db.bankAccountDao()
    private val suggestionDao get() = db.bankSuggestionDao()

    override fun observeAccounts(): Flow<List<BankAccountLink>> =
        accountDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEnabledAccounts(): List<BankAccountLink> =
        accountDao.selectEnabled().map { it.toDomain() }

    override suspend fun upsertAccounts(accounts: List<BankAccountLink>) {
        accountDao.insertAll(accounts.map { it.toEntity() })
        for (account in accounts) {
            val existing = accountDao.selectByUid(account.uid) ?: continue
            accountDao.update(
                existing.copy(
                    bankName = account.bankName,
                    displayName = account.displayName,
                    iban = account.iban,
                    currency = account.currency,
                )
            )
        }
    }

    override suspend fun setLocalAccountMapping(uid: String, localAccountId: Long?) =
        accountDao.setLocalAccountMapping(uid, localAccountId)

    override suspend fun setAccountEnabled(uid: String, enabled: Boolean) =
        accountDao.setEnabled(uid, enabled)

    override suspend fun advanceCursor(uid: String, syncedThrough: LocalDate, syncedAt: Long) =
        accountDao.setCursor(uid, syncedThrough.toString(), syncedAt)

    override fun observePending(): Flow<List<SuggestionRecord>> =
        combine(
            suggestionDao.observeByStatus(SuggestionStatus.PENDING.name),
            accountDao.observeAll(),
        ) { rows, accounts ->
            val links = accounts.map { it.toDomain() }
            rows.map { it.toDomain().toRecord(links) }
        }

    override fun observeRejected(): Flow<List<SuggestionRecord>> =
        combine(
            suggestionDao.observeByStatus(SuggestionStatus.REJECTED.name),
            accountDao.observeAll(),
        ) { rows, accounts ->
            val links = accounts.map { it.toDomain() }
            rows.map { it.toDomain().toRecord(links) }
        }

    override fun observePendingCount(): Flow<Int> = suggestionDao.observePendingCount()

    override suspend fun getRecord(id: Long): SuggestionRecord? {
        val suggestion = suggestionDao.selectById(id)?.toDomain() ?: return null
        val link = accountDao.selectByUid(suggestion.bankAccountUid)?.toDomain()
        return suggestion.toRecord(listOfNotNull(link))
    }

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

    override suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String> =
        externalIds.chunked(500)
            .flatMap { suggestionDao.selectExistingExternalIds(it) }
            .toSet()

    override suspend fun insertSuggestionsIfNew(suggestions: List<BankSuggestion>): Int {
        if (suggestions.isEmpty()) return 0
        val known = filterKnownExternalIds(suggestions.map { it.externalId })
        val fresh = suggestions.filter { it.externalId !in known }
        suggestionDao.insertAll(fresh.map { it.toEntity().copy(id = 0) })
        return fresh.size
    }

    override suspend fun accept(id: Long, createdTransactionId: Long, decidedAt: Long) =
        suggestionDao.setStatus(id, SuggestionStatus.ACCEPTED.name, createdTransactionId, decidedAt)

    override suspend fun reject(id: Long, decidedAt: Long) =
        suggestionDao.setStatus(id, SuggestionStatus.REJECTED.name, null, decidedAt)

    override suspend fun restoreToPending(id: Long) =
        suggestionDao.setStatus(id, SuggestionStatus.PENDING.name, null, null)

    override suspend fun deleteRejected(ids: Set<Long>) {
        if (ids.isNotEmpty()) suggestionDao.deleteRejected(ids)
    }

    override suspend fun clearAll() {
        suggestionDao.deleteAll()
        accountDao.deleteAll()
    }
}
