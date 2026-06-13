package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.SuggestionSource
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.transactions.TransactionRepository
import kotlin.math.abs

class AcceptSuggestionUseCase(
    private val transactionRepository: TransactionRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        source: SuggestionSource,
        suggestion: SuggestionRecord,
        accountId: Long,
        categoryId: Long,
    ): TransactionId {
        val now = clock.now()
        val transactionId = transactionRepository.upsert(
            Transaction(
                id = UNSAVED_TRANSACTION_ID,
                type = when (suggestion.direction) {
                    SyncDirection.CREDIT -> TransactionType.INCOME
                    SyncDirection.DEBIT -> TransactionType.EXPENSE
                },
                amount = Money(abs(suggestion.amountMinor), CurrencyCode(suggestion.currency)),
                occurredOn = suggestion.date,
                note = suggestion.description ?: suggestion.counterparty,
                categoryId = CategoryId(categoryId),
                accountId = AccountId(accountId),
                createdAt = now,
                updatedAt = now,
            )
        )
        transactionRepository.setExternalId(transactionId, suggestion.externalId)
        source.accept(suggestion.id, transactionId.value, now.toEpochMilliseconds())
        return transactionId
    }
}
