package com.dv.moneym.feature.sync

import com.dv.moneym.data.sync.SyncEntityType

sealed interface PendingDeletionsIntent {
    data class ToggleItem(val syncId: String) : PendingDeletionsIntent
    data class ToggleGroup(val type: SyncEntityType) : PendingDeletionsIntent
    data object ConfirmSelected : PendingDeletionsIntent
    data object Cancel : PendingDeletionsIntent
}

internal sealed interface PendingDeletionsEffect {
    data object Done : PendingDeletionsEffect
}
