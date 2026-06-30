package com.dv.moneym.feature.transactions.list.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCategoryPickerSheet
import com.dv.moneym.core.ui.MmDeleteSheet
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.MmWalletPickerSheet
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.transactions.list.DayGroup
import com.dv.moneym.feature.transactions.list.TransactionListBody
import com.dv.moneym.feature.transactions.list.TransactionUiModel
import kotlinx.datetime.LocalDate
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_bulk_assign_category
import moneym.feature.transactions.generated.resources.transactions_bulk_cancel
import moneym.feature.transactions.generated.resources.transactions_bulk_category_confirm_body
import moneym.feature.transactions.generated.resources.transactions_bulk_category_confirm_title
import moneym.feature.transactions.generated.resources.transactions_bulk_delete_body
import moneym.feature.transactions.generated.resources.transactions_bulk_delete_title
import moneym.feature.transactions.generated.resources.transactions_bulk_edit_title
import moneym.feature.transactions.generated.resources.transactions_bulk_move_payment_mode
import moneym.feature.transactions.generated.resources.transactions_bulk_move_wallet
import moneym.feature.transactions.generated.resources.transactions_bulk_payment_confirm_body
import moneym.feature.transactions.generated.resources.transactions_bulk_payment_confirm_title
import moneym.feature.transactions.generated.resources.transactions_bulk_rate_error
import moneym.feature.transactions.generated.resources.transactions_bulk_rate_label
import moneym.feature.transactions.generated.resources.transactions_bulk_save
import moneym.feature.transactions.generated.resources.transactions_bulk_wallet_confirm_body
import moneym.feature.transactions.generated.resources.transactions_bulk_wallet_confirm_title
import moneym.feature.transactions.generated.resources.transactions_delete
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal data class TransactionSelectionActions(
    val clearSelection: () -> Unit,
    val requestDelete: () -> Unit,
    val requestEdit: () -> Unit,
)

