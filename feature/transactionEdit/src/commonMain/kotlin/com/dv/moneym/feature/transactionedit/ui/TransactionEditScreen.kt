package com.dv.moneym.feature.transactionedit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
import com.dv.moneym.core.ui.calculator
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditEffect
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditUiState
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditViewModel
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_add_transaction
import moneym.feature.transactionedit.generated.resources.edit_amount_label
import moneym.feature.transactionedit.generated.resources.edit_calculator_title
import moneym.feature.transactionedit.generated.resources.edit_category_error
import moneym.feature.transactionedit.generated.resources.edit_category_label
import moneym.feature.transactionedit.generated.resources.edit_date_label
import moneym.feature.transactionedit.generated.resources.edit_date_ok
import moneym.feature.transactionedit.generated.resources.edit_date_today
import moneym.feature.transactionedit.generated.resources.edit_date_yesterday
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_body
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_cancel
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_ok
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_title
import moneym.feature.transactionedit.generated.resources.edit_note_label
import moneym.feature.transactionedit.generated.resources.edit_note_placeholder
import moneym.feature.transactionedit.generated.resources.edit_save_changes
import moneym.feature.transactionedit.generated.resources.edit_title_add
import moneym.feature.transactionedit.generated.resources.edit_title_edit
import moneym.feature.transactionedit.generated.resources.edit_type_expense
import moneym.feature.transactionedit.generated.resources.edit_type_income
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
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
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val yesterdayDate = remember { todayDate.minus(DatePeriod(days = 1)) }

    if (showDeleteDialog) {
        TransactionDeleteSheet(
            onConfirm = { onIntent(TransactionEditIntent.DeleteConfirmed); showDeleteDialog = false },
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
            yesterdayDate = yesterdayDate,
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

// ─── Scrollable body ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionEditScrollBody(
    state: TransactionEditUiState,
    todayDate: LocalDate,
    yesterdayDate: LocalDate,
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
    val yesterdayLabel = stringResource(Res.string.edit_date_yesterday)
    val dateText = state.date?.toFriendlyString(todayDate, yesterdayDate, todayLabel, yesterdayLabel) ?: todayLabel

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 16.dp),
    ) {
        TypeToggleBar(
            isExpense = state.type == TransactionType.EXPENSE,
            expenseLabel = stringResource(Res.string.edit_type_expense),
            incomeLabel = stringResource(Res.string.edit_type_income),
            onExpenseSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.EXPENSE)) },
            onIncomeSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
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
        Spacer(Modifier.height(12.dp))
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
        Spacer(Modifier.height(24.dp))
        CategoryPicker(
            categories = state.availableCategories,
            selectedCategoryId = state.selectedCategoryId,
            categoryError = state.categoryError,
            onCategorySelected = { onIntent(TransactionEditIntent.CategorySelected(it)) },
        )
    }
}

// ─── Note suggestions row ─────────────────────────────────────────────────────

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

// ─── Date display field (clickable, non-editable) ─────────────────────────────

@Composable
private fun DateDisplayField(
    dateText: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.radius

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = type.micro,
            color = colors.text2,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(radius.md)
                .background(colors.surface, radius.md)
                .border(1.dp, colors.border, radius.md)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = dateText,
                style = type.body,
                color = colors.text,
            )
        }
    }
}

// ─── Modal header ─────────────────────────────────────────────────────────────

@Composable
private fun TransactionEditModalHeader(
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MmIconButton(
            icon = MmIcons.close,
            onClick = onDismiss,
        )
        Text(
            text = if (isEditMode) stringResource(Res.string.edit_title_edit) else stringResource(Res.string.edit_title_add),
            style = type.title3,
            color = colors.text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        if (isEditMode) {
            MmIconButton(
                icon = MmIcons.trash,
                onClick = onDeleteClick,
                variant = MmIconButtonVariant.Danger,
            )
        } else {
            Spacer(Modifier.size(40.dp))
        }
    }
}

// ─── Amount display ───────────────────────────────────────────────────────────

@Composable
private fun AmountDisplay(
    amountValue: Double,
    formattedAmount: String,
    currencyCode: String,
    amountText: String,
    focusRequester: FocusRequester,
    onAmountChanged: (String) -> Unit,
    onCalculatorClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.edit_amount_label).uppercase(),
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

        // Hidden BasicTextField for numeric input (cursor brush set to accent)
        BasicTextField(
            value = amountText,
            onValueChange = onAmountChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            decorationBox = { },
        )

        Spacer(Modifier.height(8.dp))

        // Calculator button
        MmIconButton(
            icon = MmIcons.calculator,
            onClick = onCalculatorClick,
            contentDescription = stringResource(Res.string.edit_calculator_title),
        )
    }
}

