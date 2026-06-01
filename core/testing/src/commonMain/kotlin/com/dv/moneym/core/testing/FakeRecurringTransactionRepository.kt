package com.dv.moneym.core.testing

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.data.transactions.RecurringSyncRow
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class FakeRecurringTransactionRepository : RecurringTransactionRepository {
    private val _rules = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private var nextId = 1L

    private val syncIds = mutableMapOf<Long, String>()

    private val tombstoned = mutableSetOf<Long>()
    private val updatedAtOverrides = mutableMapOf<Long, Long>()

    val rules: List<RecurringTransaction> get() = _rules.value.filter { it.id.value !in tombstoned }

    fun addAll(rules: List<RecurringTransaction>) = _rules.update { it + rules }

    override fun observeAll(): Flow<List<RecurringTransaction>> =
        _rules.map { list -> list.filter { it.id.value !in tombstoned } }

    override suspend fun getById(id: RecurringTransactionId): RecurringTransaction? =
        _rules.value.find { it.id == id }

    override suspend fun upsert(rule: RecurringTransaction): RecurringTransactionId {
        return if (rule.id == UNSAVED_RECURRING_ID) {
            val id = RecurringTransactionId(nextId++)
            _rules.update { it + rule.copy(id = id) }
            id
        } else {
            _rules.update { list -> list.map { if (it.id == rule.id) rule else it } }
            rule.id
        }
    }

    override suspend fun updateCursor(id: RecurringTransactionId, lastMaterialized: LocalDate) {
        _rules.update { list ->
            list.map { if (it.id == id) it.copy(lastMaterializedDate = lastMaterialized) else it }
        }
    }

    override suspend fun delete(id: RecurringTransactionId) {
        tombstoned.add(id.value)
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
        _rules.value = emptyList()
        syncIds.clear()
        tombstoned.clear()
        updatedAtOverrides.clear()
    }

    override suspend fun exportForSync(): List<RecurringSyncRow> =
        _rules.value.map { r ->
            val dayOfWeek = (r.rule as? RecurrenceRule.Weekly)?.dayOfWeek
            val monthly = r.rule as? RecurrenceRule.Monthly
            val dayOfMonth = (monthly?.dayKind as? MonthlyDayKind.OnDay)?.day
            val useLastDay = monthly?.dayKind is MonthlyDayKind.LastDay
            val freqUnit = when (r.rule) {
                is RecurrenceRule.Daily -> "DAILY"
                is RecurrenceRule.Weekly -> "WEEKLY"
                is RecurrenceRule.Monthly -> "MONTHLY"
            }
            val endKind = when (r.endCondition) {
                EndCondition.Unlimited -> "UNLIMITED"
                is EndCondition.Count -> "COUNT"
                is EndCondition.Until -> "UNTIL"
            }
            RecurringSyncRow(
                id = r.id.value,
                syncId = syncIdFor(r.id.value),
                type = r.type.name,
                amountMinor = r.amount.minorUnits,
                currency = r.amount.currency.value,
                note = r.note,
                categoryId = r.categoryId.value,
                accountId = r.accountId.value,
                paymentModeId = r.paymentModeId?.value,
                startDate = r.startDate.toString(),
                freqUnit = freqUnit,
                freqInterval = r.rule.interval,
                dayOfWeek = dayOfWeek,
                dayOfMonth = dayOfMonth,
                useLastDay = useLastDay,
                endKind = endKind,
                endCount = (r.endCondition as? EndCondition.Count)?.occurrences,
                endDate = (r.endCondition as? EndCondition.Until)?.date?.toString(),
                lastMaterializedDate = r.lastMaterializedDate?.toString(),
                deleted = r.id.value in tombstoned,
                createdAt = r.createdAt.toEpochMilliseconds(),
                updatedAt = updatedAtOverrides[r.id.value] ?: r.updatedAt.toEpochMilliseconds(),
            )
        }

    override suspend fun upsertFromSync(row: RecurringSyncRow): Long {
        val syncId = requireNotNull(row.syncId)
        val existingId = syncIds.entries.firstOrNull { it.value == syncId }?.key
        return if (existingId == null) {
            val id = nextId++
            _rules.update { it + row.toDomain(id) }
            syncIds[id] = syncId
            id
        } else {
            _rules.update { list -> list.map { if (it.id.value == existingId) row.toDomain(existingId) else it } }
            existingId
        }
    }

    private fun RecurringSyncRow.toDomain(id: Long): RecurringTransaction {
        val rule = when (freqUnit) {
            "DAILY" -> RecurrenceRule.Daily(freqInterval)
            "WEEKLY" -> RecurrenceRule.Weekly(freqInterval, dayOfWeek ?: 1)
            "MONTHLY" -> RecurrenceRule.Monthly(
                interval = freqInterval,
                dayKind = if (useLastDay) MonthlyDayKind.LastDay else MonthlyDayKind.OnDay(dayOfMonth ?: 1),
            )
            else -> error("Unknown freq_unit: $freqUnit")
        }
        val end = when (endKind) {
            "UNLIMITED" -> EndCondition.Unlimited
            "COUNT" -> EndCondition.Count(endCount ?: error("COUNT end without count"))
            "UNTIL" -> EndCondition.Until(LocalDate.parse(endDate ?: error("UNTIL end without date")))
            else -> error("Unknown end_kind: $endKind")
        }
        return RecurringTransaction(
            id = RecurringTransactionId(id),
            type = TransactionType.valueOf(type),
            amount = Money(amountMinor, CurrencyCode(currency)),
            note = note,
            categoryId = CategoryId(categoryId),
            accountId = AccountId(accountId),
            paymentModeId = paymentModeId?.let { PaymentModeId(it) },
            startDate = LocalDate.parse(startDate),
            rule = rule,
            endCondition = end,
            lastMaterializedDate = lastMaterializedDate?.let { LocalDate.parse(it) },
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        )
    }

    private fun syncIdFor(id: Long): String = syncIds.getOrPut(id) { "sync-recurring-$id" }

    private fun idForSyncId(syncId: String): Long? {
        _rules.value.forEach { syncIdFor(it.id.value) }
        return syncIds.entries.firstOrNull { it.value == syncId }?.key
    }
}
