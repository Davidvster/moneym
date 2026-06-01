package com.dv.moneym.data.transactions.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentModeDao {
    @Query("SELECT * FROM PaymentMode WHERE deleted = 0 ORDER BY id")
    fun selectAll(): Flow<List<PaymentModeEntity>>

    @Query("SELECT * FROM PaymentMode WHERE id = :id")
    suspend fun selectById(id: Long): PaymentModeEntity?

    @Query("SELECT * FROM PaymentMode WHERE sync_id = :syncId LIMIT 1")
    suspend fun selectBySyncId(syncId: String): PaymentModeEntity?

    @Insert
    suspend fun insert(entity: PaymentModeEntity): Long

    @Query("UPDATE PaymentMode SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: Long, name: String, updatedAt: Long)

    @Update
    suspend fun update(entity: PaymentModeEntity)

    @Query("DELETE FROM PaymentMode WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE PaymentMode SET deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDeleteById(id: Long, now: Long)

    @Query("UPDATE PaymentMode SET deleted = 1, updated_at = :now WHERE sync_id = :syncId")
    suspend fun markDeletedBySyncId(syncId: String, now: Long)

    @Query("UPDATE PaymentMode SET updated_at = :now WHERE sync_id = :syncId")
    suspend fun touchBySyncId(syncId: String, now: Long)

    @Query("SELECT COUNT(*) FROM PaymentMode WHERE deleted = 0")
    suspend fun countAll(): Long

    @Query("SELECT * FROM PaymentMode")
    suspend fun selectAllForSync(): List<PaymentModeEntity>
}
