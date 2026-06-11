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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMColors
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.imageVector
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
    iconOptions: List<Icon>,
    selectedColor: Color,
    selectedIcon: Icon,
    customColors: List<Color>,
    isEditMode: Boolean,
    onNameChange: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    onIconSelected: (Icon) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    nameError: String? = null,
) {
    val colors = MM.colors

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
    ) {
        // Live preview chip
        CategoryPreviewChip(
            name = name,
            selectedColor = selectedColor,
            selectedIcon = selectedIcon,
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
        if (nameError != null) {
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Text(
                text = nameError,
                style = MM.type.caption,
                color = Color(0xFFB00020),
                modifier = Modifier.fillMaxWidth(),
            )
        }

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
            selectedIcon = selectedIcon,
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
    selectedIcon: Icon,
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
                .border(MM.dimen.strokeHairline, colors.border, MM.dimen.pill)
                .padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1_25x),
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
                val painter = rememberVectorPainter(selectedIcon.imageVector)
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
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_25x),
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_25x),
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
                .size(MM.dimen.iconXl)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .border(
                    MM.dimen.strokeHairline,
                    colors.borderStrong,
                    RoundedCornerShape(10.dp),
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onCustomColorClick() },
            contentAlignment = Alignment.Center,
        ) {
            val painter = rememberVectorPainter(Icon.Plus.imageVector)
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
            .size(MM.dimen.iconXl)
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
                    val painter = rememberVectorPainter(Icon.Check.imageVector)
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
    iconOptions: List<Icon>,
    selectedIcon: Icon,
    selectedColor: Color,
    onIconSelected: (Icon) -> Unit,
    colors: MoneyMColors,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_25x),
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_25x),
    ) {
        iconOptions.forEach { icon ->
            val isSelected = selectedIcon == icon
            Box(
                modifier = Modifier
                    .size(MM.dimen.rowMinHeightMd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) selectedColor else colors.surface)
                    .border(
                        width = MM.dimen.strokeHairline,
                        color = if (isSelected) selectedColor else colors.border,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onIconSelected(icon) },
                contentAlignment = Alignment.Center,
            ) {
                val painter = rememberVectorPainter(icon.imageVector)
                Image(
                    painter = painter,
                    contentDescription = icon.key,
                    modifier = Modifier.size(MM.dimen.padding_2_5x),
                    colorFilter = ColorFilter.tint(if (isSelected) Color.White else colors.text),
                )
            }
        }
    }
}

@Preview
@Composable
private fun NewCategorySheetBodyPreview() {
    val palette = listOf(
        Color(0xFF4A8E5C),
        Color(0xFFE07A5F),
        Color(0xFF2D6CDF),
        Color(0xFF9B51E0),
        Color(0xFFF4B400),
    )
    val iconOptions = listOf(
        Icon.Basket,
        Icon.Car,
        Icon.Home,
        Icon.Heart,
        Icon.Gift,
    )
    MoneyMTheme {
        NewCategorySheetBody(
            name = "Groceries",
            palette = palette,
            iconOptions = iconOptions,
            selectedColor = palette.first(),
            selectedIcon = iconOptions.first(),
            customColors = listOf(Color(0xFF12B5A5)),
            isEditMode = true,
            onNameChange = {},
            onColorSelected = {},
            onCustomColorClick = {},
            onIconSelected = {},
            onDeleteClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}