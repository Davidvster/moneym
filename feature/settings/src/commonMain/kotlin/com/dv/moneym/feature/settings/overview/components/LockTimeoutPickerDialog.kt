package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmRadio
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSheetHeader
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_cancel
import moneym.feature.settings.generated.resources.settings_lock_1m
import moneym.feature.settings.generated.resources.settings_lock_30s
import moneym.feature.settings.generated.resources.settings_lock_5m
import moneym.feature.settings.generated.resources.settings_lock_after_title
import moneym.feature.settings.generated.resources.settings_lock_immediately
import moneym.feature.settings.generated.resources.settings_ok
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LockTimeoutPickerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val options = listOf(
        0 to stringResource(Res.string.settings_lock_immediately),
        30 to stringResource(Res.string.settings_lock_30s),
        60 to stringResource(Res.string.settings_lock_1m),
        300 to stringResource(Res.string.settings_lock_5m),
    )
    var selectedSeconds by remember(currentSeconds) { mutableStateOf(currentSeconds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.text3) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_2x),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            MmSheetHeader(
                onClose = onDismiss,
                title = stringResource(Res.string.settings_lock_after_title),
            )

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                options.forEach { (seconds, label) ->
                    val isSelected = seconds == selectedSeconds
                    MmRow(
                        onClick = { selectedSeconds = seconds },
                        divider = false,
                        padding = PaddingValues(
                            horizontal = space.padding_0_5x,
                            vertical = space.padding_0_25x,
                        ),
                    ) {
                        Text(
                            text = label,
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        MmRadio(selected = isSelected)
                    }
                }
            }

            MmButton(
                text = stringResource(Res.string.settings_ok),
                onClick = { onConfirm(selectedSeconds) },
                fullWidth = true,
            )
            MmButton(
                text = stringResource(Res.string.settings_cancel),
                onClick = onDismiss,
                variant = MmButtonVariant.Outline,
                fullWidth = true,
            )
        }
    }
}
