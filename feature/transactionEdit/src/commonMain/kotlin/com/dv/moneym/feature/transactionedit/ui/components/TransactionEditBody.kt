package com.dv.moneym.feature.transactionedit.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditUiState
import kotlinx.datetime.LocalDate
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_date_label
import moneym.feature.transactionedit.generated.resources.edit_date_today
import moneym.feature.transactionedit.generated.resources.edit_date_yesterday
import moneym.feature.transactionedit.generated.resources.edit_note_label
import moneym.feature.transactionedit.generated.resources.edit_note_placeholder
import moneym.feature.transactionedit.generated.resources.edit_type_expense
import moneym.feature.transactionedit.generated.resources.edit_type_income
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionEditScrollBody(
    state: TransactionEditUiState,
    todayDate: LocalDate,
    focusRequester: FocusRequester,
    onIntent: (TransactionEditIntent) -> Unit,
    onDatePickerOpen: () -> Unit,
    onCalculatorOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Derive currency code from selected account
    val currencyCode = remember(state.selectedAccountId, state.availableAccounts) {
        state.availableAccounts.firstOrNull { it.id == state.selectedAccountId }?.currency?.value ?: "EUR"
    }
    val amountValue = state.amountText.toDoubleOrNull() ?: 0.0
    val formattedAmount = if (amountValue == 0.0) {
        "0.00"
    } else {
        val major = amountValue.toLong()
        val fractional = ((amountValue - major.toDouble()) * 100).toLong().coerceAtLeast(0L)
        "$major.${fractional.toString().padStart(2, '0')}"
    }
    val todayLabel = stringResource(Res.string.edit_date_today)
    val dateText = state.date?.toFriendlyString(todayDate) ?: todayLabel

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MM.space.padding_2_5x, vertical = MM.space.padding_1x)
            .padding(bottom = MM.space.padding_2x),
    ) {
        TypeToggleBar(
            isExpense = state.type == TransactionType.EXPENSE,
            expenseLabel = stringResource(Res.string.edit_type_expense),
            incomeLabel = stringResource(Res.string.edit_type_income),
            onExpenseSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.EXPENSE)) },
            onIncomeSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MM.space.padding_3x))
        AmountDisplay(
            amountValue = amountValue,
            formattedAmount = formattedAmount,
            currencyCode = currencyCode,
            amountText = state.amountText,
            focusRequester = focusRequester,
            onAmountChanged = { onIntent(TransactionEditIntent.AmountChanged(it)) },
            onCalculatorClick = onCalculatorOpen,
        )
        // Date field — clickable display field (not an editable text input)
        DateDisplayField(
            dateText = dateText,
            label = stringResource(Res.string.edit_date_label),
            onClick = onDatePickerOpen,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MM.space.padding_1_5x))
        MmField(
            value = state.note,
            onValueChange = { onIntent(TransactionEditIntent.NoteChanged(it)) },
            label = stringResource(Res.string.edit_note_label),
            placeholder = stringResource(Res.string.edit_note_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )
        // Inline note suggestions — shown when the field has text and matches exist
        if (state.noteSuggestions.isNotEmpty()) {
            NoteSuggestionsRow(
                suggestions = state.noteSuggestions,
                onSelected = { onIntent(TransactionEditIntent.NoteSelected(it)) },
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(MM.space.padding_3x))
        CategoryPicker(
            categories = state.availableCategories,
            selectedCategoryId = state.selectedCategoryId,
            categoryError = state.categoryError,
            onCategorySelected = { onIntent(TransactionEditIntent.CategorySelected(it)) },
        )
    }
}

@Composable
private fun NoteSuggestionsRow(
    suggestions: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = MM.type
    val colors = MM.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        suggestions.forEach { suggestion ->
            MmChip(
                selected = false,
                onClick = { onSelected(suggestion) },
            ) {
                Text(
                    text = suggestion,
                    style = type.caption,
                    color = colors.text,
                    maxLines = 1,
                )
            }
        }
    }
}

