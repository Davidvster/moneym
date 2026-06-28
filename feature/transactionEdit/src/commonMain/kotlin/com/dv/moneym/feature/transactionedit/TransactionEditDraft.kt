package com.dv.moneym.feature.transactionedit

import kotlinx.serialization.Serializable

@Serializable
data class TransactionEditDraft(
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val dateIso: String,
    val note: String?,
    val accountId: Long?,
    val categoryId: Long?,
    val suggestionSourceName: String?,
    val suggestionSourceType: String,
    val suggestionId: Long,
    val externalId: String,
)
