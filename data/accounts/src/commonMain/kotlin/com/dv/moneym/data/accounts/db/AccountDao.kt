package com.dv.moneym.data.accounts.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account ORDER BY name ASC")
    fun selectAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM Account WHERE is_default = 1 LIMIT 1")
    fun selectDefault(): Flow<AccountEntity?>

    @Query("SELECT * FROM Account WHERE id = :id")
    suspend fun selectById(id: Long): AccountEntity?

    @Query("SELECT * FROM Account WHERE sync_id = :syncId LIMIT 1")
    suspend fun selectBySyncId(syncId: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM Account")
    suspend fun countAll(): Long

    @Query("SELECT * FROM Account")
    suspend fun selectAllForSync(): List<AccountEntity>

    @Insert
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("DELETE FROM Account WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}
