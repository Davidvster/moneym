package com.dv.moneym.feature.transactionedit

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.MonthlyDayKind
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
    data class RecurringToggled(val on: Boolean) : TransactionEditIntent
    data class FreqUnitChanged(val unit: FreqUnit) : TransactionEditIntent
    data class FreqIntervalChanged(val value: Int) : TransactionEditIntent
    data class WeekDayChanged(val day: Int) : TransactionEditIntent
    data class MonthDayChanged(val kind: MonthlyDayKind) : TransactionEditIntent
    data class EndKindChanged(val kind: EndKind) : TransactionEditIntent
    data class EndCountChanged(val value: Int) : TransactionEditIntent
    data class EndDateChanged(val date: LocalDate) : TransactionEditIntent
}
