package com.dv.moneym.feature.transactionedit.components

import androidx.compose.runtime.Composable
import com.dv.moneym.core.ui.MmDeleteSheet
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_body
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_cancel
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_ok
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionDeleteSheet(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    MmDeleteSheet(
        title = stringResource(Res.string.edit_delete_confirm_title),
        body = stringResource(Res.string.edit_delete_confirm_body),
        cancelText = stringResource(Res.string.edit_delete_confirm_cancel),
        confirmText = stringResource(Res.string.edit_delete_confirm_ok),
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}
