package com.dv.moneym.data.overview.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OverviewDao {
    @Query("SELECT * FROM OverviewLayoutBlock ORDER BY sort_order ASC")
    fun observeLayoutBlocks(): Flow<List<OverviewLayoutBlockEntity>>

    @Query("SELECT * FROM OverviewAiWidget ORDER BY sort_order ASC, created_at ASC")
    fun observeAiWidgets(): Flow<List<OverviewAiWidgetEntity>>

    @Query("DELETE FROM OverviewLayoutBlock")
    suspend fun deleteLayoutBlocks()

    @Insert
    suspend fun insertLayoutBlocks(blocks: List<OverviewLayoutBlockEntity>)

    @Transaction
    suspend fun replaceLayoutBlocks(blocks: List<OverviewLayoutBlockEntity>) {
        deleteLayoutBlocks()
        insertLayoutBlocks(blocks)
    }

    @Insert
    suspend fun insertAiWidget(widget: OverviewAiWidgetEntity): Long

    @Update
    suspend fun updateAiWidget(widget: OverviewAiWidgetEntity)

    @Query("SELECT * FROM OverviewAiWidget WHERE id = :id")
    suspend fun selectAiWidget(id: Long): OverviewAiWidgetEntity?

    @Query("DELETE FROM OverviewAiWidget WHERE id = :id")
    suspend fun deleteAiWidget(id: Long)

    @Query("UPDATE OverviewAiWidget SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setAiWidgetEnabled(id: Long, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM OverviewAiWidget")
    suspend fun deleteAiWidgets()

    @Transaction
    suspend fun replaceAiWidgets(widgets: List<OverviewAiWidgetEntity>) {
        deleteAiWidgets()
        widgets.forEach { insertAiWidget(it.copy(id = 0)) }
    }
}
