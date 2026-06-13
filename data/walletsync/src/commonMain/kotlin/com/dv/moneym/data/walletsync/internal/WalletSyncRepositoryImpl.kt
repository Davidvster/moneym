package com.dv.moneym.data.walletsync.internal

import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.data.walletsync.SuggestionStatus
import com.dv.moneym.data.walletsync.WalletSuggestion
import com.dv.moneym.data.walletsync.WalletSyncRepository
import com.dv.moneym.data.walletsync.db.WalletSuggestionEntity
import com.dv.moneym.data.walletsync.db.WalletSyncRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class WalletSyncRepositoryImpl(
    private val db: WalletSyncRoomDatabase,
) : WalletSyncRepository {

    private val dao get() = db.walletSuggestionDao()

    override fun observePending(): Flow<List<SuggestionRecord>> =
        dao.observeByStatus(SuggestionStatus.PENDING.name).map { rows -> rows.map { it.toRecord() } }

    override fun observeRejected(): Flow<List<SuggestionRecord>> =
        dao.observeByStatus(SuggestionStatus.REJECTED.name).map { rows -> rows.map { it.toRecord() } }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun getRecord(id: Long): SuggestionRecord? =
        dao.selectById(id)?.toRecord()

    override suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String> =
        externalIds.chunked(500)
            .flatMap { dao.selectExistingExternalIds(it) }
            .toSet()

    override suspend fun insertSuggestionsIfNew(suggestions: List<WalletSuggestion>): Int {
        if (suggestions.isEmpty()) return 0
        val known = filterKnownExternalIds(suggestions.map { it.externalId })
        val fresh = suggestions.filter { it.externalId !in known }
        dao.insertAll(fresh.map { it.toEntity() })
        return fresh.size
    }

    override suspend fun accept(id: Long, createdTransactionId: Long, decidedAt: Long) =
        dao.setStatus(id, SuggestionStatus.ACCEPTED.name, createdTransactionId, decidedAt)

    override suspend fun reject(id: Long, decidedAt: Long) =
        dao.setStatus(id, SuggestionStatus.REJECTED.name, null, decidedAt)

    override suspend fun restoreToPending(id: Long) =
        dao.setStatus(id, SuggestionStatus.PENDING.name, null, null)

    override suspend fun clearAll() = dao.deleteAll()

    private fun WalletSuggestionEntity.toRecord(): SuggestionRecord =
        SuggestionRecord(
            id = id,
            externalId = externalId,
            amountMinor = amountMinor,
            currency = currency,
            direction = SyncDirection.valueOf(direction),
            date = LocalDate.parse(date),
            description = description,
            counterparty = counterparty,
            sourceLabel = sourceAppLabel ?: sourcePackage,
            suggestedAccountId = null,
        )

    private fun WalletSuggestion.toEntity(): WalletSuggestionEntity =
        WalletSuggestionEntity(
            id = 0,
            externalId = externalId,
            amountMinor = amountMinor,
            currency = currency,
            direction = direction.name,
            date = date.toString(),
            description = description,
            counterparty = counterparty,
            sourcePackage = sourcePackage,
            sourceAppLabel = sourceAppLabel,
            status = status.name,
            createdTransactionId = createdTransactionId,
            capturedAt = capturedAt,
            decidedAt = decidedAt,
        )
}
