package com.dv.moneym.feature.transactionedit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditEffect
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditUiState
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditViewModel
import com.dv.moneym.feature.transactionedit.ui.components.CalculatorBottomSheet
import com.dv.moneym.feature.transactionedit.ui.components.TransactionDatePickerDialog
import com.dv.moneym.feature.transactionedit.ui.components.TransactionDeleteSheet
import com.dv.moneym.feature.transactionedit.ui.components.TransactionEditModalHeader
import com.dv.moneym.feature.transactionedit.ui.components.TransactionEditSaveBar
import com.dv.moneym.feature.transactionedit.ui.components.TransactionEditScrollBody
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
    val sessionKey: String = kotlin.random.Random.nextLong().toString(),
) : ModalKey

fun EntryProviderScope<NavKey>.transactionEditEntry(
    onDismiss: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<TransactionEditKey>(metadata = metadata) { key ->
    TransactionEditScreen(
        transactionId = key.id?.let { TransactionId(it) },
        sessionKey = key.sessionKey,
        onDismiss = onDismiss,
    )
}

@Composable
fun TransactionEditScreen(
    transactionId: TransactionId?,
    sessionKey: String = "",
    onDismiss: () -> Unit,
    viewModel: TransactionEditViewModel = koinViewModel(
        key = sessionKey.ifEmpty { transactionId?.value?.toString() ?: "new" },
        parameters = { parametersOf(transactionId) },
    ),
) {
    val state by viewModel.state.collectAsState()

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

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
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

    if (showDeleteDialog) {
        TransactionDeleteSheet(
            onConfirm = {
                onIntent(TransactionEditIntent.DeleteConfirmed); showDeleteDialog = false
            },
            onCancel = { showDeleteDialog = false },
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
            onDeleteClick = { showDeleteDialog = true },
        )
        TransactionEditScrollBody(
            state = state,
            todayDate = todayDate,
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
