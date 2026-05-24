package com.dv.moneym.feature.transactionedit.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_title_add
import moneym.feature.transactionedit.generated.resources.edit_title_edit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionEditModalHeader(
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
            .padding(horizontal = MM.dimen.padding_1_5x, vertical = MM.dimen.padding_0_5x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MmIconButton(
            icon = Icon.Close.imageVector,
            onClick = onDismiss,
        )
        Text(
            text = if (isEditMode) stringResource(Res.string.edit_title_edit) else stringResource(
                Res.string.edit_title_add
            ),
            style = type.title3,
            color = colors.text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        if (isEditMode) {
            MmIconButton(
                icon = Icon.Trash.imageVector,
                onClick = onDeleteClick,
                variant = MmIconButtonVariant.Danger,
            )
        } else {
            Spacer(Modifier.size(MM.dimen.padding_5x))
        }
    }
}
