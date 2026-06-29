package com.dv.moneym.core.testing

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.matches
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.TransactionSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.math.roundToLong
import kotlin.time.Instant

class FakeTransactionRepository : TransactionRepository {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L

    private val syncIds = mutableMapOf<Long, String>()
    private val externalIds = mutableMapOf<Long, String>()

    private val tombstoned = mutableSetOf<Long>()
    private val updatedAtOverrides = mutableMapOf<Long, Long>()

    private fun List<Transaction>.live() = filter { it.id.value !in tombstoned }

    val transactions: List<Transaction> get() = _transactions.value.live()

    fun addAll(transactions: List<Transaction>) = _transactions.update { it + transactions }

    override fun observeAll(): Flow<List<Transaction>> = _transactions.map { it.live() }

    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> =
        _transactions.map { list ->
            list.live().filter { it.occurredOn.year == year && it.occurredOn.month.number == month }
        }

    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> =
        _transactions.map { rows ->
            val list = rows.live()
            when (filter) {
                is TransactionFilter.None -> list
                is TransactionFilter.ByCategory -> list.filter { it.categoryId == filter.categoryId }
                is TransactionFilter.ByType -> list.filter { it.type == filter.type }
                is TransactionFilter.ByCategoryAndType ->
                    list.filter { it.categoryId == filter.categoryId && it.type == filter.type }
                is TransactionFilter.BySelection -> list.filter { filter.matches(it) }
            }
        }

    override suspend fun getById(id: TransactionId): Transaction? =
        _transactions.value.find { it.id == id }

    override suspend fun upsert(transaction: Transaction): TransactionId {
        return if (transaction.id == UNSAVED_TRANSACTION_ID) {
            val id = TransactionId(nextId++)
            _transactions.update { it + transaction.copy(id = id) }
            id
        } else {
            _transactions.update { list -> list.map { if (it.id == transaction.id) transaction else it } }
            transaction.id
        }
    }

    override suspend fun delete(id: TransactionId) {
        tombstoned.add(id.value)
    }

    override suspend fun delete(ids: Set<TransactionId>) {
        tombstoned.addAll(ids.map { it.value })
    }

    override suspend fun updateCategory(ids: Set<TransactionId>, categoryId: CategoryId, type: TransactionType) {
        val idValues = ids.mapTo(mutableSetOf()) { it.value }
        _transactions.update { list ->
            list.map {
                if (it.id.value in idValues && it.id.value !in tombstoned) it.copy(categoryId = categoryId, type = type) else it
            }
        }
    }

    override suspend fun updateAccount(
        ids: Set<TransactionId>,
        accountId: AccountId,
        currency: CurrencyCode,
        rate: Double?,
    ) {
        val idValues = ids.mapTo(mutableSetOf()) { it.value }
        _transactions.update { list ->
            list.map { tx ->
                if (tx.id.value in idValues && tx.id.value !in tombstoned) {
                    tx.copy(
                        accountId = accountId,
                        amount = tx.amount.copy(
                            minorUnits = if (rate == null) tx.amount.minorUnits
                            else (tx.amount.minorUnits.toDouble() * rate).roundToLong(),
                            currency = currency,
                        ),
                    )
                } else {
                    tx
                }
            }
        }
    }

    override suspend fun updatePaymentMode(ids: Set<TransactionId>, paymentModeId: PaymentModeId?) {
        val idValues = ids.mapTo(mutableSetOf()) { it.value }
        _transactions.update { list ->
            list.map {
                if (it.id.value in idValues && it.id.value !in tombstoned) it.copy(paymentModeId = paymentModeId) else it
            }
        }
    }

    override suspend fun deleteByAccountId(id: AccountId) {
        _transactions.value.filter { it.accountId == id }.forEach { tombstoned.add(it.id.value) }
    }

    override suspend fun reassignCategory(from: CategoryId, to: CategoryId) {
        _transactions.update { list ->
            list.map { if (it.categoryId == from && it.id.value !in tombstoned) it.copy(categoryId = to) else it }
        }
    }

