package com.dv.moneym.feature.categories.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconPickerSection(
    iconOptions: List<String>,
    selectedIconKey: String,
    selectedColor: Color,
    onIconSelected: (String) -> Unit,
    colors: MoneyMColors,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        iconOptions.forEach { iconKey ->
            val isSelected = selectedIconKey == iconKey
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) selectedColor else colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) selectedColor else colors.border,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onIconSelected(iconKey) },
                contentAlignment = Alignment.Center,
            ) {
                val painter = rememberVectorPainter(resolveIconVector(iconKey))
                Image(
                    painter = painter,
                    contentDescription = iconKey,
                    modifier = Modifier.size(MM.dimen.padding_2_5x),
                    colorFilter = ColorFilter.tint(if (isSelected) Color.White else colors.text),
                )
            }
        }
    }
}
