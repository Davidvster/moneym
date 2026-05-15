package com.dv.moneym.feature.transactionedit.domain

import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.data.transactions.TransactionRepository

class DeleteTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: TransactionId) = repository.delete(id)
}
