package com.dv.moneym.feature.transactionedit.domain

import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.data.transactions.TransactionRepository

class UpsertTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): TransactionId =
        repository.upsert(transaction)
}
