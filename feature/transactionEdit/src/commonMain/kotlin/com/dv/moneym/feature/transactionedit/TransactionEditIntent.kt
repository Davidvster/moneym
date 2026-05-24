package com.dv.moneym.feature.transactionedit

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate

internal sealed interface TransactionEditIntent {
    data class TypeChanged(val type: TransactionType) : TransactionEditIntent
    data class AmountChanged(val text: String) : TransactionEditIntent
    data class DateChanged(val date: LocalDate) : TransactionEditIntent
    data object YesterdayTodayClicked : TransactionEditIntent
    data class CategorySelected(val id: CategoryId) : TransactionEditIntent
    data class AccountSelected(val id: AccountId) : TransactionEditIntent
    data class NoteChanged(val note: String) : TransactionEditIntent
    data class NoteSelected(val note: String) : TransactionEditIntent
    data class PaymentModeSelected(val id: PaymentModeId?) : TransactionEditIntent
    data object SaveRequested : TransactionEditIntent
    data object DeleteRequested : TransactionEditIntent
    data object DeleteConfirmed : TransactionEditIntent
    data object DeleteCancelled : TransactionEditIntent
    data class ShowDeleteDialog(val visible: Boolean) : TransactionEditIntent
}
