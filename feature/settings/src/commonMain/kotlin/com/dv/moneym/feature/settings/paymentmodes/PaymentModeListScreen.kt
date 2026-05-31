package com.dv.moneym.feature.settings.paymentmodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.settings.overview.PaymentModeListKey
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_payment_mode_add
import moneym.feature.settings.generated.resources.settings_payment_mode_cancel
import moneym.feature.settings.generated.resources.settings_payment_mode_delete_confirm
import moneym.feature.settings.generated.resources.settings_payment_mode_delete_message
import moneym.feature.settings.generated.resources.settings_payment_mode_delete_title
import moneym.feature.settings.generated.resources.settings_payment_mode_list_title
import moneym.feature.settings.generated.resources.settings_payment_mode_name_placeholder
import moneym.feature.settings.generated.resources.settings_payment_mode_rename_title
import moneym.feature.settings.generated.resources.settings_payment_mode_save
import androidx.compose.material3.TextFieldDefaults
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.paymentModeListEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<PaymentModeListKey>(metadata = metadata) {
    PaymentModeListScreen(onBack = onBack)
}

@Composable
internal fun PaymentModeListScreen(
    onBack: () -> Unit,
    viewModel: PaymentModeListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PaymentModeListContent(
        state = state,
        onBack = onBack,
        onAddClick = { viewModel.onIntent(PaymentModeListIntent.ShowAdd) },
        onRenameClick = { mode -> viewModel.onIntent(PaymentModeListIntent.ShowRename(mode.id, mode.name)) },
        onDeleteClick = { mode -> viewModel.onIntent(PaymentModeListIntent.ShowDelete(mode.id, mode.name)) },
        onDismissDialog = { viewModel.onIntent(PaymentModeListIntent.Dismiss) },
        onConfirmCreate = { name -> viewModel.onIntent(PaymentModeListIntent.Create(name)) },
        onConfirmRename = { id, name -> viewModel.onIntent(PaymentModeListIntent.Rename(id, name)) },
        onConfirmDelete = { id -> viewModel.onIntent(PaymentModeListIntent.Delete(id)) },
    )
}

@Composable
private fun PaymentModeListContent(
    state: PaymentModeListUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onRenameClick: (PaymentMode) -> Unit,
    onDeleteClick: (PaymentMode) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmCreate: (String) -> Unit,
    onConfirmRename: (PaymentModeId, String) -> Unit,
    onConfirmDelete: (PaymentModeId) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_payment_mode_list_title),
            onBack = onBack,
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.modes, key = { it.id.value }) { mode ->
                MmRow(onClick = { onRenameClick(mode) }) {
                    Text(
                        text = mode.name,
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onDeleteClick(mode) }) {
                        Text(
                            text = "Delete",
                            style = type.caption,
                            color = colors.danger,
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .padding(horizontal = space.padding_2_5x, vertical = space.padding_2x)
                .navigationBarsPadding()
        ) {
            MmButton(
                text = stringResource(Res.string.settings_payment_mode_add),
                onClick = onAddClick,
                variant = MmButtonVariant.Primary,
                fullWidth = true,
                leadingIcon = Icon.Plus.imageVector,
            )
        }
    }

    when (val dialog = state.dialogState) {
        is PaymentModeDialogState.Add -> {
            NameInputDialog(
                title = stringResource(Res.string.settings_payment_mode_add),
                initialName = "",
                placeholder = stringResource(Res.string.settings_payment_mode_name_placeholder),
                onDismiss = onDismissDialog,
                onConfirm = onConfirmCreate,
            )
        }

        is PaymentModeDialogState.Rename -> {
            NameInputDialog(
                title = stringResource(Res.string.settings_payment_mode_rename_title),
                initialName = dialog.currentName,
                placeholder = stringResource(Res.string.settings_payment_mode_name_placeholder),
                onDismiss = onDismissDialog,
                onConfirm = { name -> onConfirmRename(dialog.id, name) },
            )
        }

        is PaymentModeDialogState.DeleteConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                containerColor = MM.colors.bg,
                titleContentColor = MM.colors.text,
                textContentColor = MM.colors.text,
                title = {
                    Text(stringResource(Res.string.settings_payment_mode_delete_title))
                },
                text = {
                    Text(
                        stringResource(Res.string.settings_payment_mode_delete_message, dialog.name)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onConfirmDelete(dialog.id) }) {
                        Text(
                            stringResource(Res.string.settings_payment_mode_delete_confirm),
                            color = MM.colors.danger,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) {
                        Text(stringResource(Res.string.settings_payment_mode_cancel), color = MM.colors.text2)
                    }
                },
            )
        }

        PaymentModeDialogState.None -> Unit
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PaymentModeListContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        PaymentModeListContent(
            state = PaymentModeListUiState(modes = emptyList(), isLoading = false),
            onBack = {},
            onAddClick = {},
            onRenameClick = {},
            onDeleteClick = {},
            onDismissDialog = {},
            onConfirmCreate = {},
            onConfirmRename = { _, _ -> },
            onConfirmDelete = {},
        )
    }
}

@Composable
private fun NameInputDialog(
    title: String,
    initialName: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MM.colors.bg,
        titleContentColor = MM.colors.text,
        textContentColor = MM.colors.text,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MM.colors.surface,
                    unfocusedContainerColor = MM.colors.surface,
                    focusedTextColor = MM.colors.text,
                    unfocusedTextColor = MM.colors.text,
                    focusedIndicatorColor = MM.colors.accent,
                    unfocusedIndicatorColor = MM.colors.border,
                    cursorColor = MM.colors.accent,
                    focusedPlaceholderColor = MM.colors.text3,
                    unfocusedPlaceholderColor = MM.colors.text3,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
            ) {
                Text(stringResource(Res.string.settings_payment_mode_save), color = MM.colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_payment_mode_cancel), color = MM.colors.text2)
            }
        },
    )
}
