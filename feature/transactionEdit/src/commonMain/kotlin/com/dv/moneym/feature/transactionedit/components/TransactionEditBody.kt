package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
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
import kotlinx.datetime.LocalDate
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_budget_section_title
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
            ?: "USD"
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
        state.budgetRemaining?.let {
            Spacer(Modifier.height(MM.dimen.padding_3x))
            Text(
                text = stringResource(Res.string.edit_budget_section_title).uppercase(),
                style = MM.type.micro,
                color = MM.colors.text3,
            )
            Spacer(Modifier.height(MM.dimen.padding_1_5x))
            BudgetRemainingChip(
                remaining = it,
                projected = state.budgetProjected,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if ((state.showPaymentMode || state.selectedPaymentModeId != null) && state.paymentModes.isNotEmpty()) {
            Spacer(Modifier.height(MM.dimen.padding_3x))
            PaymentModePicker(
                modes = state.paymentModes.map { it.id to it.name },
                selectedId = state.selectedPaymentModeId,
                onSelected = { onIntent(TransactionEditIntent.PaymentModeSelected(it)) },
            )
        }
        if (!state.isEditMode) {
            Spacer(Modifier.height(MM.dimen.padding_3x))
            RecurrenceSection(state = state, onIntent = onIntent)
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

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
private fun TransactionEditScrollBodyPreview() {
    val epoch = kotlin.time.Instant.fromEpochSeconds(0)
    val eur = CurrencyCode("EUR")
    val categories = listOf(
        Category(
            id = CategoryId(1),
            name = "Groceries",
            iconKey = "basket",
            colorHex = "#4A8E5C",
            isUserCreated = false,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
        Category(
            id = CategoryId(2),
            name = "Transport",
            iconKey = "car",
            colorHex = "#3A82A5",
            isUserCreated = false,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
    )
    val accounts = listOf(
        Account(
            id = AccountId(1),
            name = "Main",
            type = AccountType.CASH,
            currency = eur,
            isDefault = true,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
    )
    val paymentModes = listOf(
        PaymentMode(id = PaymentModeId(1), name = "Cash", createdAt = epoch, updatedAt = epoch),
        PaymentMode(id = PaymentModeId(2), name = "Card", createdAt = epoch, updatedAt = epoch),
    )
    MoneyMTheme {
        TransactionEditScrollBody(
            state = TransactionEditUiState(
                type = TransactionType.EXPENSE,
                amountText = "42.50",
                date = LocalDate(2026, 6, 10),
                isToday = true,
                selectedCategoryId = categories.first().id,
                selectedAccountId = accounts.first().id,
                note = "Weekly shopping",
                noteSuggestions = listOf("Groceries", "Supermarket"),
                availableCategories = categories,
                availableAccounts = accounts,
                paymentModes = paymentModes,
                selectedPaymentModeId = paymentModes.first().id,
                showPaymentMode = true,
            ),
            focusRequester = remember { FocusRequester() },
            onIntent = {},
            onDatePickerOpen = {},
            onCalculatorOpen = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

