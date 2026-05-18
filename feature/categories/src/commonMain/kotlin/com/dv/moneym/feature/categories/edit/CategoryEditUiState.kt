package com.dv.moneym.feature.categories.edit

import com.dv.moneym.core.model.Icon
import kotlinx.serialization.Serializable

@Serializable
internal data class CategoryEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val name: String = "",
    val selectedIcon: Icon = Icon.Dots,
    val selectedColorHex: String = "#8A8A8A",
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
)

internal sealed interface CategoryEditIntent {
    data class NameChanged(val name: String) : CategoryEditIntent
    data class IconSelected(val icon: Icon) : CategoryEditIntent
    data class ColorSelected(val hex: String) : CategoryEditIntent
    data object SaveRequested : CategoryEditIntent
}

internal sealed interface CategoryEditEffect {
    data object Saved : CategoryEditEffect
}
