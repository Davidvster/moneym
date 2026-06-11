package com.dv.moneym.data.banksync.internal

import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.SuggestionStatus
import com.dv.moneym.data.banksync.db.BankSyncRoomDatabase
import kotlinx.coroutines.flow.Flow
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

    override fun observePendingSuggestions(): Flow<List<BankSuggestion>> =
        suggestionDao.observeByStatus(SuggestionStatus.PENDING.name).map { rows -> rows.map { it.toDomain() } }

    override fun observeRejectedSuggestions(): Flow<List<BankSuggestion>> =
        suggestionDao.observeByStatus(SuggestionStatus.REJECTED.name).map { rows -> rows.map { it.toDomain() } }

    override fun observePendingCount(): Flow<Int> = suggestionDao.observePendingCount()

    override suspend fun getSuggestion(id: Long): BankSuggestion? =
        suggestionDao.selectById(id)?.toDomain()

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

    override suspend fun clearAll() {
        suggestionDao.deleteAll()
        accountDao.deleteAll()
    }
}