    override suspend fun deleteByCategory(id: CategoryId) {
        _transactions.value.filter { it.categoryId == id }.forEach { tombstoned.add(it.id.value) }
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {
        idForSyncId(syncId)?.let { id ->
            tombstoned.add(id)
            updatedAtOverrides[id] = now
        }
    }

    override suspend fun reviveBySyncId(syncId: String, now: Long) {
        idForSyncId(syncId)?.let { id ->
            tombstoned.remove(id)
            updatedAtOverrides[id] = now
        }
    }

    override suspend fun deleteAll() {
        _transactions.value = emptyList()
        syncIds.clear()
        externalIds.clear()
        tombstoned.clear()
        updatedAtOverrides.clear()
    }

    override suspend fun convertCurrencyForAccount(
        accountId: AccountId,
        newCurrency: CurrencyCode,
        rate: Double,
    ) {
        _transactions.update { list ->
            list.map { tx ->
                if (tx.accountId != accountId) tx
                else tx.copy(
                    amount = tx.amount.copy(
                        minorUnits = (tx.amount.minorUnits * rate).toLong(),
                        currency = newCurrency,
                    )
                )
            }
        }
    }

    override suspend fun getEarliestTransactionDate(): LocalDate? =
        _transactions.value.live().minByOrNull { it.occurredOn }?.occurredOn

    override suspend fun getLatestTransactionDate(): LocalDate? =
        _transactions.value.live().maxByOrNull { it.occurredOn }?.occurredOn

    override fun getTransactionDates(): Flow<Set<LocalDate>> =
        _transactions.map { list -> list.live().map { it.occurredOn }.toSet() }

    override suspend fun countByRecurringId(id: RecurringTransactionId): Int =
        _transactions.value.live().count { it.recurringId == id }

    override suspend fun exportForSync(): List<TransactionSyncRow> =
        _transactions.value.map { t ->
            TransactionSyncRow(
                id = t.id.value,
                syncId = syncIdFor(t.id.value),
                type = t.type.name,
                amountMinor = t.amount.minorUnits,
                currency = t.amount.currency.value,
                occurredOn = t.occurredOn.toString(),
                note = t.note,
                categoryId = t.categoryId.value,
                accountId = t.accountId.value,
                paymentModeId = t.paymentModeId?.value,
                recurringId = t.recurringId?.value,
                deleted = t.id.value in tombstoned,
                createdAt = t.createdAt.toEpochMilliseconds(),
                updatedAt = updatedAtOverrides[t.id.value] ?: t.updatedAt.toEpochMilliseconds(),
                externalId = externalIds[t.id.value],
            )
        }

    override suspend fun upsertFromSync(row: TransactionSyncRow): Long {
        val syncId = requireNotNull(row.syncId)
        val existingId = syncIds.entries.firstOrNull { it.value == syncId }?.key
        return if (existingId == null) {
            val id = nextId++
            _transactions.update { it + row.toDomain(id) }
            syncIds[id] = syncId
            row.externalId?.let { externalIds[id] = it }
            id
        } else {
            _transactions.update { list -> list.map { if (it.id.value == existingId) row.toDomain(existingId) else it } }
            row.externalId?.let { externalIds[existingId] = it }
            existingId
        }
    }

    override suspend fun existsByExternalId(externalId: String): Boolean =
        externalIds.containsValue(externalId)

    override suspend fun setExternalId(id: TransactionId, externalId: String) {
        externalIds[id.value] = externalId
    }

    override suspend fun findByDateAndAmount(
        date: LocalDate,
        amountMinor: Long,
        currency: CurrencyCode,
    ): List<Transaction> =
        _transactions.value.live().filter {
            it.occurredOn == date && it.amount.minorUnits == amountMinor && it.amount.currency == currency
        }

    private fun TransactionSyncRow.toDomain(id: Long) = Transaction(
        id = TransactionId(id),
        type = TransactionType.valueOf(type),
        amount = Money(amountMinor, CurrencyCode(currency)),
        occurredOn = LocalDate.parse(occurredOn),
        note = note,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(accountId),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        paymentModeId = paymentModeId?.let { PaymentModeId(it) },
        recurringId = recurringId?.let { RecurringTransactionId(it) },
    )

    private fun syncIdFor(id: Long): String = syncIds.getOrPut(id) { "sync-tx-$id" }

    private fun idForSyncId(syncId: String): Long? {
        _transactions.value.forEach { syncIdFor(it.id.value) }
        return syncIds.entries.firstOrNull { it.value == syncId }?.key
    }
}
