package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.transactions.TransactionRepository
import kotlin.math.abs

class AcceptSuggestionUseCase(
    private val transactionRepository: TransactionRepository,
    private val bankSyncRepository: BankSyncRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        suggestion: BankSuggestion,
        accountId: Long,
        categoryId: Long,
    ): TransactionId {
        val now = clock.now()
        val transactionId = transactionRepository.upsert(
            Transaction(
                id = UNSAVED_TRANSACTION_ID,
                type = when (suggestion.direction) {
                    EbDirection.CREDIT -> TransactionType.INCOME
                    EbDirection.DEBIT -> TransactionType.EXPENSE
                },
                amount = Money(abs(suggestion.amountMinor), CurrencyCode(suggestion.currency)),
                occurredOn = suggestion.bookingDate,
                note = suggestion.description ?: suggestion.counterparty,
                categoryId = CategoryId(categoryId),
                accountId = AccountId(accountId),
                createdAt = now,
                updatedAt = now,
            )
        )
        transactionRepository.setExternalId(transactionId, suggestion.externalId)
        bankSyncRepository.accept(suggestion.id, transactionId.value, now.toEpochMilliseconds())
        return transactionId
    }
}
