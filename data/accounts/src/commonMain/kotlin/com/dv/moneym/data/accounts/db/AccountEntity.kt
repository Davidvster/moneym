package com.dv.moneym.data.accounts.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Account")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val currency: String,
    @ColumnInfo(name = "is_default", defaultValue = "0") val isDefault: Boolean = false,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "color_hex") val colorHex: String? = null,
)
