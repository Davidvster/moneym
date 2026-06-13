package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmSheetHeader

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
                .size(width = 36.dp, height = MM.dimen.padding_0_5x)
                .clip(RoundedCornerShape(50))
                .background(colors.borderStrong),
        )
    }

    MmSheetHeader(
        title = sheetTitle,
        onClose = onDismiss,
        modifier = Modifier.padding(horizontal = MM.dimen.padding_1_5x, vertical = MM.dimen.padding_1x),
    )
}
