package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.SuggestionSource
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.transactionedit.TransactionEditDraft
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import kotlinx.datetime.LocalDate

data class SavedTransactionResult(
    val date: LocalDate?,
)

class SaveTransactionEditUseCase(
    private val upsertTransaction: UpsertTransactionUseCase,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        outcome: ValidationOutcome.Ok,
        draft: TransactionEditDraft?,
        suggestionSources: Map<String, SuggestionSource>,
        today: LocalDate,
    ): SavedTransactionResult {
        val rule = outcome.rule
        if (rule == null) {
            val id = upsertTransaction(outcome.transaction)
            acceptDraftIfNeeded(draft, suggestionSources, id, outcome.transaction.updatedAt.toEpochMilliseconds())
            return SavedTransactionResult(outcome.transaction.occurredOn)
        }

        val ruleId = recurringTransactionRepository.upsert(rule)
        val startDate = rule.startDate
        if (startDate > today) return SavedTransactionResult(null)

        val id = upsertTransaction(outcome.transaction.copy(recurringId = ruleId))
        acceptDraftIfNeeded(draft, suggestionSources, id, outcome.transaction.updatedAt.toEpochMilliseconds())
        recurringTransactionRepository.updateCursor(ruleId, startDate)
        return SavedTransactionResult(outcome.transaction.occurredOn)
    }

    private suspend fun acceptDraftIfNeeded(
        draft: TransactionEditDraft?,
        suggestionSources: Map<String, SuggestionSource>,
        transactionId: TransactionId,
        decidedAt: Long,
    ) {
        if (draft == null) return
        transactionRepository.setExternalId(transactionId, draft.externalId)
        suggestionSources[draft.suggestionSourceType]?.accept(
            id = draft.suggestionId,
            createdTransactionId = transactionId.value,
            decidedAt = decidedAt,
        )
    }
}
