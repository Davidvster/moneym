package com.dv.moneym.feature.categories.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.feature.categories.presentation.CategoryEditEffect
import com.dv.moneym.feature.categories.presentation.CategoryEditViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class CategoryEditKey(
    val id: Long? = null,
    val sessionKey: String = kotlin.random.Random.nextLong().toString(),
) : ModalKey

fun EntryProviderScope<NavKey>.categoryEditEntry(
    onDismiss: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<CategoryEditKey>(metadata = metadata) { key ->
    CategoryEditScreen(
        categoryId = key.id?.let { CategoryId(it) },
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
    // This screen is kept for backward navigation compatibility.
    // The primary create/edit flow is now the NewCategorySheet within CategoryListScreen.
    // If reached via direct navigation, we dismiss immediately and let the list handle editing.
    Box(modifier = Modifier.fillMaxSize())
}
