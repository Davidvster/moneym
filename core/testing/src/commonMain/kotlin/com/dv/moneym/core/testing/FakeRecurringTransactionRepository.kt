package com.dv.moneym.core.testing

import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
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
}
