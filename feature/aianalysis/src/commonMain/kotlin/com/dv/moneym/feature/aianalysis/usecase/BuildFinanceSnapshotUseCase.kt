package com.dv.moneym.feature.aianalysis.usecase

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first

class BuildFinanceSnapshotUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(year: Int, month: Int): String {
        val selected = YearMonth(year, month)
        val accounts = accountRepository.observeAll().first()
        val categories = categoryRepository.observeActive().first()
        val budgets = budgetRepository.observeAll().first()

        val currency = (accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())
            ?.currency?.value ?: DEFAULT_CURRENCY

        val selectedTxns = transactionRepository.observeByMonth(year, month).first()
        val income = selectedTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
        val expense = selectedTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }

        val categoryNames = categories.associate { it.id to it.name }
        val topCategories = topExpenseCategories(selectedTxns, categoryNames)

        val budgetLines = budgetStatus(budgets, selectedTxns, selected, categoryNames, currency)

        val historyLines = recentHistory(selected, currency)

        return buildString {
            appendLine("Currency: $currency")
            appendLine("Period: $selected")
            appendLine("Income: ${money(income, currency)}")
            appendLine("Expense: ${money(expense, currency)}")
            appendLine("Net: ${money(income - expense, currency)}")
            if (topCategories.isNotEmpty()) {
                appendLine("Top expense categories:")
                topCategories.forEach { (name, amount) ->
                    appendLine("- $name: ${money(amount, currency)}")
                }
            }
            if (budgetLines.isNotEmpty()) {
                appendLine("Budgets:")
                budgetLines.forEach { appendLine("- $it") }
            }
            appendLine("Recent history (expense per month):")
            historyLines.forEach { appendLine("- $it") }
        }.trimEnd()
    }

    private fun topExpenseCategories(
        txns: List<Transaction>,
        categoryNames: Map<com.dv.moneym.core.model.CategoryId, String>,
    ): List<Pair<String, Long>> =
        txns.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (id, list) -> (categoryNames[id] ?: UNKNOWN_CATEGORY) to list.sumOf { it.amount.minorUnits } }
            .sortedByDescending { it.second }
            .take(MAX_TOP_CATEGORIES)

    private fun budgetStatus(
        budgets: List<Budget>,
        selectedTxns: List<Transaction>,
        selected: YearMonth,
        categoryNames: Map<com.dv.moneym.core.model.CategoryId, String>,
        currency: String,
    ): List<String> =
        budgets.filter { it.isActiveIn(selected) }.map { budget ->
            val spent = selectedTxns
                .filter { it.type == TransactionType.EXPENSE }
                .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                .sumOf { it.amount.minorUnits }
            val scope = budget.categoryId?.let { categoryNames[it] } ?: ALL_CATEGORIES
            "${budget.name} ($scope): ${money(spent, currency)} / ${money(budget.amount.minorUnits, currency)}"
        }

    private suspend fun recentHistory(selected: YearMonth, currency: String): List<String> {
        var cursor = selected.previous()
        val lines = mutableListOf<String>()
        repeat(RECENT_MONTHS) {
            val txns = transactionRepository.observeByMonth(cursor.year, cursor.monthNumber).first()
            val monthExpense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
            lines += "$cursor: ${money(monthExpense, currency)}"
            cursor = cursor.previous()
        }
        return lines
    }

    private fun money(minorUnits: Long, currency: String): String =
        formatMinor(minorUnits, currency)

    private companion object {
        const val MAX_TOP_CATEGORIES = 5
        const val RECENT_MONTHS = 3
        const val DEFAULT_CURRENCY = "EUR"
        const val UNKNOWN_CATEGORY = "Other"
        const val ALL_CATEGORIES = "all"
    }
}
