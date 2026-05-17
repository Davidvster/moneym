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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditEffect
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditUiState
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditViewModel
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_add_transaction
import moneym.feature.transactionedit.generated.resources.edit_amount_label
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

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
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
    val dateText = state.date?.toFriendlyString(todayDate, yesterdayDate, todayLabel, yesterdayLabel) ?: ""

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 100.dp),
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
        )
        MmField(
            value = dateText,
            onValueChange = {},
            label = stringResource(Res.string.edit_date_label),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDatePickerOpen() },
        )
        Spacer(Modifier.height(12.dp))
        MmField(
            value = state.note,
            onValueChange = { onIntent(TransactionEditIntent.NoteChanged(it)) },
            label = stringResource(Res.string.edit_note_label),
            placeholder = stringResource(Res.string.edit_note_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        CategoryPicker(
            categories = state.availableCategories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { onIntent(TransactionEditIntent.CategorySelected(it)) },
        )
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

        // Hidden BasicTextField for numeric input
        BasicTextField(
            value = amountText,
            onValueChange = onAmountChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            decorationBox = { },
        )
    }
}

// ─── Category picker ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: com.dv.moneym.core.model.CategoryId?,
    onCategorySelected: (com.dv.moneym.core.model.CategoryId) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Text(
        text = stringResource(Res.string.edit_category_label).uppercase(),
        style = type.micro,
        color = colors.text3,
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
    ) {
        DatePicker(state = datePickerState)
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
