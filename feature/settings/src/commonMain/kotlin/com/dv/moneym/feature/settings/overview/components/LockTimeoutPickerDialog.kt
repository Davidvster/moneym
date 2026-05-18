package com.dv.moneym.feature.settings.overview.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmRow
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_cancel
import moneym.feature.settings.generated.resources.settings_lock_1m
import moneym.feature.settings.generated.resources.settings_lock_30s
import moneym.feature.settings.generated.resources.settings_lock_5m
import moneym.feature.settings.generated.resources.settings_lock_after_title
import moneym.feature.settings.generated.resources.settings_lock_immediately
import moneym.feature.settings.generated.resources.settings_ok
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LockTimeoutPickerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    val options = listOf(
        0 to stringResource(Res.string.settings_lock_immediately),
        30 to stringResource(Res.string.settings_lock_30s),
        60 to stringResource(Res.string.settings_lock_1m),
        300 to stringResource(Res.string.settings_lock_5m),
    )
    var selectedSeconds by remember { mutableStateOf(currentSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.settings_lock_after_title),
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                options.forEach { (seconds, label) ->
                    val isSelected = seconds == selectedSeconds
                    MmRow(
                        onClick = { selectedSeconds = seconds },
                        divider = false,
                        padding = PaddingValues(
                            horizontal = space.padding_0_5x,
                            vertical = space.padding_0_25x
                        ),
                    ) {
                        Text(
                            text = label,
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) colors.accent else colors.borderStrong,
                                    CircleShape,
                                )
                                .background(if (isSelected) colors.accent else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icon.Check.imageVector,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(MM.dimen.padding_1_5x),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSeconds) }) {
                Text(stringResource(Res.string.settings_ok), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}
