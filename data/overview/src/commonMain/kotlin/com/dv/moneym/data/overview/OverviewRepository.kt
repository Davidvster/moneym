package com.dv.moneym.data.overview

import kotlinx.coroutines.flow.Flow

interface OverviewRepository {
    fun observeLayoutPrefs(): Flow<OverviewLayoutPrefs>
    fun observeAiWidgets(): Flow<List<OverviewAiWidget>>
    suspend fun replaceLayout(blocks: List<OverviewLayoutBlock>)
    suspend fun upsertAiWidget(widget: OverviewAiWidget): Long
    suspend fun deleteAiWidget(id: Long)
    suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean)
    suspend fun replaceAiWidgets(widgets: List<OverviewAiWidget>)
}
