package com.dv.moneym.feature.categories.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.categoryIconRegistry
import com.dv.moneym.core.designsystem.defaultCategoryColors
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.feature.categories.presentation.CategoryEditEffect
import com.dv.moneym.feature.categories.presentation.CategoryEditIntent
import com.dv.moneym.feature.categories.presentation.CategoryEditViewModel
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_cancel
import moneym.feature.categories.generated.resources.categories_color_label
import moneym.feature.categories.generated.resources.categories_edit_title
import moneym.feature.categories.generated.resources.categories_icon_label
import moneym.feature.categories.generated.resources.categories_name_error
import moneym.feature.categories.generated.resources.categories_name_label
import moneym.feature.categories.generated.resources.categories_new_title
import moneym.feature.categories.generated.resources.categories_save
import org.jetbrains.compose.resources.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.EntryProviderScope
import com.dv.moneym.core.navigation.ModalKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable data class CategoryEditKey(
    val id: Long? = null,
    val sessionKey: String = kotlin.random.Random.nextLong().toString(),
) : ModalKey

fun EntryProviderScope<NavKey>.categoryEditEntry(onDismiss: () -> Unit, metadata: Map<String, Any> = emptyMap()) = entry<CategoryEditKey>(metadata = metadata) { key ->
    CategoryEditScreen(
        categoryId = key.id?.let { com.dv.moneym.core.model.CategoryId(it) },
        sessionKey = key.sessionKey,
        onDismiss = onDismiss,
    )
}

@Composable
fun CategoryEditScreen(
    categoryId: CategoryId?,
    sessionKey: String = "",
    onDismiss: () -> Unit,
    viewModel: CategoryEditViewModel = koinViewModel(
        key = sessionKey.ifEmpty { categoryId?.value?.toString() ?: "new" },
        parameters = { parametersOf(categoryId) },
    ),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { if (it == CategoryEditEffect.Saved) onDismiss() }
    }
    CategoryEditContent(
        title = if (state.isEditMode) stringResource(Res.string.categories_edit_title) else stringResource(Res.string.categories_new_title),
        name = state.name,
        nameError = state.nameError,
        selectedIconKey = state.selectedIconKey,
        selectedColorHex = state.selectedColorHex,
        onIntent = viewModel::onIntent,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditContent(
    title: String,
    name: String,
    nameError: Boolean,
    selectedIconKey: String,
    selectedColorHex: String,
    onIntent: (CategoryEditIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(MoneyMIcons.Clear, contentDescription = stringResource(Res.string.categories_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(CategoryEditIntent.SaveRequested) }) {
                        Icon(MoneyMIcons.Check, contentDescription = stringResource(Res.string.categories_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.lg),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { onIntent(CategoryEditIntent.NameChanged(it)) },
                label = { Text(stringResource(Res.string.categories_name_label)) },
                isError = nameError,
                supportingText = if (nameError) ({ Text(stringResource(Res.string.categories_name_error)) }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(Res.string.categories_icon_label), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
                verticalArrangement = Arrangement.spacedBy(sp.sm),
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
            ) {
                items(categoryIconRegistry.entries.toList(), key = { it.key }) { (key, icon) ->
                    val selected = key == selectedIconKey
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small,
                            )
                            .clickable { onIntent(CategoryEditIntent.IconSelected(key)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = key, tint = categoryColor(selectedColorHex), modifier = Modifier.size(24.dp))
                    }
                }
            }
            Text(stringResource(Res.string.categories_color_label), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(40.dp),
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
                verticalArrangement = Arrangement.spacedBy(sp.sm),
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
            ) {
                items(defaultCategoryColors.mapIndexed { i, c -> Pair(i, c) }, key = { it.first }) { (_, color) ->
                    val hex = colorToHex(color)
                    val selected = hex == selectedColorHex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onBackground,
                                shape = MaterialTheme.shapes.small,
                            )
                            .clickable { onIntent(CategoryEditIntent.ColorSelected(hex)) },
                    )
                }
            }
        }
    }
}

private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
    fun Int.hex2() = toString(16).padStart(2, '0').uppercase()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return "#${r.hex2()}${g.hex2()}${b.hex2()}"
}
