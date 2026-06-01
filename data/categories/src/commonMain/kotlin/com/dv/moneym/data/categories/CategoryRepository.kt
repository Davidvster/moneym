package com.dv.moneym.data.categories

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeActive(): Flow<List<Category>>
    suspend fun getById(id: CategoryId): Category?
    suspend fun count(): Long
    suspend fun insert(category: Category): CategoryId
    suspend fun update(category: Category)
    suspend fun delete(id: CategoryId)
    suspend fun markDeletedBySyncId(syncId: String, now: Long)
    suspend fun reviveBySyncId(syncId: String, now: Long)
    suspend fun deleteAll()
    suspend fun exportForSync(): List<CategorySyncRow>
    suspend fun upsertFromSync(row: CategorySyncRow): Long
}
