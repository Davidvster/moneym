package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.db.RecurringTransactionDao
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
}
