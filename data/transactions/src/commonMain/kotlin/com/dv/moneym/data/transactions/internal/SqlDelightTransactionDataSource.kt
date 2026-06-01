package com.dv.moneym.data.transactions.internal

import com.dv.moneym.data.transactions.TransactionSyncRow
import com.dv.moneym.data.transactions.db.TransactionEntity
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal class SqlDelightTransactionDataSource(
    private val db: TransactionsRoomDatabase,
) : TransactionLocalDataSource {

    private val dao get() = db.transactionDao()

    override fun observeAll(): Flow<List<TransactionEntity>> = dao.selectAll()

    override fun observeByMonth(yearMonth: String): Flow<List<TransactionEntity>> =
        dao.selectByMonth(yearMonth)

    override fun observeByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        dao.selectByCategory(categoryId)

    override fun observeByType(type: String): Flow<List<TransactionEntity>> =
        dao.selectByType(type)

    override fun observeByCategoryAndType(categoryId: Long, type: String): Flow<List<TransactionEntity>> =
        dao.selectByCategoryAndType(categoryId, type)

    override suspend fun getById(id: Long): TransactionEntity? = dao.selectById(id)

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun insert(
        type: String, amountMinor: Long, currency: String, occurredOn: String,
        note: String?, categoryId: Long, accountId: Long, createdAt: Long, updatedAt: Long,
        paymentModeId: Long?, recurringId: Long?,
    ): Long = dao.insert(
        TransactionEntity(
            type = type,
            amountMinor = amountMinor,
            currency = currency,
            occurredOn = occurredOn,
            note = note,
            categoryId = categoryId,
            accountId = accountId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            paymentModeId = paymentModeId,
            recurringId = recurringId,
            syncId = Uuid.random().toString(),
        )
    )

    override suspend fun update(
        id: Long, type: String, amountMinor: Long, currency: String,
        occurredOn: String, note: String?, categoryId: Long, accountId: Long, updatedAt: Long,
        paymentModeId: Long?, recurringId: Long?,
    ) {
        val existing = dao.selectById(id) ?: return
        dao.update(
            existing.copy(
                type = type,
                amountMinor = amountMinor,
                currency = currency,
                occurredOn = occurredOn,
                note = note,
                categoryId = categoryId,
                accountId = accountId,
                updatedAt = updatedAt,
                paymentModeId = paymentModeId,
                recurringId = recurringId,
            )
        )
    }

    override suspend fun softDelete(id: Long, now: Long) = dao.softDeleteById(id, now)

    override suspend fun softDeleteByAccountId(accountId: Long, now: Long) =
        dao.softDeleteByAccountId(accountId, now)

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) =
        dao.markDeletedBySyncId(syncId, now)

    override suspend fun reviveBySyncId(syncId: String, now: Long) =
        dao.touchBySyncId(syncId, now)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun convertCurrencyForAccount(
        accountId: Long, currency: String, rate: Double, updatedAt: Long,
    ) = dao.convertCurrencyForAccount(rate = rate, currency = currency, updatedAt = updatedAt, accountId = accountId)

    override suspend fun getEarliestDate(): String? = dao.getEarliestDate()

    override suspend fun getLatestDate(): String? = dao.getLatestDate()

    override fun getDistinctTransactionDates(): Flow<List<String>> = dao.selectDistinctDates()

    override suspend fun countByRecurringId(recurringId: Long): Int = dao.countByRecurringId(recurringId)

    override suspend fun exportForSync(): List<TransactionEntity> = dao.selectAllForSync()

    override suspend fun upsertFromSync(row: TransactionSyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = dao.selectBySyncId(syncId)
        return if (existing == null) {
            dao.insert(
                TransactionEntity(
                    id = 0,
                    type = row.type,
                    amountMinor = row.amountMinor,
                    currency = row.currency,
                    occurredOn = row.occurredOn,
                    note = row.note,
                    categoryId = row.categoryId,
                    accountId = row.accountId,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    paymentModeId = row.paymentModeId,
                    recurringId = row.recurringId,
                    syncId = syncId,
                    deleted = row.deleted,
                )
            )
        } else {
            dao.update(
                existing.copy(
                    type = row.type,
                    amountMinor = row.amountMinor,
                    currency = row.currency,
                    occurredOn = row.occurredOn,
                    note = row.note,
                    categoryId = row.categoryId,
                    accountId = row.accountId,
                    paymentModeId = row.paymentModeId,
                    recurringId = row.recurringId,
                    updatedAt = row.updatedAt,
                    deleted = row.deleted,
                )
            )
            existing.id
        }
    }
}
