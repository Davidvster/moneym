package com.dv.moneym.feature.categories.presentation

import com.dv.moneym.core.model.CategoryId

data class CategoryEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val name: String = "",
    val selectedIconKey: String = "dots",
    val selectedColorHex: String = "#8A8A8A",
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface CategoryEditIntent {
    data class NameChanged(val name: String) : CategoryEditIntent
    data class IconSelected(val key: String) : CategoryEditIntent
    data class ColorSelected(val hex: String) : CategoryEditIntent
    data object SaveRequested : CategoryEditIntent
}

sealed interface CategoryEditEffect {
    data object Saved : CategoryEditEffect
}
