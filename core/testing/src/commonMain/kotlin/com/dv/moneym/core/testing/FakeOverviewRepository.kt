package com.dv.moneym.core.testing

import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.OverviewLayoutPrefs
import com.dv.moneym.data.overview.OverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

class FakeOverviewRepository : OverviewRepository {
    private val _layout = MutableStateFlow(OverviewLayoutPrefs())
    private val _widgets = MutableStateFlow<List<OverviewAiWidget>>(emptyList())
    private var nextWidgetId = 1L

    val layout: OverviewLayoutPrefs get() = _layout.value
    val widgets: List<OverviewAiWidget> get() = _widgets.value

    override fun observeLayoutPrefs(): Flow<OverviewLayoutPrefs> = _layout

    override fun observeAiWidgets(): Flow<List<OverviewAiWidget>> =
        _widgets.map { widgets -> widgets.sortedWith(compareBy({ it.sortOrder }, { it.createdAt })) }

    override suspend fun replaceLayout(blocks: List<OverviewLayoutBlock>) {
        _layout.value = OverviewLayoutPrefs(blocks.sortedBy { it.sortOrder })
    }

    override suspend fun upsertAiWidget(widget: OverviewAiWidget): Long {
        val existing = _widgets.value.firstOrNull { it.id == widget.id && widget.id != 0L }
        return if (existing == null) {
            val id = nextWidgetId++
            _widgets.update { it + widget.copy(id = id) }
            id
        } else {
            _widgets.update { list -> list.map { if (it.id == widget.id) widget else it } }
            widget.id
        }
    }

    override suspend fun deleteAiWidget(id: Long) {
        _widgets.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean) {
        val now = Clock.System.now()
        _widgets.update { list ->
            list.map { if (it.id == id) it.copy(enabled = enabled, updatedAt = now) else it }
        }
    }

    override suspend fun replaceAiWidgets(widgets: List<OverviewAiWidget>) {
        var next = 1L
        _widgets.value = widgets.map { widget ->
            val id = if (widget.id == 0L) next++ else widget.id.also { next = maxOf(next, it + 1) }
            widget.copy(id = id)
        }
        nextWidgetId = next
    }
}
