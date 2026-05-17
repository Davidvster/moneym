package com.dv.moneym.feature.categories.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMColors
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIcons
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_create
import moneym.feature.categories.generated.resources.categories_save_changes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NewCategorySaveButton(
    isEditMode: Boolean,
    nameIsBlank: Boolean,
    colors: MoneyMColors,
    onSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
    ) {
        MmButton(
            text = if (isEditMode) stringResource(Res.string.categories_save_changes) else stringResource(
                Res.string.categories_create
            ),
            onClick = onSave,
            variant = MmButtonVariant.Accent,
            size = MmButtonSize.Lg,
            leadingIcon = MmIcons.check,
            fullWidth = true,
            enabled = !nameIsBlank,
        )
    }
}
