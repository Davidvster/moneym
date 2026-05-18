package com.dv.moneym.data.transactions.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.db.TransactionsDatabase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlDelightPaymentModeDataSource(
    private val db: TransactionsDatabase,
    private val dispatchers: DispatcherProvider,
) : PaymentModeRepository {

    private val q get() = db.paymentModeQueries

    override fun observeAll(): Flow<List<PaymentMode>> =
        q.selectAll().asFlow().mapToList(dispatchers.io).map { rows ->
            rows.map { row ->
                PaymentMode(
                    id = PaymentModeId(row.id),
                    name = row.name,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                )
            }
        }

    override suspend fun getById(id: PaymentModeId): PaymentMode? = withContext(dispatchers.io) {
        q.selectById(id.value).executeAsOneOrNull()?.let { row ->
            PaymentMode(
                id = PaymentModeId(row.id),
                name = row.name,
                createdAt = Instant.fromEpochMilliseconds(row.created_at),
                updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
            )
        }
    }

    override suspend fun create(name: String) = withContext(dispatchers.io) {
        val now = Clock.System.now().toEpochMilliseconds()
        q.insert(name = name, created_at = now, updated_at = now)
    }

    override suspend fun rename(id: PaymentModeId, name: String) = withContext(dispatchers.io) {
        val now = Clock.System.now().toEpochMilliseconds()
        q.updateName(name = name, updated_at = now, id = id.value)
    }

    override suspend fun delete(id: PaymentModeId) = withContext(dispatchers.io) {
        q.deleteById(id.value)
    }
}