// ─── Category picker ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: com.dv.moneym.core.model.CategoryId?,
    categoryError: Boolean,
    onCategorySelected: (com.dv.moneym.core.model.CategoryId) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Text(
        text = stringResource(Res.string.edit_category_label).uppercase(),
        style = type.micro,
        color = if (categoryError) colors.danger else colors.text3,
    )
    Spacer(Modifier.height(12.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { cat ->
            CategoryChip(
                category = cat,
                isSelected = cat.id == selectedCategoryId,
                onClick = { onCategorySelected(cat.id) },
            )
        }
    }

    // Category error message
    if (categoryError) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.edit_category_error),
            style = type.caption,
            color = colors.danger,
        )
    }
}

// ─── Save bar ─────────────────────────────────────────────────────────────────

@Composable
private fun TransactionEditSaveBar(
    isEditMode: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    val colors = MM.colors
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
            text = if (isEditMode) stringResource(Res.string.edit_save_changes) else stringResource(Res.string.edit_add_transaction),
            onClick = onSave,
            variant = MmButtonVariant.Accent,
            size = MmButtonSize.Lg,
            leadingIcon = MmIcons.check,
            fullWidth = true,
            enabled = !isSaving,
        )
    }
}

// ─── Date picker dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    state: TransactionEditUiState,
    todayDate: LocalDate,
    yesterdayDate: LocalDate,
    onIntent: (TransactionEditIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val initialMillis = state.date
        ?.atStartOfDayIn(TimeZone.UTC)
        ?.toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
    )

    // Apply design system colors to the date picker
    val themedColors = DatePickerDefaults.colors(
        containerColor = colors.bg,
        titleContentColor = colors.text,
        headlineContentColor = colors.text,
        weekdayContentColor = colors.text2,
        subheadContentColor = colors.text2,
        yearContentColor = colors.text,
        currentYearContentColor = colors.accent,
        selectedYearContentColor = colors.bg,
        selectedYearContainerColor = colors.accent,
        selectedDayContentColor = colors.bg,
        selectedDayContainerColor = colors.accent,
        todayContentColor = colors.accent,
        todayDateBorderColor = colors.accent,
        dayContentColor = colors.text,
        navigationContentColor = colors.text,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Yesterday quick button
                TextButton(onClick = {
                    onIntent(TransactionEditIntent.DateChanged(yesterdayDate))
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.edit_date_yesterday), color = colors.text2)
                }
                // Today quick button
                TextButton(onClick = {
                    onIntent(TransactionEditIntent.DateChanged(todayDate))
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.edit_date_today), color = colors.accent)
                }
                // OK button
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onIntent(TransactionEditIntent.DateChanged(localDate))
                    }
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.edit_date_ok), color = colors.accent)
                }
            }
        },
        colors = themedColors,
    ) {
        DatePicker(state = datePickerState, colors = themedColors)
    }
}

// ─── Type Toggle Bar ──────────────────────────────────────────────────────────

@Composable
private fun TypeToggleBar(
    isExpense: Boolean,
    expenseLabel: String,
    incomeLabel: String,
    onExpenseSelected: () -> Unit,
    onIncomeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.radius

    val expenseActiveColor = colors.danger
    val incomeActiveColor = colors.accent
    val inactiveBg = colors.surface2
    val inactiveFg = colors.text2

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(radius.xl)
            .background(inactiveBg),
    ) {
        // Expense tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(radius.xl)
                .background(if (isExpense) expenseActiveColor else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpenseSelected() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = expenseLabel,
                style = type.caption.copy(
                    fontWeight = if (isExpense) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isExpense) Color.White else inactiveFg,
                ),
            )
        }
        // Income tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(radius.xl)
                .background(if (!isExpense) incomeActiveColor else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onIncomeSelected() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = incomeLabel,
                style = type.caption.copy(
                    fontWeight = if (!isExpense) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (!isExpense) Color.White else inactiveFg,
                ),
            )
        }
    }
}

// ─── Date formatting ─────────────────────────────────────────────────────────

private fun LocalDate.toFriendlyString(today: LocalDate, yesterday: LocalDate, todayLabel: String, yesterdayLabel: String): String = when (this) {
    today -> todayLabel
    yesterday -> yesterdayLabel
    else -> {
        val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val yearSuffix = if (year != today.year) " $year" else ""
        "$dayName, $monthName $dayOfMonth$yearSuffix"
    }
}

