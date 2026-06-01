package com.dv.moneym.core.testing

import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.data.transactions.RecurringSyncRow
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeRecurringTransactionRepository : RecurringTransactionRepository {
    private val _rules = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private var nextId = 1L

    val rules: List<RecurringTransaction> get() = _rules.value

    fun addAll(rules: List<RecurringTransaction>) = _rules.update { it + rules }

    override fun observeAll(): Flow<List<RecurringTransaction>> = _rules

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
        _rules.update { list -> list.filter { it.id != id } }
    }

    override suspend fun deleteAll() {
        _rules.value = emptyList()
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
                syncId = "sync-recurring-${r.id.value}",
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
                deleted = false,
                createdAt = r.createdAt.toEpochMilliseconds(),
                updatedAt = r.updatedAt.toEpochMilliseconds(),
            )
        }
}
