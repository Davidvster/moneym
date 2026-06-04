package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteAllTransactionsConfirmSheet(
    title: String,
    warning: String,
    inputLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    categoryName: String,
    input: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val matches = input.trim().equals(categoryName, ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_3x,
                ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            Text(text = title, style = MM.type.title3, color = colors.text)
            Text(text = warning, style = MM.type.caption, color = colors.danger)
            MmField(
                value = input,
                onValueChange = onInputChange,
                label = inputLabel,
                placeholder = categoryName,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
            ) {
                MmButton(
                    text = cancelLabel,
                    onClick = onCancel,
                    variant = MmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    variant = MmButtonVariant.Danger,
                    enabled = matches,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
