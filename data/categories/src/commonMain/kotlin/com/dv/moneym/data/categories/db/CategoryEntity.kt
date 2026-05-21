package com.dv.moneym.data.categories.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "icon_key")        val iconKey: String,
    @ColumnInfo(name = "color_hex")       val colorHex: String,
    @ColumnInfo(name = "is_user_created") val isUserCreated: Boolean = false,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at")      val createdAt: Long,
    @ColumnInfo(name = "updated_at")      val updatedAt: Long,
    @ColumnInfo(name = "category_type")   val categoryType: String = "EXPENSE",
)
