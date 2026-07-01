package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.imageVector
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_lock_1m
import moneym.feature.settings.generated.resources.settings_lock_30s
import moneym.feature.settings.generated.resources.settings_lock_5m
import moneym.feature.settings.generated.resources.settings_lock_after_title
import moneym.feature.settings.generated.resources.settings_lock_immediately
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = space.padding_2_5x, topEnd = space.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_3x),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .padding(bottom = space.padding_0_5x)
                        .size(width = 36.dp, height = space.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            MmSheetHeader(
                onClose = onDismiss,
                title = stringResource(Res.string.settings_lock_after_title),
            )

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                options.forEach { (seconds, label) ->
                    MmRow(
                        onClick = { onConfirm(seconds) },
                        divider = false,
                        padding = PaddingValues(
                            horizontal = space.padding_0_5x,
                            vertical = space.padding_1x,
                        ),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = label,
                                style = type.body,
                                color = colors.text,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                            if (seconds == currentSeconds) {
                                MaterialIcon(
                                    imageVector = Icon.Check.imageVector,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(space.iconMd),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LockTimeoutPickerDialogPreviewLight() {
    MoneyMTheme(darkTheme = false) {
        LockTimeoutPickerDialog(
            currentSeconds = 60,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun LockTimeoutPickerDialogPreviewDark() {
    MoneyMTheme(darkTheme = true) {
        LockTimeoutPickerDialog(
            currentSeconds = 60,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
