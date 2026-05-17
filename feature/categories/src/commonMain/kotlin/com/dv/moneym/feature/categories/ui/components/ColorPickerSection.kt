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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.dv.moneym.core.ui.MmIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerSection(
    palette: List<Color>,
    selectedColor: Color,
    customColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    colors: MoneyMColors,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Preset palette swatches
        palette.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) },
            )
        }
        // Custom colors generated via HSV picker — each appears as its own swatch
        customColors.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) },
            )
        }
        // "+" button to open HSV color picker — always visible
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .border(
                    1.dp,
                    colors.borderStrong,
                    RoundedCornerShape(10.dp),
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onCustomColorClick() },
            contentAlignment = Alignment.Center,
        ) {
            val painter = rememberVectorPainter(MmIcons.plus)
            Image(
                painter = painter,
                contentDescription = "Custom color",
                modifier = Modifier.size(MM.dimen.padding_2x),
                colorFilter = ColorFilter.tint(colors.text2),
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(MM.dimen.padding_1x))
                    .background(colors.bg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    val painter = rememberVectorPainter(MmIcons.check)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(MM.dimen.padding_2x),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
            }
        }
    }
}