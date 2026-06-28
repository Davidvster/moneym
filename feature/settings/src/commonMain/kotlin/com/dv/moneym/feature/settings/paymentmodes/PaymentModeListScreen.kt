package com.dv.moneym.feature.settings.paymentmodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmDeleteSheet
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSheetHeader
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = space.padding_2_5x,
                vertical = space.padding_2x,
            ),
        ) {
            item {
                MmCard(padded = false, shape = space.radius_1_5x) {
                    Column {
                        state.modes.forEachIndexed { idx, mode ->
                            MmRow(
                                onClick = { onRenameClick(mode) },
                                divider = idx < state.modes.lastIndex,
                            ) {
                                Text(
                                    text = mode.name,
                                    style = type.body,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f),
                                )
                                MmIconButton(
                                    icon = Icon.Trash.imageVector,
                                    onClick = { onDeleteClick(mode) },
                                    variant = MmIconButtonVariant.Danger,
                                    contentDescription = stringResource(Res.string.settings_payment_mode_delete_title),
                                )
                            }
                        }
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
            NameInputSheet(
                title = stringResource(Res.string.settings_payment_mode_add),
                initialName = "",
                placeholder = stringResource(Res.string.settings_payment_mode_name_placeholder),
                confirmText = stringResource(Res.string.settings_payment_mode_save),
                onDismiss = onDismissDialog,
                onConfirm = onConfirmCreate,
            )
        }

        is PaymentModeDialogState.Rename -> {
            NameInputSheet(
                title = stringResource(Res.string.settings_payment_mode_rename_title),
                initialName = dialog.currentName,
                placeholder = stringResource(Res.string.settings_payment_mode_name_placeholder),
                confirmText = stringResource(Res.string.settings_payment_mode_save),
                onDismiss = onDismissDialog,
                onConfirm = { name -> onConfirmRename(dialog.id, name) },
            )
        }

        is PaymentModeDialogState.DeleteConfirm -> {
            MmDeleteSheet(
                title = stringResource(Res.string.settings_payment_mode_delete_title),
                body = stringResource(Res.string.settings_payment_mode_delete_message, dialog.name),
                cancelText = stringResource(Res.string.settings_payment_mode_cancel),
                confirmText = stringResource(Res.string.settings_payment_mode_delete_confirm),
                onConfirm = { onConfirmDelete(dialog.id) },
                onCancel = onDismissDialog,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameInputSheet(
    title: String,
    initialName: String,
    placeholder: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = MM.colors
    var name by remember(initialName) { mutableStateOf(initialName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_3x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 36.dp, height = MM.dimen.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            MmSheetHeader(title = title, onClose = onDismiss)
            MmField(
                value = name,
                onValueChange = { name = it },
                placeholder = placeholder,
            )
            MmButton(
                text = confirmText,
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
            Spacer(Modifier.height(MM.dimen.padding_1x))
        }
    }
}
