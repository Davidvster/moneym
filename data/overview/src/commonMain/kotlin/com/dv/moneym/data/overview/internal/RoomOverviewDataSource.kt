package com.dv.moneym.data.overview.internal

import com.dv.moneym.data.overview.db.OverviewAiWidgetEntity
import com.dv.moneym.data.overview.db.OverviewLayoutBlockEntity
import com.dv.moneym.data.overview.db.OverviewRoomDatabase
import kotlinx.coroutines.flow.Flow

internal class RoomOverviewDataSource(
    private val db: OverviewRoomDatabase,
) : OverviewLocalDataSource {

    private val dao get() = db.overviewDao()

    override fun observeLayoutBlocks(): Flow<List<OverviewLayoutBlockEntity>> =
        dao.observeLayoutBlocks()

    override fun observeAiWidgets(): Flow<List<OverviewAiWidgetEntity>> =
        dao.observeAiWidgets()

    override suspend fun replaceLayoutBlocks(blocks: List<OverviewLayoutBlockEntity>) =
        dao.replaceLayoutBlocks(blocks)

    override suspend fun upsertAiWidget(widget: OverviewAiWidgetEntity): Long {
        return if (widget.id == 0L || dao.selectAiWidget(widget.id) == null) {
            dao.insertAiWidget(widget.copy(id = 0))
        } else {
            dao.updateAiWidget(widget)
            widget.id
        }
    }

    override suspend fun deleteAiWidget(id: Long) =
        dao.deleteAiWidget(id)

    override suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean, updatedAt: Long) =
        dao.setAiWidgetEnabled(id, enabled, updatedAt)

    override suspend fun replaceAiWidgets(widgets: List<OverviewAiWidgetEntity>) =
        dao.replaceAiWidgets(widgets)
}
