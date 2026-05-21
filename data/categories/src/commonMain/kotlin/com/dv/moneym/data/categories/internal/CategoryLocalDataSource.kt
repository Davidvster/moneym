package com.dv.moneym.data.categories.internal

import com.dv.moneym.data.categories.db.CategoryEntity
import kotlinx.coroutines.flow.Flow

internal interface CategoryLocalDataSource {
    fun observeAll(): Flow<List<CategoryEntity>>
    fun observeActive(): Flow<List<CategoryEntity>>
    suspend fun getById(id: Long): CategoryEntity?
    suspend fun count(): Long
    suspend fun insert(
        name: String, iconKey: String, colorHex: String,
        isUserCreated: Boolean, createdAt: Long, updatedAt: Long,
        categoryType: String = "EXPENSE",
    ): Long

    suspend fun update(
        id: Long, name: String, iconKey: String, colorHex: String,
        archived: Boolean, updatedAt: Long,
    )

    suspend fun delete(id: Long)
    suspend fun deleteAll()
}