// ─── Delete sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDeleteSheet(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Grab handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            Text(
                text = stringResource(Res.string.edit_delete_confirm_title),
                style = MM.type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.edit_delete_confirm_body),
                style = MM.type.caption,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MmButton(
                    text = stringResource(Res.string.edit_delete_confirm_cancel),
                    onClick = onCancel,
                    variant = MmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = stringResource(Res.string.edit_delete_confirm_ok),
                    onClick = onConfirm,
                    variant = MmButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Category chip ────────────────────────────────────────────────────────────

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

// ─── Calculator Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorBottomSheet(
    initialAmountText: String,
    onDismiss: () -> Unit,
    onAmountSaved: (String) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calculator state
    var display by remember {
        mutableStateOf(
            if (initialAmountText.isNotBlank() && initialAmountText.toDoubleOrNull() != null &&
                initialAmountText.toDouble() > 0) {
                initialAmountText
            } else {
                ""
            }
        )
    }
    var currentValue by remember { mutableStateOf(0.0) }
    var pendingOp by remember { mutableStateOf<Char?>(null) }
    var justAppliedOp by remember { mutableStateOf(false) }

    // Initialize from current amount
    LaunchedEffect(Unit) {
        val initial = initialAmountText.toDoubleOrNull() ?: 0.0
        if (initial > 0) {
            currentValue = initial
        }
    }

    fun calcResult(): Double {
        val inputVal = display.toDoubleOrNull() ?: 0.0
        return when (pendingOp) {
            '+' -> currentValue + inputVal
            '-' -> currentValue - inputVal
            '*' -> currentValue * inputVal
            '/' -> if (inputVal != 0.0) currentValue / inputVal else currentValue
            else -> inputVal
        }
    }

    fun formatResult(v: Double): String {
        val major = v.toLong()
        val frac = ((v - major) * 100).toLong().coerceAtLeast(0L)
        return "$major.${frac.toString().padStart(2, '0')}"
    }

    fun onCalcKey(key: String) {
        when (key) {
            "C" -> {
                display = ""
                currentValue = 0.0
                pendingOp = null
                justAppliedOp = false
            }
            "⌫" -> {
                if (display.isNotEmpty()) display = display.dropLast(1)
            }
            "+", "-", "*", "/" -> {
                val result = calcResult()
                currentValue = result
                pendingOp = key[0]
                display = ""
                justAppliedOp = true
            }
            "=" -> {
                val result = calcResult()
                display = formatResult(result)
                currentValue = result
                pendingOp = null
                justAppliedOp = false
            }
            "." -> {
                if (!display.contains('.')) display += "."
            }
            else -> {
                if (justAppliedOp) {
                    display = key
                    justAppliedOp = false
                } else {
                    if (display.length < 12) display += key
                }
            }
        }
    }

    val resultPreview = if (pendingOp != null && display.isNotEmpty()) {
        val result = calcResult()
        formatResult(result)
    } else {
        ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Grab handle
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            // Title
            Text(
                text = stringResource(Res.string.edit_calculator_title),
                style = type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (pendingOp != null) {
                    Text(
                        text = "${formatResult(currentValue)} $pendingOp${if (display.isNotEmpty()) " $display" else ""}",
                        style = type.caption.copy(color = colors.text2),
                    )
                }
                Text(
                    text = display.ifEmpty { "0" },
                    style = type.display.copy(fontSize = 36.sp, color = colors.text),
                )
                if (resultPreview.isNotEmpty()) {
                    Text(
                        text = "= $resultPreview",
                        style = type.caption.copy(color = colors.accent),
                    )
                }
            }

            // Calculator buttons
            val rows = listOf(
                listOf("7", "8", "9", "/"),
                listOf("4", "5", "6", "*"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", ".", "+"),
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("/", "*", "-", "+")
                        val isClear = key == "C"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isClear -> colors.danger.copy(alpha = 0.15f)
                                        isOp -> colors.accent.copy(alpha = 0.15f)
                                        else -> colors.surface
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isClear -> colors.danger.copy(alpha = 0.3f)
                                        isOp -> colors.accent.copy(alpha = 0.3f)
                                        else -> colors.border
                                    },
                                    RoundedCornerShape(12.dp),
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures { onCalcKey(key) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = key,
                                style = type.title3.copy(
                                    color = when {
                                        isClear -> colors.danger
                                        isOp -> colors.accent
                                        else -> colors.text
                                    }
                                ),
                            )
                        }
                    }
                }
            }

            // Bottom row: backspace + save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Backspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { onCalcKey("⌫") }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⌫", style = type.title3.copy(color = colors.text))
                }
                // Equals / result preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.15f))
                        .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { onCalcKey("=") }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("=", style = type.title3.copy(color = colors.accent))
                }
                // Save / apply
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                val result = calcResult()
                                val finalDisplay = if (display.isEmpty()) {
                                    formatResult(currentValue)
                                } else {
                                    formatResult(result)
                                }
                                onAmountSaved(finalDisplay)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", style = type.title3.copy(color = Color.White))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
