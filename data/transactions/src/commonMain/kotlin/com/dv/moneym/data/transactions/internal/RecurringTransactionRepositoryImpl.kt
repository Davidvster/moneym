package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.db.RecurringTransactionDao
import kotlin.time.Clock
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

    override suspend fun upsert(rule: RecurringTransaction): RecurringTransactionId {
        return if (rule.id == UNSAVED_RECURRING_ID) {
            val newId = dao.insert(rule.toEntity().copy(id = 0))
            RecurringTransactionId(newId)
        } else {
            dao.update(rule.toEntity())
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
