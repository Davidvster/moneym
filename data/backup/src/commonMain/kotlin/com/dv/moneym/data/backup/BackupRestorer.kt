package com.dv.moneym.data.backup

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.security.SecurityPrefs
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.overview.OverviewRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.serialization.json.Json

class BackupRestorer(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val overviewRepository: OverviewRepository,
    private val appSettings: AppSettings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun restore(jsonStr: String) {
        val backup = json.decodeFromString<BackupDto>(jsonStr)

        transactionRepository.deleteAll()
        accountRepository.deleteAll()
        categoryRepository.deleteAll()

        val catIdMap = mutableMapOf<Long, CategoryId>()
        backup.categories.forEach { dto ->
            catIdMap[dto.id] = categoryRepository.insert(dto.toDomain())
        }

        val accIdMap = mutableMapOf<Long, AccountId>()
        backup.accounts.forEach { dto ->
            accIdMap[dto.id] = accountRepository.insert(dto.toDomain())
        }

        backup.transactions.forEach { dto ->
            val catId = catIdMap[dto.categoryId] ?: return@forEach
            val accId = accIdMap[dto.accountId] ?: return@forEach
            transactionRepository.upsert(
                dto.toDomain(
                    idOverride = UNSAVED_TRANSACTION_ID,
                    catIdOverride = catId,
                    accIdOverride = accId,
                )
            )
        }

        backup.budgets.forEach { dto ->
            val accId = accIdMap[dto.accountId] ?: AccountId(0)
            budgetRepository.insert(dto.toDomain(idOverride = BudgetId(0), accIdOverride = accId))
        }

        overviewRepository.replaceLayout(backup.overviewLayout.toDomain().blocks)
        overviewRepository.replaceAiWidgets(backup.overviewAiWidgets.map { it.toDomain(idOverride = 0) })

        backup.settings.let { settings ->
            appSettings.putInt(
                SecurityPrefs.BACKGROUND_LOCK_SECONDS,
                settings.backgroundLockSeconds
            )
        }
    }
}
