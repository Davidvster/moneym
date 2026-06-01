package com.dv.moneym.data.transactions.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentModeDao {
    @Query("SELECT * FROM PaymentMode ORDER BY id")
    fun selectAll(): Flow<List<PaymentModeEntity>>

    @Query("SELECT * FROM PaymentMode WHERE id = :id")
    suspend fun selectById(id: Long): PaymentModeEntity?

    @Insert
    suspend fun insert(entity: PaymentModeEntity): Long

    @Query("UPDATE PaymentMode SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM PaymentMode WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM PaymentMode")
    suspend fun countAll(): Long

    @Query("SELECT * FROM PaymentMode")
    suspend fun selectAllForSync(): List<PaymentModeEntity>
}
