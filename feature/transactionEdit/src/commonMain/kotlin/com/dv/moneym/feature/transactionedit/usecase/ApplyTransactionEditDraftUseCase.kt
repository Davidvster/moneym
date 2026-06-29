package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.feature.transactionedit.TransactionEditDraft
import com.dv.moneym.feature.transactionedit.TransactionEditUiState
import kotlinx.datetime.LocalDate

class ApplyTransactionEditDraftUseCase {
    internal operator fun invoke(
        state: TransactionEditUiState,
        draft: TransactionEditDraft,
        defaultAccount: Account?,
        today: LocalDate,
    ): TransactionEditUiState {
        if (state.draftApplied) return state
        val draftType = draft.type
        val draftDate = runCatching { LocalDate.parse(draft.dateIso) }.getOrNull() ?: state.date
        val accountId =
            draft.accountId?.let { AccountId(it) } ?: state.selectedAccountId ?: defaultAccount?.id
        return state.copy(
            draftApplied = true,
            type = draftType,
            amountText = draft.amountMinor.toAmountText(),
            date = draftDate,
            isToday = draftDate == today,
            note = draft.note.orEmpty(),
            selectedAccountId = accountId,
            visibleCategories = state.availableCategories.filter { it.type == draftType },
            selectedCategoryId = draft.categoryId?.let { CategoryId(it) }
                ?: state.selectedCategoryId
                ?: state.availableCategories.firstOrNull { it.type == draftType }?.id,
        )
    }
}

private fun Long.toAmountText(): String {
    val abs = if (this < 0) -this else this
    val major = abs / 100
    val cents = abs % 100
    return "$major.${cents.toString().padStart(2, '0')}"
}
