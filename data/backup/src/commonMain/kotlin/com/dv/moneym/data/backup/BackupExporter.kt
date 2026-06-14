package com.dv.moneym.data.backup

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.SecurityPrefs
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class BackupExporter(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val appSettings: AppSettings,
) {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    suspend fun exportToJson(startDate: LocalDate? = null, endDate: LocalDate? = null): String {
        val categories = categoryRepository.observeAll().first()
        val accounts = accountRepository.observeAll().first()
        val transactions = transactionRepository.observeAll().first()
            .filter {
                (startDate == null || it.occurredOn >= startDate) &&
                    (endDate == null || it.occurredOn <= endDate)
            }
        val budgets = budgetRepository.observeAll().first()
        val recurring = recurringTransactionRepository.observeAll().first()
        val currency = accounts.firstOrNull { it.isDefault }?.currency?.value ?: "USD"
        val lockSeconds = appSettings.getInt(
            SecurityPrefs.BACKGROUND_LOCK_SECONDS,
            SecurityPrefs.DEFAULT_LOCK_SECONDS
        )

        val backup = BackupDto(
            moneym = BackupMetaDto(
                exportedAt = Clock.System.now().toString(),
                defaultCurrency = currency,
            ),
            categories = categories.map { it.toDto() },
            accounts = accounts.map { it.toDto() },
            transactions = transactions.map { it.toDto() },
            budgets = budgets.map { it.toDto() },
            recurringTransactions = recurring.map { it.toDto() },
            settings = BackupSettingsDto(
                backgroundLockSeconds = lockSeconds,
                defaultCurrency = currency,
            ),
        )
        return json.encodeToString(backup)
    }

    suspend fun exportToCsv(startDate: LocalDate? = null, endDate: LocalDate? = null): String {
        val transactions = transactionRepository.observeAll().first()
            .filter {
                (startDate == null || it.occurredOn >= startDate) &&
                    (endDate == null || it.occurredOn <= endDate)
            }
        val categories = categoryRepository.observeAll().first().associateBy { it.id }
        val accounts = accountRepository.observeAll().first().associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("date,type,amount,currency,category,account,note")
        transactions.forEach { txn ->
            val catName =
                (categories[txn.categoryId]?.name ?: "").replace(",", ";").replace("\"", "\"\"")
            val accName =
                (accounts[txn.accountId]?.name ?: "").replace(",", ";").replace("\"", "\"\"")
            val note = (txn.note ?: "").replace("\"", "\"\"")
            val amount = "${txn.amount.minorUnits / 100}.${
                (txn.amount.minorUnits % 100).toString().padStart(2, '0')
            }"
            sb.appendLine("${txn.occurredOn},${txn.type.name},$amount,${txn.amount.currency.value},\"$catName\",\"$accName\",\"$note\"")
        }
        return sb.toString()
    }
}
