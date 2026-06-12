package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.transactions.TransactionRepository
import kotlin.math.abs

class FindDuplicateUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(suggestion: BankSuggestion): Transaction? =
        transactionRepository.findByDateAndAmount(
            date = suggestion.bookingDate,
            amountMinor = abs(suggestion.amountMinor),
            currency = CurrencyCode(suggestion.currency),
        ).firstOrNull()
}
