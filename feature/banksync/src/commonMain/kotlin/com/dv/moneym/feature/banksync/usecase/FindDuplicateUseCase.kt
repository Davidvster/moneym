package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.SuggestionRecord
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.data.transactions.TransactionRepository
import kotlin.math.abs

class FindDuplicateUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(suggestion: SuggestionRecord): Transaction? =
        transactionRepository.findByDateAndAmount(
            date = suggestion.date,
            amountMinor = abs(suggestion.amountMinor),
            currency = CurrencyCode(suggestion.currency),
        ).firstOrNull()
}