internal data class VisibleTransactionSelection(
    val summary: TransactionSelectionUiState,
    val actions: TransactionSelectionActions,
)

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
internal fun TransactionPageScreen(
    yearMonth: YearMonth,
    onEditTransaction: (TransactionId) -> Unit,
    onEditRecurring: (RecurringTransactionId) -> Unit,
    onAddFirst: (() -> Unit)? = null,
    isVisible: Boolean = true,
    onSelectionChanged: (VisibleTransactionSelection?) -> Unit = {},
    scrollToTopRequest: Int = 0,
) {
    val vm = koinViewModel<TransactionPageViewModel>(
        key = yearMonth.toString(),
        parameters = { parametersOf(yearMonth) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = isVisible && state.selection.selectionMode) {
        vm.onIntent(TransactionPageIntent.ClearSelection)
    }

    LaunchedEffect(isVisible, state.selection) {
        onSelectionChanged(
            if (isVisible && state.selection.selectionMode) {
                VisibleTransactionSelection(
                    summary = state.selection,
                    actions = TransactionSelectionActions(
                        clearSelection = { vm.onIntent(TransactionPageIntent.ClearSelection) },
                        requestDelete = { vm.onIntent(TransactionPageIntent.DeleteRequested) },
                        requestEdit = { vm.onIntent(TransactionPageIntent.EditRequested) },
                    ),
                )
            } else null,
        )
    }

    TransactionPageBulkSheets(
        state = state,
        onIntent = vm::onIntent,
    )

    TransactionListBody(
        dayGroups = state.dayGroups,
        txDisplayPrefs = state.txDisplayPrefs,
        isLoading = state.isLoading,
        isEmpty = state.isEmpty,
        onEditTransaction = onEditTransaction,
        onEditRecurring = onEditRecurring,
        onAddFirst = onAddFirst,
        selectionMode = state.selection.selectionMode,
        selectedIds = state.selection.selectedIds,
        scrollToTopRequest = scrollToTopRequest,
        onToggleSelection = { vm.onIntent(TransactionPageIntent.TransactionPressed(it)) },
        onStartSelection = {
            if (!state.selection.selectionMode) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            vm.onIntent(TransactionPageIntent.TransactionLongPressed(it))
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TransactionPageBulkSheets(
    state: TransactionPageUiState,
    onIntent: (TransactionPageIntent) -> Unit,
) {
    when (val sheet = state.bulkSheet) {
        BulkSheetState.None -> Unit
        BulkSheetState.Actions -> BulkActionsSheet(
            selection = state.selection,
            onCategory = { onIntent(TransactionPageIntent.PickCategoryRequested) },
            onWallet = { onIntent(TransactionPageIntent.PickWalletRequested) },
            onPaymentMode = { onIntent(TransactionPageIntent.PickPaymentModeRequested) },
            onDismiss = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        BulkSheetState.DeleteConfirm -> MmDeleteSheet(
            title = stringResource(Res.string.transactions_bulk_delete_title),
            body = stringResource(Res.string.transactions_bulk_delete_body, state.selection.selectedCount),
            cancelText = stringResource(Res.string.transactions_bulk_cancel),
            confirmText = stringResource(Res.string.transactions_delete),
            onConfirm = { onIntent(TransactionPageIntent.ConfirmDelete) },
            onCancel = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        BulkSheetState.CategoryPicker -> MmCategoryPickerSheet(
            categories = state.availableCategories,
            selectedId = null,
            onPick = { onIntent(TransactionPageIntent.CategoryPicked(it)) },
            onDismiss = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        is BulkSheetState.CategoryConfirm -> ConfirmSheet(
            title = stringResource(Res.string.transactions_bulk_category_confirm_title),
            body = stringResource(Res.string.transactions_bulk_category_confirm_body, sheet.category.name),
            onConfirm = { onIntent(TransactionPageIntent.ConfirmCategory) },
            onCancel = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        BulkSheetState.WalletPicker -> MmWalletPickerSheet(
            accounts = state.availableAccounts,
            selectedAccountId = null,
            onSelect = { id ->
                state.availableAccounts.firstOrNull { it.id == id }
                    ?.let { onIntent(TransactionPageIntent.WalletPicked(it)) }
            },
            onDismiss = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        is BulkSheetState.WalletConfirm -> WalletConfirmSheet(
            account = sheet.account,
            requiresRate = sheet.requiresRate,
            rateText = state.bulkRateText,
            rateError = state.bulkRateError,
            onRateChanged = { onIntent(TransactionPageIntent.WalletRateChanged(it)) },
            onConfirm = { onIntent(TransactionPageIntent.ConfirmWallet) },
            onCancel = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        BulkSheetState.PaymentModePicker -> PaymentModePickerSheet(
            modes = state.paymentModes,
            onPick = { onIntent(TransactionPageIntent.PaymentModePicked(it.id)) },
            onDismiss = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
        is BulkSheetState.PaymentModeConfirm -> ConfirmSheet(
            title = stringResource(Res.string.transactions_bulk_payment_confirm_title),
            body = stringResource(
                Res.string.transactions_bulk_payment_confirm_body,
                sheet.paymentMode.name,
            ),
            onConfirm = { onIntent(TransactionPageIntent.ConfirmPaymentMode) },
            onCancel = { onIntent(TransactionPageIntent.DismissBulkSheet) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkActionsSheet(
    selection: TransactionSelectionUiState,
    onCategory: () -> Unit,
    onWallet: () -> Unit,
    onPaymentMode: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = MM.colors.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MM.colors.text3) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MmSheetHeader(title = stringResource(Res.string.transactions_bulk_edit_title), onClose = onDismiss)
            BulkActionRow(Icon.Tag, stringResource(Res.string.transactions_bulk_assign_category), onCategory)
            if (selection.canMoveWallet) {
                BulkActionRow(Icon.Wallet, stringResource(Res.string.transactions_bulk_move_wallet), onWallet)
            }
            if (selection.canMovePaymentMode) {
                BulkActionRow(Icon.Banknote, stringResource(Res.string.transactions_bulk_move_payment_mode), onPaymentMode)
            }
        }
    }
}

@Composable
private fun BulkActionRow(
    icon: Icon,
    label: String,
    onClick: () -> Unit,
) {
    MmRow(onClick = onClick, divider = false) {
        androidx.compose.material3.Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = MM.colors.text2,
            modifier = Modifier.padding(end = MM.dimen.padding_1x),
        )
        Text(text = label, style = MM.type.body, color = MM.colors.text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmSheet(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = MM.colors.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MM.colors.text3) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            MmSheetHeader(title = title, onClose = onCancel)
            Text(text = body, style = MM.type.body, color = MM.colors.text2)
            MmButton(
                text = stringResource(Res.string.transactions_bulk_save),
                onClick = onConfirm,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun WalletConfirmSheet(
    account: Account,
    requiresRate: Boolean,
    rateText: String,
    rateError: Boolean,
    onRateChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    ConfirmSheetScaffold(
        title = stringResource(Res.string.transactions_bulk_wallet_confirm_title),
        onCancel = onCancel,
        onConfirm = onConfirm,
    ) {
        Text(
            text = stringResource(Res.string.transactions_bulk_wallet_confirm_body, account.name),
            style = MM.type.body,
            color = MM.colors.text2,
        )
        if (requiresRate) {
            MmField(
                value = rateText,
                onValueChange = onRateChanged,
                label = stringResource(Res.string.transactions_bulk_rate_label, account.currency.value),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
            )
            if (rateError) {
                Text(
                    text = stringResource(Res.string.transactions_bulk_rate_error),
                    style = MM.type.caption,
                    color = MM.colors.danger,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmSheetScaffold(
    title: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = MM.colors.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MM.colors.text3) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            MmSheetHeader(title = title, onClose = onCancel)
            content()
            MmButton(
                text = stringResource(Res.string.transactions_bulk_save),
                onClick = onConfirm,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentModePickerSheet(
    modes: List<PaymentMode>,
    onPick: (PaymentMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = MM.colors.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MM.colors.text3) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MmSheetHeader(title = stringResource(Res.string.transactions_bulk_move_payment_mode), onClose = onDismiss)
            modes.forEach { mode ->
                Text(
                    text = mode.name,
                    style = MM.type.body,
                    color = MM.colors.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(mode) }
                        .padding(vertical = MM.dimen.padding_1_5x),
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TransactionPageScreenPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        TransactionListBody(
            dayGroups = listOf(
                DayGroup(
                    date = LocalDate(2026, 6, 10),
                    label = "Today, Jun 10",
                    transactions = listOf(
                        TransactionUiModel(
                            id = TransactionId(1L),
                            type = TransactionType.EXPENSE,
                            amountFormatted = "42.50",
                            amountMinorUnits = 4250L,
                            currency = "EUR",
                            isExpense = true,
                            categoryName = "Groceries",
                            categoryColorHex = "#4CAF50",
                            categoryIcon = Icon.Basket,
                            note = "Weekly shop",
                            occurredOn = LocalDate(2026, 6, 10),
                            paymentModeName = "Card",
                        ),
                        TransactionUiModel(
                            id = TransactionId(2L),
                            type = TransactionType.EXPENSE,
                            amountFormatted = "12.90",
                            amountMinorUnits = 1290L,
                            currency = "EUR",
                            isExpense = true,
                            categoryName = "Restaurants",
                            categoryColorHex = "#FF7043",
                            categoryIcon = Icon.Restaurant,
                            note = "Lunch",
                            occurredOn = LocalDate(2026, 6, 10),
                            paymentModeName = "Cash",
                        ),
                    ),
                ),
                DayGroup(
                    date = LocalDate(2026, 6, 9),
                    label = "Yesterday, Jun 9",
                    transactions = listOf(
                        TransactionUiModel(
                            id = TransactionId(3L),
                            type = TransactionType.INCOME,
                            amountFormatted = "2500.00",
                            amountMinorUnits = 250000L,
                            currency = "EUR",
                            isExpense = false,
                            categoryName = "Salary",
                            categoryColorHex = "#66BB6A",
                            categoryIcon = Icon.Banknote,
                            note = "Monthly salary",
                            occurredOn = LocalDate(2026, 6, 9),
                            paymentModeName = "Bank transfer",
                        ),
                        TransactionUiModel(
                            id = TransactionId(4L),
                            type = TransactionType.EXPENSE,
                            amountFormatted = "65.00",
                            amountMinorUnits = 6500L,
                            currency = "EUR",
                            isExpense = true,
                            categoryName = "Transport",
                            categoryColorHex = "#42A5F5",
                            categoryIcon = Icon.Car,
                            note = "Fuel",
                            occurredOn = LocalDate(2026, 6, 9),
                            paymentModeName = "Card",
                        ),
                    ),
                ),
            ),
            txDisplayPrefs = com.dv.moneym.core.model.TxDisplayPrefs(),
            isLoading = false,
            isEmpty = false,
            onEditTransaction = {},
            onEditRecurring = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
