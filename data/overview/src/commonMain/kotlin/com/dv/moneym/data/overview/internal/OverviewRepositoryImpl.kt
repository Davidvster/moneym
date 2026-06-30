package com.dv.moneym.data.overview.internal

import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.OverviewLayoutPrefs
import com.dv.moneym.data.overview.OverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class OverviewRepositoryImpl(
    private val dataSource: OverviewLocalDataSource,
) : OverviewRepository {

    override fun observeLayoutPrefs(): Flow<OverviewLayoutPrefs> =
        dataSource.observeLayoutBlocks().map { rows ->
            OverviewLayoutPrefs(rows.map { it.toDomain() })
        }

    override fun observeAiWidgets(): Flow<List<OverviewAiWidget>> =
        dataSource.observeAiWidgets().map { rows -> rows.map { it.toDomain() } }

    override suspend fun replaceLayout(blocks: List<OverviewLayoutBlock>) =
        dataSource.replaceLayoutBlocks(blocks.map { it.toEntity() })

    override suspend fun upsertAiWidget(widget: OverviewAiWidget): Long =
        dataSource.upsertAiWidget(widget.toEntity())

    override suspend fun deleteAiWidget(id: Long) =
        dataSource.deleteAiWidget(id)

    override suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean) =
        dataSource.setAiWidgetEnabled(
            id = id,
            enabled = enabled,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )

    override suspend fun replaceAiWidgets(widgets: List<OverviewAiWidget>) =
        dataSource.replaceAiWidgets(widgets.map { it.toEntity() })
}
