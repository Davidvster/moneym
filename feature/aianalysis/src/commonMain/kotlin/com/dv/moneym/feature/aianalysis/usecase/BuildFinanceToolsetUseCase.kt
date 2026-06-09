package com.dv.moneym.feature.aianalysis.usecase

import com.dv.moneym.core.ai.AiTool
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first

class BuildFinanceToolsetUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: AppClock,
) {
    operator fun invoke(year: Int, month: Int): List<AiTool> {
        val selected = YearMonth(year, month)
        return listOf(
            AiTool(
                name = "totals",
                description = "Income, expense and net totals for the selected period.",
                paramsSchema = "{}",
                invoke = { totals(year, month) },
            ),
            AiTool(
                name = "spendingByCategory",
                description = "Expense totals grouped by category for the selected period.",
                paramsSchema = "{}",
                invoke = { spendingByCategory(year, month) },
            ),
            AiTool(
                name = "topExpenses",
                description = "The largest individual expense transactions. Optional integer param 'n' (default 5).",
                paramsSchema = """{"n":"integer"}""",
                invoke = { params -> topExpenses(year, month, params["n"]?.toIntOrNull() ?: DEFAULT_TOP) },
            ),
            AiTool(
                name = "comparePreviousMonths",
                description = "Expense totals for each of the previous 3 months.",
                paramsSchema = "{}",
                invoke = { comparePreviousMonths(selected) },
            ),
            AiTool(
                name = "transactions",
                description = "Individual transactions for the selected period with date, type, " +
                    "category, amount and the user's note (their own description of what it was for). " +
                    "Optional string param 'q' keeps only transactions whose note or category " +
                    "contains the text.",
                paramsSchema = """{"q":"string"}""",
                invoke = { params -> transactions(year, month, params["q"]) },
            ),
        )
    }

    private suspend fun currency(): String {
        val accounts = accountRepository.observeAll().first()
        return (accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())
            ?.currency?.value ?: DEFAULT_CURRENCY
    }

    private suspend fun totals(year: Int, month: Int): String {
        val currency = currency()
        val txns = transactionRepository.observeByMonth(year, month).first()
        val income = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
        val expense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
        return "Income ${money(income, currency)}, Expense ${money(expense, currency)}, Net ${money(income - expense, currency)}"
    }

    private suspend fun spendingByCategory(year: Int, month: Int): String {
        val currency = currency()
        val categoryNames = categoryRepository.observeActive().first().associate { it.id to it.name }
        val txns = transactionRepository.observeByMonth(year, month).first()
        val grouped = txns.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (id, list) -> (categoryNames[id] ?: UNKNOWN_CATEGORY) to list.sumOf { it.amount.minorUnits } }
            .sortedByDescending { it.second }
        if (grouped.isEmpty()) return NO_DATA
        return grouped.joinToString("; ") { (name, amount) -> "$name ${money(amount, currency)}" }
    }

    private suspend fun topExpenses(year: Int, month: Int, n: Int): String {
        val currency = currency()
        val categoryNames = categoryRepository.observeActive().first().associate { it.id to it.name }
        val txns = transactionRepository.observeByMonth(year, month).first()
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount.minorUnits }
            .take(n.coerceAtLeast(1))
        if (txns.isEmpty()) return NO_DATA
        return txns.joinToString("; ") { tx ->
            val name = categoryNames[tx.categoryId] ?: UNKNOWN_CATEGORY
            val note = tx.note?.takeIf { it.isNotBlank() }?.let { " note: $it" } ?: ""
            "$name ${money(tx.amount.minorUnits, currency)} on ${tx.occurredOn}$note"
        }
    }

    private suspend fun transactions(year: Int, month: Int, query: String?): String {
        val currency = currency()
        val categoryNames = categoryRepository.observeActive().first().associate { it.id to it.name }
        val q = query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val txns = transactionRepository.observeByMonth(year, month).first()
            .sortedByDescending { it.occurredOn }
            .filter { tx ->
                if (q == null) return@filter true
                val name = (categoryNames[tx.categoryId] ?: UNKNOWN_CATEGORY).lowercase()
                name.contains(q) || tx.note?.lowercase()?.contains(q) == true
            }
            .take(MAX_TRANSACTIONS)
        if (txns.isEmpty()) return NO_DATA
        return txns.joinToString("; ") { tx ->
            val name = categoryNames[tx.categoryId] ?: UNKNOWN_CATEGORY
            val sign = if (tx.type == TransactionType.EXPENSE) "-" else "+"
            val note = tx.note?.takeIf { it.isNotBlank() }?.let { " note: $it" } ?: ""
            "${tx.occurredOn} $name $sign${money(tx.amount.minorUnits, currency)}$note"
        }
    }

    private suspend fun comparePreviousMonths(selected: YearMonth): String {
        val currency = currency()
        var cursor = selected.previous()
        val lines = mutableListOf<String>()
        repeat(RECENT_MONTHS) {
            val expense = transactionRepository.observeByMonth(cursor.year, cursor.monthNumber).first()
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount.minorUnits }
            lines += "$cursor ${money(expense, currency)}"
            cursor = cursor.previous()
        }
        return lines.joinToString("; ")
    }

    private fun money(minorUnits: Long, currency: String): String =
        formatMinor(minorUnits, currency)

    private companion object {
        const val DEFAULT_TOP = 5
        const val MAX_TRANSACTIONS = 50
        const val RECENT_MONTHS = 3
        const val DEFAULT_CURRENCY = "EUR"
        const val UNKNOWN_CATEGORY = "Other"
        const val NO_DATA = "No data."
    }
}
