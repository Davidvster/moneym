package com.dv.moneym.feature.transactionedit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIcons
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_add_transaction
import moneym.feature.transactionedit.generated.resources.edit_save_changes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionEditSaveBar(
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
            .padding(
                start = MM.dimen.padding_2x,
                end = MM.dimen.padding_2x,
                top = MM.dimen.padding_1_5x,
                bottom = MM.dimen.padding_2x
            ),
    ) {
        MmButton(
            text = if (isEditMode) stringResource(Res.string.edit_save_changes) else stringResource(
                Res.string.edit_add_transaction
            ),
            onClick = onSave,
            variant = MmButtonVariant.Accent,
            size = MmButtonSize.Lg,
            leadingIcon = MmIcons.check,
            fullWidth = true,
            enabled = !isSaving,
        )
    }
}
