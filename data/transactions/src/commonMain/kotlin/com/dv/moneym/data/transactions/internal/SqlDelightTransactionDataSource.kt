package com.dv.moneym.data.transactions.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.transactions.TransactionEntry
import com.dv.moneym.data.transactions.db.TransactionsDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class SqlDelightTransactionDataSource(
    private val db: TransactionsDatabase,
    private val dispatchers: DispatcherProvider,
) : TransactionLocalDataSource {

    private val q get() = db.transactionEntryQueries

    override fun observeAll(): Flow<List<TransactionEntry>> =
        q.selectAll().asFlow().mapToList(dispatchers.io)

    override fun observeByMonth(yearMonth: String): Flow<List<TransactionEntry>> =
        q.selectByMonth(yearMonth).asFlow().mapToList(dispatchers.io)

    override fun observeByCategory(categoryId: Long): Flow<List<TransactionEntry>> =
        q.selectByCategory(categoryId).asFlow().mapToList(dispatchers.io)

    override fun observeByType(type: String): Flow<List<TransactionEntry>> =
        q.selectByType(type).asFlow().mapToList(dispatchers.io)

    override fun observeByCategoryAndType(categoryId: Long, type: String): Flow<List<TransactionEntry>> =
        q.selectByCategoryAndType(categoryId, type).asFlow().mapToList(dispatchers.io)

    override suspend fun getById(id: Long): TransactionEntry? = withContext(dispatchers.io) {
        q.selectById(id).executeAsOneOrNull()
    }

    override suspend fun insert(
        type: String, amountMinor: Long, currency: String, occurredOn: String,
        note: String?, categoryId: Long, accountId: Long, createdAt: Long, updatedAt: Long,
        paymentModeId: Long?,
    ): Long = withContext(dispatchers.io) {
        var id = 0L
        db.transaction {
            q.insert(type, amountMinor, currency, occurredOn, note, categoryId, accountId, createdAt, updatedAt, paymentModeId)
            id = q.lastInsertId().executeAsOne()
        }
        id
    }

    override suspend fun update(
        id: Long, type: String, amountMinor: Long, currency: String,
        occurredOn: String, note: String?, categoryId: Long, accountId: Long, updatedAt: Long,
        paymentModeId: Long?,
    ) = withContext(dispatchers.io) {
        q.updateById(type, amountMinor, currency, occurredOn, note, categoryId, accountId, updatedAt, paymentModeId, id)
    }

    override suspend fun delete(id: Long) = withContext(dispatchers.io) {
        q.deleteById(id)
    }

    override suspend fun getEarliestDate(): String? = withContext(dispatchers.io) {
        q.getEarliestDate().executeAsOne().MIN
    }

    override suspend fun getLatestDate(): String? = withContext(dispatchers.io) {
        q.getLatestDate().executeAsOne().MAX
    }

    override fun getDistinctTransactionDates(): Flow<List<String>> =
        db.transactionEntryQueries.selectDistinctDates()
            .asFlow()
            .mapToList(dispatchers.io)
}
