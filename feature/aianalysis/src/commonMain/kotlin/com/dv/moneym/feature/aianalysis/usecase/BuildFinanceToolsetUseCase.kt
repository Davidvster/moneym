package com.dv.moneym.feature.aianalysis.usecase

import com.dv.moneym.core.ai.AiTool
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

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
            AiTool(
                name = "dataRange",
                description = "The full span of stored data: earliest and latest transaction date " +
                    "and the total transaction count. Call this first when a question is not about " +
                    "the selected period, to learn what date ranges are available to search.",
                paramsSchema = "{}",
                invoke = { dataRange() },
            ),
            AiTool(
                name = "categories",
                description = "All active categories with their type (income or expense). Use to map " +
                    "a category name the user mentions to what exists in the data.",
                paramsSchema = "{}",
                invoke = { categories() },
            ),
            AiTool(
                name = "accounts",
                description = "All accounts with type, currency and which one is the default.",
                paramsSchema = "{}",
                invoke = { accounts() },
            ),
            AiTool(
                name = "searchTransactions",
                description = "Search ALL transactions across the entire history, not just the " +
                    "selected period. Every param is optional: 'q' matches note or category text; " +
                    "'from' and 'to' are inclusive dates as yyyy-MM-dd; 'type' is income or expense; " +
                    "'limit' caps the result count (default 50, max 200). Returns date, type, " +
                    "category, amount and note, newest first.",
                paramsSchema = """{"q":"string","from":"string","to":"string","type":"string","limit":"integer"}""",
                invoke = { params -> searchTransactions(params) },
            ),
            AiTool(
                name = "totalsForRange",
                description = "Income, expense and net totals across ALL history within an optional " +
                    "date range. 'from' and 'to' are inclusive dates as yyyy-MM-dd; omit either to " +
                    "leave that end open.",
                paramsSchema = """{"from":"string","to":"string"}""",
                invoke = { params -> totalsForRange(params) },
            ),
            AiTool(
                name = "budgets",
                description = "Budgets active in the selected period, each with its category scope, " +
                    "the amount spent so far and the budget limit.",
                paramsSchema = "{}",
                invoke = { budgets(year, month, selected) },
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
        return txns.joinToString("; ") { formatTxn(it, categoryNames, currency) }
    }

    private suspend fun dataRange(): String {
        val earliest = transactionRepository.getEarliestTransactionDate()
        val latest = transactionRepository.getLatestTransactionDate()
        if (earliest == null || latest == null) return NO_DATA
        val count = transactionRepository.observeAll().first().size
        return "Transactions span $earliest to $latest, $count total."
    }

    private suspend fun categories(): String {
        val cats = categoryRepository.observeActive().first()
        if (cats.isEmpty()) return NO_DATA
        return cats.joinToString("; ") { "${it.name} (${it.type.name.lowercase()})" }
    }

    private suspend fun accounts(): String {
        val accs = accountRepository.observeAll().first()
        if (accs.isEmpty()) return NO_DATA
        return accs.joinToString("; ") { acc ->
            val default = if (acc.isDefault) ", default" else ""
            "${acc.name} (${acc.type.name.lowercase()}, ${acc.currency.value}$default)"
        }
    }

    private suspend fun searchTransactions(params: Map<String, String>): String {
        val currency = currency()
        val categoryNames = categoryRepository.observeAll().first().associate { it.id to it.name }
        val q = params["q"]?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val from = params["from"]?.let { parseDate(it) }
        val to = params["to"]?.let { parseDate(it) }
        val type = params["type"]?.let { parseType(it) }
        val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, MAX_SEARCH) ?: MAX_TRANSACTIONS
        val txns = transactionRepository.observeAll().first()
            .asSequence()
            .filter { from == null || it.occurredOn >= from }
            .filter { to == null || it.occurredOn <= to }
            .filter { type == null || it.type == type }
            .filter { tx ->
                if (q == null) return@filter true
                val name = (categoryNames[tx.categoryId] ?: UNKNOWN_CATEGORY).lowercase()
                name.contains(q) || tx.note?.lowercase()?.contains(q) == true
            }
            .sortedByDescending { it.occurredOn }
            .take(limit)
            .toList()
        if (txns.isEmpty()) return NO_DATA
        return txns.joinToString("; ") { formatTxn(it, categoryNames, currency) }
    }

    private suspend fun totalsForRange(params: Map<String, String>): String {
        val currency = currency()
        val from = params["from"]?.let { parseDate(it) }
        val to = params["to"]?.let { parseDate(it) }
        val txns = transactionRepository.observeAll().first()
            .filter { from == null || it.occurredOn >= from }
            .filter { to == null || it.occurredOn <= to }
        val income = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
        val expense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
        val label = "${from ?: "start"}..${to ?: "now"}"
        return "$label: Income ${money(income, currency)}, Expense ${money(expense, currency)}, Net ${money(income - expense, currency)}"
    }

    private suspend fun budgets(year: Int, month: Int, selected: YearMonth): String {
        val currency = currency()
        val categoryNames = categoryRepository.observeActive().first().associate { it.id to it.name }
        val active = budgetRepository.observeAll().first().filter { it.isActiveIn(selected) }
        if (active.isEmpty()) return NO_DATA
        val expenses = transactionRepository.observeByMonth(year, month).first()
            .filter { it.type == TransactionType.EXPENSE }
        return active.joinToString("; ") { budget ->
            val spent = expenses
                .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                .sumOf { it.amount.minorUnits }
            val scope = budget.categoryId?.let { categoryNames[it] } ?: ALL_CATEGORIES
            "${budget.name} ($scope): ${money(spent, currency)} / ${money(budget.amount.minorUnits, currency)}"
        }
    }

    private fun formatTxn(tx: Transaction, categoryNames: Map<CategoryId, String>, currency: String): String {
        val name = categoryNames[tx.categoryId] ?: UNKNOWN_CATEGORY
        val sign = if (tx.type == TransactionType.EXPENSE) "-" else "+"
        val note = tx.note?.takeIf { it.isNotBlank() }?.let { " note: $it" } ?: ""
        return "${tx.occurredOn} $name $sign${money(tx.amount.minorUnits, currency)}$note"
    }

    private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw.trim()) }.getOrNull()

    private fun parseType(raw: String): TransactionType? = when (raw.trim().lowercase()) {
        "income" -> TransactionType.INCOME
        "expense" -> TransactionType.EXPENSE
        else -> null
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
        const val MAX_SEARCH = 200
        const val RECENT_MONTHS = 3
        const val DEFAULT_CURRENCY = "EUR"
        const val UNKNOWN_CATEGORY = "Other"
        const val ALL_CATEGORIES = "all"
        const val NO_DATA = "No data."
    }
}
