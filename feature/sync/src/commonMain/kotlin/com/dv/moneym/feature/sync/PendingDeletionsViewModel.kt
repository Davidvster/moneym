package com.dv.moneym.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.data.sync.PendingDeletion
import com.dv.moneym.data.sync.SyncDeletionController
import com.dv.moneym.data.sync.SyncEntityType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val GROUP_ORDER = listOf(
    SyncEntityType.ACCOUNT,
    SyncEntityType.TRANSACTION,
    SyncEntityType.BUDGET,
    SyncEntityType.RECURRING,
    SyncEntityType.CATEGORY,
    SyncEntityType.PAYMENT_MODE,
)

class PendingDeletionsViewModel(
    private val controller: SyncDeletionController,
) : ViewModel() {

    // syncId -> user override. Absent means the default (checked = will delete).
    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _isResolving = MutableStateFlow(false)

    internal val state: StateFlow<PendingDeletionsUiState> = combine(
        controller.pendingDeletions,
        _overrides,
        _isResolving,
    ) { pending, overrides, isResolving ->
        buildState(pending, overrides, isResolving)
    }.stateIn(viewModelScope, SharingStarted.Lazily, PendingDeletionsUiState())

    private val _effects = Channel<PendingDeletionsEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    fun onIntent(intent: PendingDeletionsIntent) {
        when (intent) {
            is PendingDeletionsIntent.ToggleItem -> toggleItem(intent.syncId)
            is PendingDeletionsIntent.ToggleGroup -> toggleGroup(intent.type)
            PendingDeletionsIntent.ConfirmSelected -> confirmSelected()
            PendingDeletionsIntent.Cancel -> cancel()
        }
    }

    private fun buildState(
        pending: List<PendingDeletion>,
        overrides: Map<String, Boolean>,
        isResolving: Boolean,
    ): PendingDeletionsUiState {
        val byType = pending.groupBy { it.entityType }
        val groups = GROUP_ORDER.mapNotNull { type ->
            val rows = byType[type] ?: return@mapNotNull null
            val items = rows.map { p ->
                DeletionItem(
                    syncId = p.syncId,
                    label = p.label,
                    checked = overrides[p.syncId] ?: true,
                )
            }
            DeletionGroup(type = type, items = items)
        }
        val selectedCount = groups.sumOf { g -> g.items.count { it.checked } }
        return PendingDeletionsUiState(
            groups = groups,
            selectedCount = selectedCount,
            isResolving = isResolving,
        )
    }

    private fun toggleItem(syncId: String) {
        val current = state.value.groups.flatMap { it.items }.firstOrNull { it.syncId == syncId }
            ?: return
        _overrides.update { it + (syncId to !current.checked) }
    }

    private fun toggleGroup(type: SyncEntityType) {
        val group = state.value.groups.firstOrNull { it.type == type } ?: return
        val target = !group.allChecked
        _overrides.update { overrides ->
            overrides + group.items.associate { it.syncId to target }
        }
    }

    private fun confirmSelected() {
        if (_isResolving.value) return
        val confirmed = state.value.groups
            .flatMap { it.items }
            .filter { it.checked }
            .map { it.syncId }
            .toSet()
        _isResolving.update { true }
        viewModelScope.launch {
            controller.resolveDeletions(confirmed)
            _effects.send(PendingDeletionsEffect.Done)
        }
    }

    private fun cancel() {
        viewModelScope.launch { _effects.send(PendingDeletionsEffect.Done) }
    }
}
