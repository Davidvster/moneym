package com.dv.moneym.feature.transactionedit.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmLoadingSpinner
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_add_transaction
import moneym.feature.transactionedit.generated.resources.edit_save_as_new
import moneym.feature.transactionedit.generated.resources.edit_save_changes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionEditSaveBar(
    isEditMode: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onSaveAsNew: (() -> Unit)? = null,
    saveLabel: String? = null,
) {
    val colors = MM.colors
    val dividerColor = colors.divider
    val haptic = LocalHapticFeedback.current
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
            )
            .navigationBarsPadding(),
    ) {
        if (isEditMode && onSaveAsNew != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
                MmButton(
                    text = stringResource(Res.string.edit_save_as_new),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSaveAsNew()
                    },
                    variant = MmButtonVariant.Secondary,
                    size = MmButtonSize.Lg,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = saveLabel ?: stringResource(Res.string.edit_save_changes),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSave()
                    },
                    variant = MmButtonVariant.Accent,
                    size = MmButtonSize.Lg,
                    leadingIcon = Icon.Check.imageVector,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
                if (isSaving) {
                    Spacer(Modifier.width(MM.dimen.padding_1x))
                    MmLoadingSpinner()
                }
            }
        } else {
            MmButton(
                text = saveLabel ?: stringResource(Res.string.edit_add_transaction),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSave()
                },
                variant = MmButtonVariant.Accent,
                size = MmButtonSize.Lg,
                leadingIcon = Icon.Check.imageVector,
                fullWidth = true,
                enabled = !isSaving,
            )
        }
    }
}

@Preview
@Composable
private fun TransactionEditSaveBarPreview() {
    MoneyMTheme {
        TransactionEditSaveBar(
            isEditMode = false,
            isSaving = false,
            onSave = {},
        )
    }
}
