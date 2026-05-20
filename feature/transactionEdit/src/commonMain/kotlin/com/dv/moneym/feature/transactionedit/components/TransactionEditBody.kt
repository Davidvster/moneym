package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.transactionedit.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.TransactionEditUiState
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_date_today
import moneym.feature.transactionedit.generated.resources.edit_date_yesterday
import moneym.feature.transactionedit.generated.resources.edit_note_placeholder
import moneym.feature.transactionedit.generated.resources.edit_payment_mode_label
import moneym.feature.transactionedit.generated.resources.edit_type_expense
import moneym.feature.transactionedit.generated.resources.edit_type_income
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionEditScrollBody(
    state: TransactionEditUiState,
    focusRequester: FocusRequester,
    onIntent: (TransactionEditIntent) -> Unit,
    onDatePickerOpen: () -> Unit,
    onCalculatorOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Derive currency code from selected account
    val currencyCode = remember(state.selectedAccountId, state.availableAccounts) {
        state.availableAccounts.firstOrNull { it.id == state.selectedAccountId }?.currency?.value
            ?: "EUR"
    }
    val todayLabel = stringResource(Res.string.edit_date_today)
    val yesterdayLabel = stringResource(Res.string.edit_date_yesterday)
    val dateText = if (state.date != null) state.date.toFriendlyString(state.date) else todayLabel
    val notesFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x)
            .padding(bottom = MM.dimen.padding_2x),
    ) {
        TypeToggleBar(
            isExpense = state.type == TransactionType.EXPENSE,
            expenseLabel = stringResource(Res.string.edit_type_expense),
            incomeLabel = stringResource(Res.string.edit_type_income),
            onExpenseSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.EXPENSE)) },
            onIncomeSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MM.dimen.padding_2x))

        // Date row: date field + quick Yesterday/Today button — same height (MmButton.Sm)
        Row(
            modifier = Modifier.fillMaxWidth().height(MM.dimen.padding_4x),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MmButton(
                text = dateText,
                onClick = onDatePickerOpen,
                leadingIcon = Icon.Calendar.imageVector,
                size = MmButtonSize.Md,
                variant = MmButtonVariant.Secondary,
                modifier = Modifier.weight(0.7f).fillMaxHeight(),
            )
            MmButton(
                text = if (state.isToday == true) yesterdayLabel else todayLabel,
                onClick = { onIntent(TransactionEditIntent.YesterdayTodayClicked) },
                size = MmButtonSize.Md,
                variant = MmButtonVariant.Secondary,
                modifier = Modifier.weight(0.3f).fillMaxHeight(),
            )
        }

        AmountDisplay(
            amountText = state.amountText,
            currencyCode = currencyCode,
            focusRequester = focusRequester,
            onAmountChanged = { onIntent(TransactionEditIntent.AmountChanged(it)) },
            onCalculatorClick = onCalculatorOpen,
            notesFocusRequester = notesFocusRequester,
        )

        MmField(
            value = state.note,
            onValueChange = { onIntent(TransactionEditIntent.NoteChanged(it)) },
            placeholder = stringResource(Res.string.edit_note_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(notesFocusRequester),
        )
        // Inline note suggestions — shown when the field has text and matches exist
        if (state.noteSuggestions.isNotEmpty()) {
            NoteSuggestionsRow(
                suggestions = state.noteSuggestions,
                onSelected = { onIntent(TransactionEditIntent.NoteSelected(it)) },
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(MM.dimen.padding_3x))
        CategoryPicker(
            categories = state.availableCategories.filter { it.type == state.type },
            selectedCategoryId = state.selectedCategoryId,
            categoryError = state.categoryError,
            onCategorySelected = { onIntent(TransactionEditIntent.CategorySelected(it)) },
        )
        if ((state.showPaymentMode || state.selectedPaymentModeId != null) && state.paymentModes.isNotEmpty()) {
            Spacer(Modifier.height(MM.dimen.padding_3x))
            PaymentModePicker(
                modes = state.paymentModes.map { it.id to it.name },
                selectedId = state.selectedPaymentModeId,
                onSelected = { onIntent(TransactionEditIntent.PaymentModeSelected(it)) },
            )
        }
    }
}

@Composable
internal fun PaymentModePicker(
    modes: List<Pair<PaymentModeId, String>>,
    selectedId: PaymentModeId?,
    onSelected: (PaymentModeId?) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Text(
        text = stringResource(Res.string.edit_payment_mode_label).uppercase(),
        style = type.micro,
        color = colors.text3,
    )
    Spacer(Modifier.height(MM.dimen.padding_1_5x))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
    ) {
        modes.forEach { (id, name) ->
            val isSelected = id == selectedId
            MmChip(
                selected = isSelected,
                onClick = {
                    // Tap selected chip to deselect
                    onSelected(if (isSelected) null else id)
                },
            ) {
                Text(
                    text = name,
                    style = type.caption,
                    color = if (isSelected) colors.bg else colors.text,
                    maxLines = 1,
                )
            }
        }
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

