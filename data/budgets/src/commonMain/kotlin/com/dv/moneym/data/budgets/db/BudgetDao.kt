package com.dv.moneym.data.budgets.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM Budget ORDER BY created_at DESC")
    fun selectAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM Budget WHERE account_id = :accountId OR account_id = 0 ORDER BY created_at DESC")
    fun selectByAccount(accountId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM Budget WHERE id = :id")
    suspend fun selectById(id: Long): BudgetEntity?

    @Insert
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("DELETE FROM Budget WHERE id = :id")
    suspend fun deleteById(id: Long)
}
