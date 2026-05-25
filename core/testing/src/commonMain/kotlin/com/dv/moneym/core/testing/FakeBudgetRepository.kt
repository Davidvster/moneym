package com.dv.moneym.core.testing

import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.data.budgets.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeBudgetRepository : BudgetRepository {
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    private var nextId = 1L

    val budgets: List<Budget> get() = _budgets.value

    fun addAll(budgets: List<Budget>) = _budgets.update { it + budgets }

    override fun observeAll(): Flow<List<Budget>> = _budgets

    override suspend fun getById(id: BudgetId): Budget? = _budgets.value.find { it.id == id }

    override suspend fun insert(budget: Budget): BudgetId {
        val id = BudgetId(nextId++)
        _budgets.update { it + budget.copy(id = id) }
        return id
    }

    override suspend fun update(budget: Budget) {
        _budgets.update { list -> list.map { if (it.id == budget.id) budget else it } }
    }

    override suspend fun delete(id: BudgetId) {
        _budgets.update { list -> list.filter { it.id != id } }
    }
}
