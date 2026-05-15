package com.dv.moneym.feature.transactionedit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditEffect
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditUiState
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditViewModel
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.navigation.ModalKey
import kotlinx.serialization.Serializable
import org.koin.core.parameter.parametersOf

@Serializable data class TransactionEditKey(
    val id: Long? = null,
    val sessionKey: String = kotlin.random.Random.nextLong().toString(),
) : ModalKey

fun EntryProviderScope<NavKey>.transactionEditEntry(
    onDismiss: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<TransactionEditKey>(metadata = metadata) { key ->
    TransactionEditScreen(
        transactionId = key.id?.let { com.dv.moneym.core.model.TransactionId(it) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditContent(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sp = MoneyMTheme.spacing

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onIntent(TransactionEditIntent.DeleteCancelled) },
            title = { Text(stringResource(Res.string.edit_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.edit_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { onIntent(TransactionEditIntent.DeleteConfirmed) }) {
                    Text(stringResource(Res.string.edit_delete_confirm_ok), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(TransactionEditIntent.DeleteCancelled) }) {
                    Text(stringResource(Res.string.edit_delete_confirm_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditMode) Res.string.edit_title_edit else Res.string.edit_title_add)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(MoneyMIcons.Clear, contentDescription = stringResource(Res.string.edit_close))
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { onIntent(TransactionEditIntent.DeleteRequested) }) {
                            Icon(MoneyMIcons.Delete, contentDescription = stringResource(Res.string.edit_delete),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(
                        onClick = { onIntent(TransactionEditIntent.SaveRequested) },
                        enabled = !state.isSaving,
                    ) {
                        Icon(MoneyMIcons.Check, contentDescription = stringResource(Res.string.edit_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sp.lg),
        ) {
            Spacer(Modifier.height(sp.md))
            Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.EXPENSE)) },
                    label = { Text(stringResource(Res.string.edit_type_expense)) },
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME)) },
                    label = { Text(stringResource(Res.string.edit_type_income)) },
                )
            }
            Spacer(Modifier.height(sp.md))
            OutlinedTextField(
                value = state.amountText,
                onValueChange = { onIntent(TransactionEditIntent.AmountChanged(it)) },
                label = { Text(stringResource(Res.string.edit_amount_label)) },
                isError = state.amountError,
                supportingText = if (state.amountError) ({ Text(stringResource(Res.string.edit_amount_error)) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(sp.md))
            val dateText = state.date?.toString() ?: ""
            OutlinedTextField(
                value = dateText,
                onValueChange = { text ->
                    runCatching { kotlinx.datetime.LocalDate.parse(text) }.getOrNull()
                        ?.let { onIntent(TransactionEditIntent.DateChanged(it)) }
                },
                label = { Text(stringResource(Res.string.edit_date_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(sp.md))
            OutlinedTextField(
                value = state.note,
                onValueChange = { onIntent(TransactionEditIntent.NoteChanged(it)) },
                label = { Text(stringResource(Res.string.edit_note_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(sp.md))
            if (state.categoryError) {
                Text(
                    stringResource(Res.string.edit_category_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            } else {
                Text(
                    stringResource(Res.string.edit_category_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(sp.xs))
            CategoryGrid(
                categories = state.availableCategories,
                selected = state.selectedCategoryId,
                onSelect = { onIntent(TransactionEditIntent.CategorySelected(it)) },
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selected: CategoryId?,
    onSelect: (CategoryId) -> Unit,
) {
    val sp = MoneyMTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(sp.sm),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        items(categories, key = { it.id.value }) { cat ->
            FilterChip(
                selected = cat.id == selected,
                onClick = { onSelect(cat.id) },
                label = {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (cat.id == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                leadingIcon = {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(end = sp.xs)
                            .then(
                                Modifier
                                    .height(8.dp)
                                    .padding(0.dp)
                            )
                    ) {
                        // category colour dot
                    }
                },
            )
        }
    }
}
