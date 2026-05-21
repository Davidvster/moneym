package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.db.PaymentModeEntity
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class SqlDelightPaymentModeDataSource(
    private val db: TransactionsRoomDatabase,
) : PaymentModeRepository {

    private val dao get() = db.paymentModeDao()

    override fun observeAll(): Flow<List<PaymentMode>> =
        dao.selectAll().map { rows ->
            rows.map { row ->
                PaymentMode(
                    id = PaymentModeId(row.id),
                    name = row.name,
                    createdAt = Instant.fromEpochMilliseconds(row.createdAt),
                    updatedAt = Instant.fromEpochMilliseconds(row.updatedAt),
                )
            }
        }

    override suspend fun getById(id: PaymentModeId): PaymentMode? =
        dao.selectById(id.value)?.let { row ->
            PaymentMode(
                id = PaymentModeId(row.id),
                name = row.name,
                createdAt = Instant.fromEpochMilliseconds(row.createdAt),
                updatedAt = Instant.fromEpochMilliseconds(row.updatedAt),
            )
        }

    override suspend fun create(name: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.insert(PaymentModeEntity(name = name, createdAt = now, updatedAt = now))
    }

    override suspend fun rename(id: PaymentModeId, name: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.updateName(id = id.value, name = name, updatedAt = now)
    }

    override suspend fun delete(id: PaymentModeId) = dao.deleteById(id.value)
}
