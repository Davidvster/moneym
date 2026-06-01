package com.dv.moneym.data.budgets.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM Budget WHERE deleted = 0 ORDER BY created_at DESC")
    fun selectAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM Budget WHERE (account_id = :accountId OR account_id = 0) AND deleted = 0 ORDER BY created_at DESC")
    fun selectByAccount(accountId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM Budget WHERE id = :id")
    suspend fun selectById(id: Long): BudgetEntity?

    @Query("SELECT * FROM Budget WHERE sync_id = :syncId LIMIT 1")
    suspend fun selectBySyncId(syncId: String): BudgetEntity?

    @Insert
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("DELETE FROM Budget WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE Budget SET deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDeleteById(id: Long, now: Long)

    @Query("UPDATE Budget SET deleted = 1, updated_at = :now WHERE sync_id = :syncId")
    suspend fun markDeletedBySyncId(syncId: String, now: Long)

    @Query("UPDATE Budget SET updated_at = :now WHERE sync_id = :syncId")
    suspend fun touchBySyncId(syncId: String, now: Long)

    @Query("SELECT * FROM Budget")
    suspend fun selectAllForSync(): List<BudgetEntity>
}
