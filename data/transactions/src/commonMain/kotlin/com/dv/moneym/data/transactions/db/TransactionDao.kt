package com.dv.moneym.data.transactions.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM TransactionEntry ORDER BY occurred_on DESC, created_at DESC")
    fun selectAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM TransactionEntry WHERE occurred_on LIKE :yearMonth || '-%' ORDER BY occurred_on DESC, created_at DESC")
    fun selectByMonth(yearMonth: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM TransactionEntry WHERE id = :id")
    suspend fun selectById(id: Long): TransactionEntity?

    @Query("SELECT * FROM TransactionEntry WHERE sync_id = :syncId LIMIT 1")
    suspend fun selectBySyncId(syncId: String): TransactionEntity?

    @Query("SELECT * FROM TransactionEntry WHERE category_id = :categoryId ORDER BY occurred_on DESC, created_at DESC")
    fun selectByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM TransactionEntry WHERE type = :type ORDER BY occurred_on DESC, created_at DESC")
    fun selectByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM TransactionEntry WHERE category_id = :categoryId AND type = :type ORDER BY occurred_on DESC, created_at DESC")
    fun selectByCategoryAndType(categoryId: Long, type: String): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM TransactionEntry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM TransactionEntry WHERE account_id = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("DELETE FROM TransactionEntry")
    suspend fun deleteAll()

    @Query("""
        UPDATE TransactionEntry
        SET amount_minor = CAST(ROUND(CAST(amount_minor AS REAL) * :rate) AS INTEGER),
            currency = :currency,
            updated_at = :updatedAt
        WHERE account_id = :accountId
    """)
    suspend fun convertCurrencyForAccount(rate: Double, currency: String, updatedAt: Long, accountId: Long)

    @Query("SELECT MIN(occurred_on) FROM TransactionEntry")
    suspend fun getEarliestDate(): String?

    @Query("SELECT MAX(occurred_on) FROM TransactionEntry")
    suspend fun getLatestDate(): String?

    @Query("SELECT DISTINCT occurred_on FROM TransactionEntry ORDER BY occurred_on")
    fun selectDistinctDates(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM TransactionEntry WHERE recurring_id = :recurringId")
    suspend fun countByRecurringId(recurringId: Long): Int

    @Query("SELECT * FROM TransactionEntry")
    suspend fun selectAllForSync(): List<TransactionEntity>
}
