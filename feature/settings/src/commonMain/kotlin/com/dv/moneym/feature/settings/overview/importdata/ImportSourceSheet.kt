package com.dv.moneym.feature.settings.overview.importdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_import_from
import moneym.feature.settings.generated.resources.settings_import_source_ehf
import moneym.feature.settings.generated.resources.settings_import_source_moneym
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSourceSheet(
    onMoneyMSelected: () -> Unit,
    onEasyHomeFinanceSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_3x,
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
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
                text = stringResource(Res.string.settings_import_from),
                style = type.title3,
                color = colors.text,
            )

            MmButton(
                text = stringResource(Res.string.settings_import_source_moneym),
                onClick = onMoneyMSelected,
                variant = MmButtonVariant.Secondary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )

            MmButton(
                text = stringResource(Res.string.settings_import_source_ehf),
                onClick = onEasyHomeFinanceSelected,
                variant = MmButtonVariant.Secondary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )

            Spacer(Modifier.height(MM.dimen.padding_1x))
        }
    }
}
