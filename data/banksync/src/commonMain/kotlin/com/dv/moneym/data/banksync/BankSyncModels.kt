package com.dv.moneym.data.banksync

import kotlinx.datetime.LocalDate

data class BankAccountLink(
    val uid: String,
    val bankName: String,
    val displayName: String? = null,
    val iban: String? = null,
    val currency: String,
    val localAccountId: Long? = null,
    val enabled: Boolean = true,
    val lastSyncedDate: LocalDate? = null,
    val lastSyncedAt: Long? = null,
)

enum class SuggestionStatus { PENDING, ACCEPTED, REJECTED }

data class BankSuggestion(
    val id: Long,
    val externalId: String,
    val bankAccountUid: String,
    val amountMinor: Long,
    val currency: String,
    val direction: EbDirection,
    val bookingDate: LocalDate,
    val valueDate: LocalDate? = null,
    val description: String? = null,
    val counterparty: String? = null,
    val status: SuggestionStatus = SuggestionStatus.PENDING,
    val createdTransactionId: Long? = null,
    val fetchedAt: Long,
    val decidedAt: Long? = null,
)
