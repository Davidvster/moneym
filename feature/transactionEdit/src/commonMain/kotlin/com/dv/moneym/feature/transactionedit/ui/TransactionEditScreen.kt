package com.dv.moneym.feature.transactionedit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditEffect
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditUiState
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
    val type = MM.type

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // Derive currency code from selected account
    val currencyCode = remember(state.selectedAccountId, state.availableAccounts) {
        state.availableAccounts
            .firstOrNull { it.id == state.selectedAccountId }
            ?.currency?.value
            ?: "EUR"
    }

    // Parse amountText to Double for display logic
    val amountValue = state.amountText.toDoubleOrNull() ?: 0.0
    val formattedAmount = if (amountValue == 0.0) {
        "0.00"
    } else {
        // Format to 2 decimal places
        val major = amountValue.toLong()
        val fractional = ((amountValue - major.toDouble()) * 100).toLong().coerceAtLeast(0L)
        "$major.${fractional.toString().padStart(2, '0')}"
    }

    // Delete confirm dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(TransactionEditIntent.DeleteConfirmed)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        val initialMillis = state.date
            ?.atStartOfDayIn(TimeZone.UTC)
            ?.toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Convert millis back to LocalDate
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onIntent(TransactionEditIntent.DateChanged(localDate))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Modal header — 52dp tall
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmIconButton(
                icon = MmIcons.close,
                onClick = onDismiss,
            )
            Text(
                text = if (state.isEditMode) "Edit transaction" else "New transaction",
                style = type.title3,
                color = colors.text,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            if (state.isEditMode) {
                MmIconButton(
                    icon = MmIcons.trash,
                    onClick = { showDeleteDialog = true },
                    variant = MmIconButtonVariant.Danger,
                )
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }

        // Scrollable body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 100.dp),
        ) {
            // Expense / Income segmented
            MmSegmented(
                options = listOf("Expense", "Income"),
                selectedIndex = if (state.type == TransactionType.EXPENSE) 0 else 1,
                onOptionSelected = { index ->
                    onIntent(
                        TransactionEditIntent.TypeChanged(
                            if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // Big amount display — tapping opens numeric keyboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "AMOUNT",
                    style = type.micro,
                    color = colors.text3,
                )
                Spacer(Modifier.height(8.dp))

                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = interactionSource,
                    ) {
                        focusRequester.requestFocus()
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = currencyCode,
                            style = type.bodyMono,
                            color = colors.text3,
                        )
                        Text(
                            text = formattedAmount,
                            style = type.display.copy(
                                fontSize = 52.sp,
                                color = if (amountValue == 0.0) colors.text3 else colors.text,
                            ),
                        )
                    }
                }

                // Hidden BasicTextField for numeric input
                BasicTextField(
                    value = state.amountText,
                    onValueChange = { onIntent(TransactionEditIntent.AmountChanged(it)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester),
                    decorationBox = { },
                )
            }

            // Date field
            val dateText = state.date?.toString() ?: ""
            MmField(
                value = dateText,
                onValueChange = {},
                label = "Date",
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        showDatePicker = true
                    },
            )

            Spacer(Modifier.height(12.dp))

            // Note field
            MmField(
                value = state.note,
                onValueChange = { onIntent(TransactionEditIntent.NoteChanged(it)) },
                label = "Note (optional)",
                placeholder = "Add a note...",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // Category picker
            Text(
                text = "CATEGORY",
                style = type.micro,
                color = colors.text3,
            )
            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.availableCategories.forEach { cat ->
                    CategoryChip(
                        category = cat,
                        isSelected = cat.id == state.selectedCategoryId,
                        onClick = { onIntent(TransactionEditIntent.CategorySelected(cat.id)) },
                    )
                }
            }
        }

        // Pinned save bar
        val dividerColor = colors.divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .background(colors.bg)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        ) {
            MmButton(
                text = if (state.isEditMode) "Save changes" else "Add transaction",
                onClick = { onIntent(TransactionEditIntent.SaveRequested) },
                variant = MmButtonVariant.Accent,
                size = MmButtonSize.Lg,
                leadingIcon = MmIcons.check,
                fullWidth = true,
                enabled = !state.isSaving,
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val catColor = categoryColor(category.colorHex)
    val catIcon = iconForKey(category.iconKey)

    MmChip(
        selected = isSelected,
        onClick = onClick,
        leadingContent = {
            CategoryIconTile(
                categoryName = category.name,
                categoryColor = catColor,
                categoryIcon = catIcon,
                size = 20.dp,
                variant = IndicatorStyle.IconTile,
            )
        },
    ) {
        Text(
            text = category.name,
            style = type.caption,
            color = if (isSelected) colors.bg else colors.text,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
