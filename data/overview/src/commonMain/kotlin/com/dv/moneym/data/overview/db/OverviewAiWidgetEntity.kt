package com.dv.moneym.data.overview.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "OverviewAiWidget")
data class OverviewAiWidgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prompt: String,
    @ColumnInfo(name = "a2ui_json")
    val a2uiJson: String,
    val enabled: Boolean,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "last_generated_at")
    val lastGeneratedAt: Long? = null,
    @ColumnInfo(name = "last_generation_engine_id")
    val lastGenerationEngineId: String? = null,
)
