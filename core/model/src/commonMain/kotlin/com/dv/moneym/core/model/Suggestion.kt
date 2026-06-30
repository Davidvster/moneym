package com.dv.moneym.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

enum class SyncDirection { DEBIT, CREDIT }

/**
 * Source-agnostic view of a pending/rejected transaction suggestion, shared by the bank-sync and
 * wallet-notification-sync features so they can drive the same review screen.
 *
 * [suggestedAccountId] is a pre-filled target wallet (bank sync resolves it from the account-link
 * mapping; notification sync leaves it null so the user picks one). [sourceLabel] is a short origin
 * tag shown on the card (bank name, or payment-app label).
 */
data class SuggestionRecord(
    val id: Long,
    val externalId: String,
    val amountMinor: Long,
    val currency: String,
    val direction: SyncDirection,
    val date: LocalDate,
    val description: String?,
    val counterparty: String?,
    val sourceLabel: String?,
    val suggestedAccountId: Long?,
    val suggestedCategoryId: Long? = null,
)

/**
 * The review screen's only data dependency. Both `BankSyncRepository` and `WalletSyncRepository`
 * implement it, letting one ViewModel/screen serve either source.
 */
interface SuggestionSource {
    fun observePending(): Flow<List<SuggestionRecord>>
    fun observeRejected(): Flow<List<SuggestionRecord>>
    fun observePendingCount(): Flow<Int>
    suspend fun getRecord(id: Long): SuggestionRecord?
    suspend fun accept(id: Long, createdTransactionId: Long, decidedAt: Long)
    suspend fun reject(id: Long, decidedAt: Long)
    suspend fun restoreToPending(id: Long)
    suspend fun deleteRejected(ids: Set<Long>)
}
