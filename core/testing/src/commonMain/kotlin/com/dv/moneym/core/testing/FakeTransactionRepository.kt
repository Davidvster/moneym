package com.dv.moneym.core.testing

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

class FakeTransactionRepository : TransactionRepository {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L

    val transactions: List<Transaction> get() = _transactions.value

    fun addAll(transactions: List<Transaction>) = _transactions.update { it + transactions }

    override fun observeAll(): Flow<List<Transaction>> = _transactions

    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> =
        _transactions.map { list ->
            list.filter { it.occurredOn.year == year && it.occurredOn.month.number == month }
        }

    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> =
        _transactions.map { list ->
            when (filter) {
                is TransactionFilter.None -> list
                is TransactionFilter.ByCategory -> list.filter { it.categoryId == filter.categoryId }
                is TransactionFilter.ByType -> list.filter { it.type == filter.type }
                is TransactionFilter.ByCategoryAndType ->
                    list.filter { it.categoryId == filter.categoryId && it.type == filter.type }
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
        _transactions.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deleteByAccountId(id: AccountId) {
        _transactions.update { list -> list.filter { it.accountId != id } }
    }

    override suspend fun deleteAll() {
        _transactions.value = emptyList()
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

    override suspend fun getEarliestTransactionDate(): LocalDate? = null

    override suspend fun getLatestTransactionDate(): LocalDate? = null

    override fun getTransactionDates(): Flow<Set<LocalDate>> =
        _transactions.map { list -> list.map { it.occurredOn }.toSet() }

    override suspend fun countByRecurringId(id: RecurringTransactionId): Int =
        _transactions.value.count { it.recurringId == id }

    override suspend fun exportForSync(): List<TransactionSyncRow> =
        _transactions.value.map { t ->
            TransactionSyncRow(
                id = t.id.value,
                syncId = "sync-tx-${t.id.value}",
                type = t.type.name,
                amountMinor = t.amount.minorUnits,
                currency = t.amount.currency.value,
                occurredOn = t.occurredOn.toString(),
                note = t.note,
                categoryId = t.categoryId.value,
                accountId = t.accountId.value,
                paymentModeId = t.paymentModeId?.value,
                recurringId = t.recurringId?.value,
                deleted = false,
                createdAt = t.createdAt.toEpochMilliseconds(),
                updatedAt = t.updatedAt.toEpochMilliseconds(),
            )
        }
}
