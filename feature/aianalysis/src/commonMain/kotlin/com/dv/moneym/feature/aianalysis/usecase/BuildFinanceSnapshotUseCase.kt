package com.dv.moneym.feature.aianalysis.usecase

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.CategoryId
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

        val categoryNames = categories.associate { it.id to it.name }

        // Snapshot covers the whole selected year so the model can answer anything across it,
        // not only the month the user happens to be viewing.
        val yearTxns = transactionRepository.observeAll().first()
            .filter { it.occurredOn.year == year }
        val selectedTxns = yearTxns.filter { it.occurredOn.monthNumber == month }

        val yearIncome = yearTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
        val yearExpense = yearTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }

        val monthlyLines = monthlyBreakdown(yearTxns, currency)
        val topCategories = topExpenseCategories(yearTxns, categoryNames)
        val budgetLines = budgetStatus(budgets, selectedTxns, selected, categoryNames, currency)
        val noteLines = transactionNotes(yearTxns, categoryNames, currency)

        return buildString {
            appendLine("Currency: $currency")
            appendLine("Year: $year (the user is currently viewing $selected)")
            appendLine("Year income: ${money(yearIncome, currency)}")
            appendLine("Year expense: ${money(yearExpense, currency)}")
            appendLine("Year net: ${money(yearIncome - yearExpense, currency)}")
            if (monthlyLines.isNotEmpty()) {
                appendLine("Monthly breakdown (income / expense / net):")
                monthlyLines.forEach { appendLine("- $it") }
            }
            if (topCategories.isNotEmpty()) {
                appendLine("Top expense categories (year):")
                topCategories.forEach { (name, amount) ->
                    appendLine("- $name: ${money(amount, currency)}")
                }
            }
            if (budgetLines.isNotEmpty()) {
                appendLine("Budgets active in $selected:")
                budgetLines.forEach { appendLine("- $it") }
            }
            if (noteLines.isNotEmpty()) {
                appendLine("Transactions with notes (the user's own description of what each was for):")
                noteLines.forEach { appendLine("- $it") }
            }
        }.trimEnd()
    }

    private fun monthlyBreakdown(yearTxns: List<Transaction>, currency: String): List<String> =
        yearTxns.groupBy { it.occurredOn.monthNumber }
            .entries
            .sortedBy { it.key }
            .map { (monthNumber, list) ->
                val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
                val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
                val ym = YearMonth(list.first().occurredOn.year, monthNumber)
                "$ym: ${money(income, currency)} / ${money(expense, currency)} / ${money(income - expense, currency)}"
            }

    private fun topExpenseCategories(
        txns: List<Transaction>,
        categoryNames: Map<CategoryId, String>,
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
        categoryNames: Map<CategoryId, String>,
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

    private fun transactionNotes(
        txns: List<Transaction>,
        categoryNames: Map<CategoryId, String>,
        currency: String,
    ): List<String> =
        txns.filter { !it.note.isNullOrBlank() }
            .sortedByDescending { it.amount.minorUnits }
            .take(MAX_NOTES)
            .map { tx ->
                val name = categoryNames[tx.categoryId] ?: UNKNOWN_CATEGORY
                val sign = if (tx.type == TransactionType.EXPENSE) "-" else "+"
                "${tx.occurredOn} $name $sign${money(tx.amount.minorUnits, currency)}: ${tx.note}"
            }

    private fun money(minorUnits: Long, currency: String): String =
        formatMinor(minorUnits, currency)

    private companion object {
        const val MAX_TOP_CATEGORIES = 8
        const val MAX_NOTES = 40
        const val DEFAULT_CURRENCY = "EUR"
        const val UNKNOWN_CATEGORY = "Other"
        const val ALL_CATEGORIES = "all"
    }
}
