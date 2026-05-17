package com.dv.moneym.feature.transactionedit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_body
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_cancel
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_ok
import moneym.feature.transactionedit.generated.resources.edit_delete_confirm_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDeleteSheet(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_3x
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Grab handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            Text(
                text = stringResource(Res.string.edit_delete_confirm_title),
                style = MM.type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.edit_delete_confirm_body),
                style = MM.type.caption,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
            ) {
                MmButton(
                    text = stringResource(Res.string.edit_delete_confirm_cancel),
                    onClick = onCancel,
                    variant = MmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = stringResource(Res.string.edit_delete_confirm_ok),
                    onClick = onConfirm,
                    variant = MmButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(MM.dimen.padding_1x))
        }
    }
}
