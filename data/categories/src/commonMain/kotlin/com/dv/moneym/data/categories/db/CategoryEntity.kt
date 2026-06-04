package com.dv.moneym.data.categories.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "icon_key")                          val iconKey: String,
    @ColumnInfo(name = "color_hex")                         val colorHex: String,
    @ColumnInfo(name = "is_user_created", defaultValue = "0") val isUserCreated: Boolean = false,
    @ColumnInfo(defaultValue = "0")                          val archived: Boolean = false,
    @ColumnInfo(name = "created_at")                        val createdAt: Long,
    @ColumnInfo(name = "updated_at")                        val updatedAt: Long,
    @ColumnInfo(name = "category_type", defaultValue = "'EXPENSE'") val categoryType: String = "EXPENSE",
    @ColumnInfo(name = "sync_id") val syncId: String? = null,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
)
