package com.dv.moneym.data.walletsync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletSuggestionDao {
    @Query("SELECT * FROM WalletSuggestion WHERE status = :status ORDER BY date DESC, id DESC")
    fun observeByStatus(status: String): Flow<List<WalletSuggestionEntity>>

    @Query("SELECT COUNT(*) FROM WalletSuggestion WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT external_id FROM WalletSuggestion WHERE external_id IN (:externalIds)")
    suspend fun selectExistingExternalIds(externalIds: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(suggestions: List<WalletSuggestionEntity>)

    @Query("SELECT * FROM WalletSuggestion WHERE id = :id")
    suspend fun selectById(id: Long): WalletSuggestionEntity?

    @Query("UPDATE WalletSuggestion SET status = :status, created_transaction_id = :transactionId, decided_at = :decidedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, transactionId: Long?, decidedAt: Long?)

    @Query("DELETE FROM WalletSuggestion")
    suspend fun deleteAll()
}
