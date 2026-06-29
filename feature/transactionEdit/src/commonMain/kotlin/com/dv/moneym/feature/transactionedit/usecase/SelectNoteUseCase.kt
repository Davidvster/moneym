package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first

data class SelectNoteResult(
    val note: String,
    val categoryId: CategoryId?,
)

class SelectNoteUseCase(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(note: String, type: TransactionType): SelectNoteResult {
        val match = transactionRepository.observeAll()
            .first()
            .filter { it.note == note && it.type == type }
            .maxByOrNull { it.occurredOn }
        return SelectNoteResult(
            note = note,
            categoryId = match?.categoryId,
        )
    }
}
