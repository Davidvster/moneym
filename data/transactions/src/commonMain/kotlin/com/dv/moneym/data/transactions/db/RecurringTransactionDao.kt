package com.dv.moneym.data.transactions.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM RecurringTransactionEntry WHERE deleted = 0 ORDER BY created_at DESC")
    fun selectAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM RecurringTransactionEntry WHERE id = :id")
    suspend fun selectById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM RecurringTransactionEntry WHERE sync_id = :syncId LIMIT 1")
    suspend fun selectBySyncId(syncId: String): RecurringTransactionEntity?

    @Insert
    suspend fun insert(entity: RecurringTransactionEntity): Long

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Query("UPDATE RecurringTransactionEntry SET last_materialized = :date, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCursor(id: Long, date: String, updatedAt: Long)

    @Query("DELETE FROM RecurringTransactionEntry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE RecurringTransactionEntry SET deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDeleteById(id: Long, now: Long)

    @Query("UPDATE RecurringTransactionEntry SET deleted = 1, updated_at = :now WHERE sync_id = :syncId")
    suspend fun markDeletedBySyncId(syncId: String, now: Long)

    @Query("UPDATE RecurringTransactionEntry SET updated_at = :now WHERE sync_id = :syncId")
    suspend fun touchBySyncId(syncId: String, now: Long)

    @Query("DELETE FROM RecurringTransactionEntry")
    suspend fun deleteAll()

    @Query("SELECT * FROM RecurringTransactionEntry")
    suspend fun selectAllForSync(): List<RecurringTransactionEntity>
}
