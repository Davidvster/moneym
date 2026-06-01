package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.data.transactions.RecurringSyncRow
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.db.RecurringTransactionDao
import com.dv.moneym.data.transactions.db.RecurringTransactionEntity
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class RecurringTransactionRepositoryImpl(
    private val dao: RecurringTransactionDao,
) : RecurringTransactionRepository {

    override fun observeAll(): Flow<List<RecurringTransaction>> =
        dao.selectAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: RecurringTransactionId): RecurringTransaction? =
        dao.selectById(id.value)?.toDomain()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun upsert(rule: RecurringTransaction): RecurringTransactionId {
        return if (rule.id == UNSAVED_RECURRING_ID) {
            val newId = dao.insert(rule.toEntity().copy(id = 0, syncId = Uuid.random().toString()))
            RecurringTransactionId(newId)
        } else {
            val existing = dao.selectById(rule.id.value)
            dao.update(rule.toEntity().copy(syncId = existing?.syncId, deleted = existing?.deleted ?: false))
            rule.id
        }
    }

    override suspend fun updateCursor(id: RecurringTransactionId, lastMaterialized: LocalDate) {
        dao.updateCursor(
            id = id.value,
            date = lastMaterialized.toString(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun delete(id: RecurringTransactionId) = dao.deleteById(id.value)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun exportForSync(): List<RecurringSyncRow> =
        dao.selectAllForSync().map { it.toSyncRow() }

    override suspend fun upsertFromSync(row: RecurringSyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = dao.selectBySyncId(syncId)
        return if (existing == null) {
            dao.insert(row.toEntity(id = 0, syncId = syncId))
        } else {
            dao.update(row.toEntity(id = existing.id, syncId = syncId))
            existing.id
        }
    }

    private fun RecurringSyncRow.toEntity(id: Long, syncId: String) = RecurringTransactionEntity(
        id = id,
        type = type,
        amountMinor = amountMinor,
        currency = currency,
        note = note,
        categoryId = categoryId,
        accountId = accountId,
        paymentModeId = paymentModeId,
        startDate = startDate,
        freqUnit = freqUnit,
        freqInterval = freqInterval,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        useLastDay = useLastDay,
        endKind = endKind,
        endCount = endCount,
        endDate = endDate,
        lastMaterializedDate = lastMaterializedDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncId = syncId,
        deleted = deleted,
    )
}
