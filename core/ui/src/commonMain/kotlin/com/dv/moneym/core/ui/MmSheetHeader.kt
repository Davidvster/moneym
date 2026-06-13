package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import moneym.core.ui.generated.resources.Res
import moneym.core.ui.generated.resources.mm_close
import org.jetbrains.compose.resources.stringResource

@Composable
fun MmSheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (title != null) {
            Text(text = title, style = MM.type.title3, color = MM.colors.text, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        MmIconButton(
            icon = Icon.Close.imageVector,
            onClick = onClose,
            contentDescription = stringResource(Res.string.mm_close),
        )
    }
}
