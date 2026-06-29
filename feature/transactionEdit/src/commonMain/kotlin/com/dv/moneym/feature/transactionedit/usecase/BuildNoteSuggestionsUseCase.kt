package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class BuildNoteSuggestionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val suggestNotes: SuggestNotesUseCase,
) {
    suspend operator fun invoke(query: String, today: LocalDate): List<String> {
        if (query.isBlank()) return emptyList()
        return suggestNotes(transactionRepository.observeAll().first(), query, today)
    }
}
