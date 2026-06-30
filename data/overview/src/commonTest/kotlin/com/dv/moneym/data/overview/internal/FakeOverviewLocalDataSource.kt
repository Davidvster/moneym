package com.dv.moneym.data.overview.internal

import com.dv.moneym.data.overview.db.OverviewAiWidgetEntity
import com.dv.moneym.data.overview.db.OverviewLayoutBlockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeOverviewLocalDataSource : OverviewLocalDataSource {
    private val layoutRows = MutableStateFlow<List<OverviewLayoutBlockEntity>>(emptyList())
    private val widgetRows = MutableStateFlow<List<OverviewAiWidgetEntity>>(emptyList())
    private var nextWidgetId = 1L

    override fun observeLayoutBlocks(): Flow<List<OverviewLayoutBlockEntity>> =
        layoutRows.map { rows -> rows.sortedBy { it.sortOrder } }

    override fun observeAiWidgets(): Flow<List<OverviewAiWidgetEntity>> =
        widgetRows.map { rows -> rows.sortedWith(compareBy({ it.sortOrder }, { it.createdAt })) }

    override suspend fun replaceLayoutBlocks(blocks: List<OverviewLayoutBlockEntity>) {
        layoutRows.value = blocks
    }

    override suspend fun upsertAiWidget(widget: OverviewAiWidgetEntity): Long {
        val existing = widgetRows.value.firstOrNull { it.id == widget.id && widget.id != 0L }
        return if (existing == null) {
            val id = nextWidgetId++
            widgetRows.update { it + widget.copy(id = id) }
            id
        } else {
            widgetRows.update { rows -> rows.map { if (it.id == widget.id) widget else it } }
            widget.id
        }
    }

    override suspend fun deleteAiWidget(id: Long) {
        widgetRows.update { rows -> rows.filterNot { it.id == id } }
    }

    override suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean, updatedAt: Long) {
        widgetRows.update { rows ->
            rows.map { if (it.id == id) it.copy(enabled = enabled, updatedAt = updatedAt) else it }
        }
    }

    override suspend fun replaceAiWidgets(widgets: List<OverviewAiWidgetEntity>) {
        var next = 1L
        widgetRows.value = widgets.map { widget ->
            val id = if (widget.id == 0L) next++ else widget.id.also { next = maxOf(next, it + 1) }
            widget.copy(id = id)
        }
        nextWidgetId = next
    }
}
