package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.TransactionSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

internal class TransactionRepositoryImpl(
    private val dataSource: TransactionLocalDataSource,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        dataSource.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> =
        dataSource.observeByMonth(yearMonthKey(year, month))
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> =
        when (filter) {
            is TransactionFilter.None -> observeAll()
            is TransactionFilter.ByCategory ->
                dataSource.observeByCategory(filter.categoryId.value)
                    .map { it.map { r -> r.toDomain() } }

            is TransactionFilter.ByType ->
                dataSource.observeByType(filter.type.name).map { it.map { r -> r.toDomain() } }

            is TransactionFilter.ByCategoryAndType ->
                dataSource.observeByCategoryAndType(filter.categoryId.value, filter.type.name)
                    .map { it.map { r -> r.toDomain() } }
        }

    override suspend fun getById(id: TransactionId): Transaction? =
        dataSource.getById(id.value)?.toDomain()

    override suspend fun upsert(transaction: Transaction): TransactionId {
        val now = Clock.System.now().toEpochMilliseconds()
        return if (transaction.id == UNSAVED_TRANSACTION_ID) {
            val newId = dataSource.insert(
                type = transaction.type.name,
                amountMinor = transaction.amount.minorUnits,
                currency = transaction.amount.currency.value,
                occurredOn = transaction.occurredOn.toString(),
                note = transaction.note,
                categoryId = transaction.categoryId.value,
                accountId = transaction.accountId.value,
                createdAt = now,
                updatedAt = now,
                paymentModeId = transaction.paymentModeId?.value,
                recurringId = transaction.recurringId?.value,
            )
            TransactionId(newId)
        } else {
            dataSource.update(
                id = transaction.id.value,
                type = transaction.type.name,
                amountMinor = transaction.amount.minorUnits,
                currency = transaction.amount.currency.value,
                occurredOn = transaction.occurredOn.toString(),
                note = transaction.note,
                categoryId = transaction.categoryId.value,
                accountId = transaction.accountId.value,
                updatedAt = now,
                paymentModeId = transaction.paymentModeId?.value,
                recurringId = transaction.recurringId?.value,
            )
            transaction.id
        }
    }

    override suspend fun delete(id: TransactionId) =
        dataSource.softDelete(id.value, Clock.System.now().toEpochMilliseconds())

    override suspend fun deleteByAccountId(id: AccountId) =
        dataSource.softDeleteByAccountId(id.value, Clock.System.now().toEpochMilliseconds())

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) =
        dataSource.markDeletedBySyncId(syncId, now)

    override suspend fun reviveBySyncId(syncId: String, now: Long) =
        dataSource.reviveBySyncId(syncId, now)

    override suspend fun deleteAll() = dataSource.deleteAll()

    override suspend fun convertCurrencyForAccount(accountId: AccountId, newCurrency: CurrencyCode, rate: Double) {
        dataSource.convertCurrencyForAccount(
            accountId = accountId.value,
            currency = newCurrency.value,
            rate = rate,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun getEarliestTransactionDate(): LocalDate? =
        dataSource.getEarliestDate()?.let { LocalDate.parse(it) }

    override suspend fun getLatestTransactionDate(): LocalDate? =
        dataSource.getLatestDate()?.let { LocalDate.parse(it) }

    override fun getTransactionDates(): Flow<Set<LocalDate>> =
        dataSource.getDistinctTransactionDates()
            .map { strings ->
                strings.mapNotNullTo(mutableSetOf()) { s ->
                    runCatching { LocalDate.parse(s) }.getOrNull()
                }
            }

    override suspend fun countByRecurringId(id: RecurringTransactionId): Int =
        dataSource.countByRecurringId(id.value)

    override suspend fun exportForSync(): List<TransactionSyncRow> =
        dataSource.exportForSync().map { it.toSyncRow() }

    override suspend fun upsertFromSync(row: TransactionSyncRow): Long = dataSource.upsertFromSync(row)
}
