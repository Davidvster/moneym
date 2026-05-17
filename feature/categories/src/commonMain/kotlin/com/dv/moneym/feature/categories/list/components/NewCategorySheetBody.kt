package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.feature.categories.list.resolveIconVector
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_color_label
import moneym.feature.categories.generated.resources.categories_delete
import moneym.feature.categories.generated.resources.categories_icon_label
import moneym.feature.categories.generated.resources.categories_name_label
import moneym.feature.categories.generated.resources.categories_name_placeholder
import moneym.feature.categories.generated.resources.categories_name_preview_placeholder
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewCategorySheetBody(
    name: String,
    palette: List<Color>,
    iconOptions: List<String>,
    selectedColor: Color,
    selectedIconKey: String,
    customColors: List<Color>,
    isEditMode: Boolean,
    onNameChange: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    onIconSelected: (String) -> Unit,
    onDeleteClick: () -> Unit,
) {
    val colors = MM.colors

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
    ) {
        // Live preview chip
        CategoryPreviewChip(
            name = name,
            selectedColor = selectedColor,
            selectedIconKey = selectedIconKey,
            placeholderText = stringResource(Res.string.categories_name_preview_placeholder),
            colors = colors,
        )

        Spacer(modifier = Modifier.height(MM.dimen.padding_3x))

        MmField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(Res.string.categories_name_label),
            placeholder = stringResource(Res.string.categories_name_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(MM.dimen.padding_3x))
        Text(
            text = stringResource(Res.string.categories_color_label),
            style = MM.type.caption.copy(color = colors.text2),
        )
        Spacer(modifier = Modifier.height(MM.dimen.padding_1_5x))

        ColorPickerSection(
            palette = palette,
            selectedColor = selectedColor,
            customColors = customColors,
            onColorSelected = onColorSelected,
            onCustomColorClick = onCustomColorClick,
            colors = colors,
        )

        Spacer(modifier = Modifier.height(MM.dimen.padding_3x))
        Text(
            text = stringResource(Res.string.categories_icon_label),
            style = MM.type.caption.copy(color = colors.text2),
        )
        Spacer(modifier = Modifier.height(MM.dimen.padding_1_5x))

        IconPickerSection(
            iconOptions = iconOptions,
            selectedIconKey = selectedIconKey,
            selectedColor = selectedColor,
            onIconSelected = onIconSelected,
            colors = colors,
        )

        if (isEditMode) {
            Spacer(modifier = Modifier.height(MM.dimen.padding_3x))
            MmButton(
                text = stringResource(Res.string.categories_delete),
                onClick = onDeleteClick,
                variant = MmButtonVariant.Danger,
                fullWidth = true,
            )
        }

        Spacer(modifier = Modifier.height(MM.dimen.padding_2x))
    }
}

@Composable
private fun CategoryPreviewChip(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerSection(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerSection(
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