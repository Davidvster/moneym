package com.dv.moneym.feature.categories.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.feature.categories.presentation.CategoryListEffect
import com.dv.moneym.feature.categories.presentation.CategoryListIntent
import com.dv.moneym.feature.categories.presentation.CategoryListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoryListScreen(
    onEditCategory: (CategoryId?) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CategoryListEffect.NavigateToEdit -> onEditCategory(effect.id)
            }
        }
    }
    CategoryListContent(
        active = state.active,
        archived = state.archived,
        showArchived = state.showArchived,
        onIntent = viewModel::onIntent,
        onAdd = { viewModel.navigateToEdit(null) },
        onEdit = { viewModel.navigateToEdit(it) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryListContent(
    active: List<Category>,
    archived: List<Category>,
    showArchived: Boolean,
    onIntent: (CategoryListIntent) -> Unit,
    onAdd: () -> Unit,
    onEdit: (CategoryId) -> Unit,
    onBack: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(MoneyMIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(MoneyMIcons.Add, contentDescription = "Add category")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
        ) {
            if (active.isEmpty() && archived.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(sp.xxl), contentAlignment = Alignment.Center) {
                    Text("No categories yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }}
            }
            items(active, key = { it.id.value }) { cat ->
                CategoryRow(cat, onEdit = { onEdit(cat.id) }, onArchive = {
                    onIntent(CategoryListIntent.ArchiveRequested(cat.id))
                })
            }
            if (archived.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { onIntent(CategoryListIntent.ToggleShowArchived) },
                        modifier = Modifier.padding(horizontal = sp.lg),
                    ) {
                        Text(if (showArchived) "Hide archived" else "Show archived (${archived.size})")
                    }
                }
                if (showArchived) {
                    items(archived, key = { "arch_${it.id.value}" }) { cat ->
                        CategoryRow(cat, onEdit = { onEdit(cat.id) }, onUnarchive = {
                            onIntent(CategoryListIntent.UnarchiveRequested(cat.id))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
) {
    val sp = MoneyMTheme.spacing
    Surface(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            Icon(
                imageVector = iconForKey(category.iconKey),
                contentDescription = null,
                tint = categoryColor(category.colorHex),
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
                if (category.archived) {
                    Text("Archived", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onUnarchive != null) {
                TextButton(onClick = onUnarchive) { Text("Restore") }
            } else if (onArchive != null && !category.isUserCreated.not()) {
                TextButton(onClick = onArchive) { Text("Archive") }
            }
        }
    }
}
