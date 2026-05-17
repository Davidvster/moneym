package com.dv.moneym.feature.categories.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMColors
import com.dv.moneym.feature.categories.ui.resolveIconVector

@Composable
internal fun CategoryPreviewChip(
    name: String,
    selectedColor: Color,
    selectedIconKey: String,
    placeholderText: String,
    colors: MoneyMColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(MM.dimen.pill)
                .background(colors.surface)
                .border(1.dp, colors.border, MM.dimen.pill)
                .padding(horizontal = MM.dimen.padding_2x, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(MM.dimen.padding_1x))
                    .background(selectedColor),
                contentAlignment = Alignment.Center,
            ) {
                val painter = rememberVectorPainter(resolveIconVector(selectedIconKey))
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
            Text(
                text = name.ifBlank { placeholderText },
                style = MM.type.body,
                color = if (name.isBlank()) colors.text3 else colors.text,
            )
        }
    }
}
