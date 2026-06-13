package com.dv.moneym.data.walletsync

import com.dv.moneym.core.model.SyncDirection
import kotlinx.datetime.LocalDate

enum class SuggestionStatus { PENDING, ACCEPTED, REJECTED }

/**
 * A transaction parsed from a payment-app notification, awaiting the user's review.
 * Mirrors `BankSuggestion` but carries the originating app instead of a bank-account link.
 */
data class WalletSuggestion(
    val id: Long,
    val externalId: String,
    val amountMinor: Long,
    val currency: String,
    val direction: SyncDirection,
    val date: LocalDate,
    val description: String? = null,
    val counterparty: String? = null,
    val sourcePackage: String,
    val sourceAppLabel: String? = null,
    val status: SuggestionStatus = SuggestionStatus.PENDING,
    val createdTransactionId: Long? = null,
    val capturedAt: Long,
    val decidedAt: Long? = null,
)
