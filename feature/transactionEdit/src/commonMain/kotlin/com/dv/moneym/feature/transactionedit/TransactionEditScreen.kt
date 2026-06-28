package com.dv.moneym.feature.transactionedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.navigation.ModalKey
import kotlinx.datetime.LocalDate
import com.dv.moneym.feature.transactionedit.components.CalculatorBottomSheet
import com.dv.moneym.feature.transactionedit.components.TransactionDatePickerDialog
import com.dv.moneym.feature.transactionedit.components.TransactionDeleteSheet
import com.dv.moneym.feature.transactionedit.components.TransactionEditModalHeader
import com.dv.moneym.feature.transactionedit.components.TransactionEditSaveBar
import com.dv.moneym.feature.transactionedit.components.TransactionEditScrollBody
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock

@Serializable
data class TransactionEditKey(
    val id: Long? = null,
    val draft: TransactionEditDraft? = null,
) : ModalKey

fun EntryProviderScope<NavKey>.transactionEditEntry(
    onDismiss: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<TransactionEditKey>(metadata = metadata) { key ->
    TransactionEditScreen(
        transactionId = key.id?.let { TransactionId(it) },
        draft = key.draft.takeIf { key.id == null },
        onDismiss = onDismiss,
    )
}

@Composable
private fun TransactionEditScreen(
    transactionId: TransactionId?,
    draft: TransactionEditDraft?,
    onDismiss: () -> Unit,
    viewModel: TransactionEditViewModel = koinViewModel(
        parameters = { parametersOf(transactionId, draft) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TransactionEditEffect.Saved, TransactionEditEffect.Deleted -> onDismiss()
            }
        }
    }

    TransactionEditContent(
        state = state,
        onIntent = viewModel::onIntent,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionEditContent(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showCalculator by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // Auto-focus the amount field for new transactions
    LaunchedEffect(state.isEditMode) {
        if (!state.isEditMode) {
            delay(150)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val todayDate = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val yesterdayDate = remember { todayDate.minus(DatePeriod(days = 1)) }

    if (state.showDeleteDialog) {
        TransactionDeleteSheet(
            onConfirm = {
                onIntent(TransactionEditIntent.DeleteConfirmed)
                onIntent(TransactionEditIntent.ShowDeleteDialog(false))
            },
            onCancel = { onIntent(TransactionEditIntent.ShowDeleteDialog(false)) },
        )
    }
    if (showDatePicker) {
        TransactionDatePickerDialog(
            state = state,
            todayDate = todayDate,
            yesterdayDate = yesterdayDate,
            onIntent = onIntent,
            onDismiss = { showDatePicker = false },
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

    // imePadding() ensures the Save button is always visible above the keyboard
    Column(modifier = Modifier.fillMaxSize().background(colors.bg).imePadding()) {
        TransactionEditModalHeader(
            isEditMode = state.isEditMode,
            onDismiss = onDismiss,
            onDeleteClick = { onIntent(TransactionEditIntent.ShowDeleteDialog(true)) },
            accounts = state.availableAccounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = { onIntent(TransactionEditIntent.AccountSelected(it)) },
        )
        TransactionEditScrollBody(
            state = state,
            focusRequester = focusRequester,
            onIntent = onIntent,
            onDatePickerOpen = { showDatePicker = true },
            onCalculatorOpen = { showCalculator = true },
            modifier = Modifier.weight(1f),
        )
        TransactionEditSaveBar(
            isEditMode = state.isEditMode,
            isSaving = state.isSaving,
            onSave = { onIntent(TransactionEditIntent.SaveRequested) },
        )
    }
}

// Store screenshot — new-transaction screen, light theme, EUR, May 2026. Rendered
// directly by StoreScreenshotTest (no @Preview so the package scanner skips it).
@Composable
internal fun StoreTransactionEditPreview() {
    val epoch = kotlin.time.Instant.fromEpochSeconds(0)
    val eur = CurrencyCode("EUR")
    val categories = listOf(
        Category(CategoryId(1), "Groceries", "basket", "#4A8E5C", false, false, epoch, epoch),
        Category(CategoryId(2), "Restaurants", "restaurant", "#FF7043", false, false, epoch, epoch),
        Category(CategoryId(3), "Transport", "car", "#3A82A5", false, false, epoch, epoch),
        Category(CategoryId(4), "Shopping", "bag", "#AB47BC", false, false, epoch, epoch),
        Category(CategoryId(5), "Home", "home", "#26A69A", false, false, epoch, epoch),
        Category(CategoryId(6), "Entertainment", "play_circle", "#EC407A", false, false, epoch, epoch),
    )
    val accounts = listOf(
        Account(AccountId(1), "Main", AccountType.CASH, eur, true, false, epoch, epoch),
    )
    val paymentModes = listOf(
        PaymentMode(PaymentModeId(1), "Cash", epoch, epoch),
        PaymentMode(PaymentModeId(2), "Card", epoch, epoch),
    )
    CompositionLocalProvider(LocalInspectionMode provides true) {
        MoneyMTheme(darkTheme = false) {
            TransactionEditContent(
                state = TransactionEditUiState(
                    type = TransactionType.EXPENSE,
                    amountText = "42.50",
                    date = LocalDate(2026, 5, 26),
                    isToday = false,
                    selectedCategoryId = CategoryId(1),
                    selectedAccountId = AccountId(1),
                    note = "Weekly groceries",
                    availableCategories = categories,
                    availableAccounts = accounts,
                    paymentModes = paymentModes,
                    selectedPaymentModeId = PaymentModeId(2),
                    showPaymentMode = true,
                ),
                onIntent = {},
                onDismiss = {},
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@androidx.compose.runtime.Composable
private fun TransactionEditContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        TransactionEditContent(
            state = TransactionEditUiState(
                amountText = "42.50",
                date = kotlinx.datetime.LocalDate(2026, 5, 26),
                isToday = true,
            ),
            onIntent = {},
            onDismiss = {},
        )
    }
}
