package com.dv.moneym.data.banksync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM BankAccount ORDER BY bank_name, display_name")
    fun observeAll(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM BankAccount WHERE enabled = 1")
    suspend fun selectEnabled(): List<BankAccountEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(accounts: List<BankAccountEntity>)

    @Update
    suspend fun update(account: BankAccountEntity)

    @Query("SELECT * FROM BankAccount WHERE uid = :uid")
    suspend fun selectByUid(uid: String): BankAccountEntity?

    @Query("UPDATE BankAccount SET local_account_id = :localAccountId WHERE uid = :uid")
    suspend fun setLocalAccountMapping(uid: String, localAccountId: Long?)

    @Query("UPDATE BankAccount SET enabled = :enabled WHERE uid = :uid")
    suspend fun setEnabled(uid: String, enabled: Boolean)

    @Query("UPDATE BankAccount SET last_synced_date = :date, last_synced_at = :at WHERE uid = :uid")
    suspend fun setCursor(uid: String, date: String, at: Long)

    @Query("DELETE FROM BankAccount")
    suspend fun deleteAll()
}

@Dao
interface BankSuggestionDao {
    @Query("SELECT * FROM BankSuggestion WHERE status = :status ORDER BY booking_date DESC, id DESC")
    fun observeByStatus(status: String): Flow<List<BankSuggestionEntity>>

    @Query("SELECT COUNT(*) FROM BankSuggestion WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT external_id FROM BankSuggestion WHERE external_id IN (:externalIds)")
    suspend fun selectExistingExternalIds(externalIds: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(suggestions: List<BankSuggestionEntity>)

    @Query("SELECT * FROM BankSuggestion WHERE id = :id")
    suspend fun selectById(id: Long): BankSuggestionEntity?

    @Query("UPDATE BankSuggestion SET status = :status, created_transaction_id = :transactionId, decided_at = :decidedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, transactionId: Long?, decidedAt: Long?)

    @Query("DELETE FROM BankSuggestion")
    suspend fun deleteAll()
}
