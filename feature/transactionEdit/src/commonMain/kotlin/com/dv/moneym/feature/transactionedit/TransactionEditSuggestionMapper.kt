package com.dv.moneym.feature.transactionedit

import com.dv.moneym.core.model.TransactionType

fun transactionEditDraftFromSuggestion(
    amountMinor: Long,
    currency: String,
    isExpense: Boolean,
    dateIso: String,
    description: String?,
    counterparty: String?,
    targetAccountId: Long?,
    categoryId: Long?,
    sourceLabel: String,
    suggestionSourceType: String,
    suggestionId: Long,
    externalId: String,
) = TransactionEditDraft(
    amountMinor = amountMinor,
    currency = currency,
    type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
    dateIso = dateIso,
    note = description ?: counterparty,
    accountId = targetAccountId,
    categoryId = categoryId,
    suggestionSourceName = sourceLabel.takeIf { it.isNotBlank() },
    suggestionSourceType = suggestionSourceType,
    suggestionId = suggestionId,
    externalId = externalId,
)
