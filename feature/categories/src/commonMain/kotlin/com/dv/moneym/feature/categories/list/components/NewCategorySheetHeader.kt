package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NewCategorySheetHeader(
    sheetTitle: String,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors

    // Grabber
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

    // Header row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MM.dimen.padding_1_5x, vertical = MM.dimen.padding_1x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MmIconButton(
            icon = MmIcons.close,
            onClick = onDismiss,
            contentDescription = stringResource(Res.string.categories_cancel),
        )
        Text(
            text = sheetTitle,
            style = MM.type.title3,
            color = colors.text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(MM.dimen.padding_5x))
    }
}
