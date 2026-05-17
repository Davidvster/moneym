package com.dv.moneym.feature.categories.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
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
