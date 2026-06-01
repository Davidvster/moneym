package com.dv.moneym.data.categories.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category ORDER BY name ASC")
    fun selectAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM Category WHERE archived = 0 ORDER BY name ASC")
    fun selectActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM Category WHERE id = :id")
    suspend fun selectById(id: Long): CategoryEntity?

    @Query("SELECT * FROM Category WHERE sync_id = :syncId LIMIT 1")
    suspend fun selectBySyncId(syncId: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM Category")
    suspend fun countAll(): Long

    @Query("SELECT * FROM Category")
    suspend fun selectAllForSync(): List<CategoryEntity>

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)

    @Query("DELETE FROM Category WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM Category")
    suspend fun deleteAll()
}
