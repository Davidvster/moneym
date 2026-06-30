package com.dv.moneym.data.overview.internal

import com.dv.moneym.data.overview.db.OverviewAiWidgetEntity
import com.dv.moneym.data.overview.db.OverviewLayoutBlockEntity
import kotlinx.coroutines.flow.Flow

internal interface OverviewLocalDataSource {
    fun observeLayoutBlocks(): Flow<List<OverviewLayoutBlockEntity>>
    fun observeAiWidgets(): Flow<List<OverviewAiWidgetEntity>>
    suspend fun replaceLayoutBlocks(blocks: List<OverviewLayoutBlockEntity>)
    suspend fun upsertAiWidget(widget: OverviewAiWidgetEntity): Long
    suspend fun deleteAiWidget(id: Long)
    suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean, updatedAt: Long)
    suspend fun replaceAiWidgets(widgets: List<OverviewAiWidgetEntity>)
}
