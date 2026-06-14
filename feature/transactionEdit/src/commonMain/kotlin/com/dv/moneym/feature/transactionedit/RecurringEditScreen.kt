package com.dv.moneym.feature.transactionedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.feature.transactionedit.components.AmountDisplay
import com.dv.moneym.feature.transactionedit.components.CalculatorBottomSheet
import com.dv.moneym.feature.transactionedit.components.CategoryPicker
import com.dv.moneym.feature.transactionedit.components.PaymentModePicker
import com.dv.moneym.feature.transactionedit.components.RecurrenceSection
import com.dv.moneym.feature.transactionedit.components.TransactionDeleteSheet
import com.dv.moneym.feature.transactionedit.components.TransactionEditModalHeader
import com.dv.moneym.feature.transactionedit.components.TransactionEditSaveBar
import com.dv.moneym.feature.transactionedit.components.TypeToggleBar
import kotlinx.serialization.Serializable
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_note_placeholder
import moneym.feature.transactionedit.generated.resources.edit_save
import moneym.feature.transactionedit.generated.resources.edit_type_expense
import moneym.feature.transactionedit.generated.resources.edit_type_income
import moneym.feature.transactionedit.generated.resources.tx_recurring_starts
import moneym.feature.transactionedit.generated.resources.tx_recurring_update_button
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class RecurringEditKey(val ruleId: Long) : ModalKey

fun EntryProviderScope<NavKey>.recurringEditEntry(
    onDismiss: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<RecurringEditKey>(metadata = metadata) { key ->
    RecurringEditScreen(
        ruleId = RecurringTransactionId(key.ruleId),
        onDismiss = onDismiss,
    )
}

@Composable
private fun RecurringEditScreen(
    ruleId: RecurringTransactionId,
    onDismiss: () -> Unit,
    viewModel: RecurringEditViewModel = koinViewModel(
        parameters = { parametersOf(ruleId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { _ -> onDismiss() }
    }
    RecurringEditContent(
        state = state,
        onIntent = viewModel::onIntent,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringEditContent(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    var showCalculator by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val currencyCode = remember(state.selectedAccountId, state.availableAccounts) {
        val accounts = state.availableAccounts
        (accounts.firstOrNull { it.id == state.selectedAccountId }
            ?: accounts.firstOrNull { it.isDefault }
            ?: accounts.firstOrNull())?.currency?.value
            ?: "USD"
    }

    if (state.showDeleteDialog) {
        TransactionDeleteSheet(
            onConfirm = {
                onIntent(TransactionEditIntent.DeleteConfirmed)
                onIntent(TransactionEditIntent.ShowDeleteDialog(false))
            },
            onCancel = { onIntent(TransactionEditIntent.ShowDeleteDialog(false)) },
        )
    }
    if (showCalculator) {
        CalculatorBottomSheet(
            initialAmountText = state.amountText,
            onDismiss = { showCalculator = false },
            onAmountSaved = { result ->
                onIntent(TransactionEditIntent.AmountChanged(result))
                showCalculator = false
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).imePadding()) {
        TransactionEditModalHeader(
            isEditMode = state.isEditMode,
            onDismiss = onDismiss,
            onDeleteClick = { onIntent(TransactionEditIntent.ShowDeleteDialog(true)) },
            accounts = state.availableAccounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = { onIntent(TransactionEditIntent.AccountSelected(it)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_1x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            TypeToggleBar(
                isExpense = state.type == TransactionType.EXPENSE,
                expenseLabel = stringResource(Res.string.edit_type_expense),
                incomeLabel = stringResource(Res.string.edit_type_income),
                onExpenseSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.EXPENSE)) },
                onIncomeSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME)) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Start date is locked — display only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                Text(
                    text = stringResource(Res.string.tx_recurring_starts, state.date.toString()),
                    style = MM.type.micro,
                    color = MM.colors.text3,
                )
            }

            AmountDisplay(
                amountText = state.amountText,
                currencyCode = currencyCode,
                focusRequester = focusRequester,
                onAmountChanged = { onIntent(TransactionEditIntent.AmountChanged(it)) },
                onCalculatorClick = { showCalculator = true },
                notesFocusRequester = remember { FocusRequester() },
            )

            MmField(
                value = state.note,
                onValueChange = { onIntent(TransactionEditIntent.NoteChanged(it)) },
                placeholder = stringResource(Res.string.edit_note_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryPicker(
                categories = state.availableCategories.filter { it.type == state.type },
                selectedCategoryId = state.selectedCategoryId,
                categoryError = state.categoryError,
                onCategorySelected = { onIntent(TransactionEditIntent.CategorySelected(it)) },
            )

            if ((state.showPaymentMode || state.selectedPaymentModeId != null) && state.paymentModes.isNotEmpty()) {
                PaymentModePicker(
                    modes = state.paymentModes.map { it.id to it.name },
                    selectedId = state.selectedPaymentModeId,
                    onSelected = { onIntent(TransactionEditIntent.PaymentModeSelected(it)) },
                )
            }

            RecurrenceSection(state = state, onIntent = onIntent)
        }
        TransactionEditSaveBar(
            isEditMode = state.isEditMode,
            isSaving = state.isSaving,
            onSave = { onIntent(TransactionEditIntent.SaveRequested) },
            saveLabel = if (state.isEditMode) {
                stringResource(Res.string.tx_recurring_update_button)
            } else {
                stringResource(Res.string.edit_save)
            },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@androidx.compose.runtime.Composable
private fun RecurringEditContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        RecurringEditContent(
            state = TransactionEditUiState(
                isRecurring = true,
                amountText = "100.00",
                date = kotlinx.datetime.LocalDate(2026, 5, 26),
            ),
            onIntent = {},
            onDismiss = {},
        )
    }
}
