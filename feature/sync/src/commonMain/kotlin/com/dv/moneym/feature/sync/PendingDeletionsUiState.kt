package com.dv.moneym.feature.sync

import com.dv.moneym.data.sync.SyncEntityType

data class DeletionItem(
    val syncId: String,
    val label: String,
    val checked: Boolean,
)

data class DeletionGroup(
    val type: SyncEntityType,
    val items: List<DeletionItem>,
) {
    val allChecked: Boolean get() = items.isNotEmpty() && items.all { it.checked }
}

data class PendingDeletionsUiState(
    val groups: List<DeletionGroup> = emptyList(),
    val selectedCount: Int = 0,
    val isResolving: Boolean = false,
)
