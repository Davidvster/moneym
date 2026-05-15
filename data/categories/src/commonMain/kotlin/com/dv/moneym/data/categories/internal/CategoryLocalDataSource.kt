package com.dv.moneym.data.categories.internal

import com.dv.moneym.data.categories.Category
import kotlinx.coroutines.flow.Flow

internal interface CategoryLocalDataSource {
    fun observeAll(): Flow<List<Category>>
    fun observeActive(): Flow<List<Category>>
    suspend fun getById(id: Long): Category?
    suspend fun count(): Long
    suspend fun insert(
        name: String, iconKey: String, colorHex: String,
        isUserCreated: Boolean, createdAt: Long, updatedAt: Long,
    ): Long
    suspend fun update(
        id: Long, name: String, iconKey: String, colorHex: String,
        archived: Boolean, updatedAt: Long,
    )
    suspend fun delete(id: Long)
}
