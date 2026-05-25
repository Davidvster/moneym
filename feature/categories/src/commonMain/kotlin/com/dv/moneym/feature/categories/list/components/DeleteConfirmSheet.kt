package com.dv.moneym.feature.categories.list.components

import androidx.compose.runtime.Composable
import com.dv.moneym.core.ui.MmDeleteSheet
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_cancel
import moneym.feature.categories.generated.resources.categories_delete_button
import moneym.feature.categories.generated.resources.categories_delete_confirm_body
import moneym.feature.categories.generated.resources.categories_delete_confirm_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeleteConfirmSheet(
    categoryName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    MmDeleteSheet(
        title = stringResource(Res.string.categories_delete_confirm_title, categoryName),
        body = stringResource(Res.string.categories_delete_confirm_body),
        cancelText = stringResource(Res.string.categories_cancel),
        confirmText = stringResource(Res.string.categories_delete_button),
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}
