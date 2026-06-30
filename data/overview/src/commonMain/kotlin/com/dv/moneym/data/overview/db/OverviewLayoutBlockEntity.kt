package com.dv.moneym.data.overview.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "OverviewLayoutBlock")
data class OverviewLayoutBlockEntity(
    @PrimaryKey
    @ColumnInfo(name = "block_id")
    val blockId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    val visible: Boolean,
)
