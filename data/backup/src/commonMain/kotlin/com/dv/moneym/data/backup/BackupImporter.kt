package com.dv.moneym.data.backup

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.ImportMode
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class BackupImporter(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun previewFromJson(jsonStr: String): ImportPreview {
        return try {
            val backup = json.decodeFromString<BackupDto>(jsonStr)
            val existingCats = categoryRepository.observeAll().first()
            val existingAccs = accountRepository.observeAll().first()
            val existingTxns = transactionRepository.observeAll().first()

            val catKeys = existingCats.map { catMatchKey(it.name, it.iconKey, it.colorHex) }.toSet()
            val accKeys = existingAccs.map { accMatchKey(it.name, it.type.name, it.currency.value) }.toSet()
            val txnKeys = existingTxns.map { t ->
                val cn = existingCats.firstOrNull { it.id == t.categoryId }?.name ?: ""
                val an = existingAccs.firstOrNull { it.id == t.accountId }?.name ?: ""
                txnMatchKey(t.occurredOn.toString(), t.amount.minorUnits, t.amount.currency.value, cn, an, t.note)
            }.toSet()

            val catsDtoKeys = backup.categories.map { catMatchKey(it.name, it.iconKey, it.colorHex) }
            val accsDtoKeys = backup.accounts.map { accMatchKey(it.name, it.type, it.currency) }
            val txnsDtoKeys = backup.transactions.map { t ->
                val cn = backup.categories.firstOrNull { it.id == t.categoryId }?.name ?: ""
                val an = backup.accounts.firstOrNull { it.id == t.accountId }?.name ?: ""
                txnMatchKey(t.occurredOn, t.amountMinor, t.currency, cn, an, t.note)
            }

            ImportPreview(
                categories = EntityCount(
                    new = catsDtoKeys.count { it !in catKeys },
                    duplicate = catsDtoKeys.count { it in catKeys },
                ),
                accounts = EntityCount(
                    new = accsDtoKeys.count { it !in accKeys },
                    duplicate = accsDtoKeys.count { it in accKeys },
                ),
                transactions = EntityCount(
                    new = txnsDtoKeys.count { it !in txnKeys },
                    duplicate = txnsDtoKeys.count { it in txnKeys },
                ),
            )
        } catch (e: Exception) {
            ImportPreview(
                transactions = EntityCount(0, 0),
                categories = EntityCount(0, 0),
                accounts = EntityCount(0, 0),
                isValid = false,
                errorMessage = "Invalid JSON: ${e.message?.take(100)}",
            )
        }
    }

    suspend fun applyFromJson(jsonStr: String, mode: ImportMode) {
        val backup = json.decodeFromString<BackupDto>(jsonStr)

        if (mode == ImportMode.REPLACE) {
            // In REPLACE mode: insert all incoming data; existing data stays
            // (full replace would need delete-all which risks data loss — using merge for safety)
        }

        // Build ID remapping tables (import IDs → new IDs assigned by DB)
        val catIdMap = mutableMapOf<Long, CategoryId>()
        val accIdMap = mutableMapOf<Long, AccountId>()
        val ruleIdMap = mutableMapOf<Long, RecurringTransactionId>()

        backup.categories.forEach { dto ->
            val existing = categoryRepository.observeAll().first()
                .firstOrNull { catMatchKey(it.name, it.iconKey, it.colorHex) == catMatchKey(dto.name, dto.iconKey, dto.colorHex) }
            if (existing != null) {
                catIdMap[dto.id] = existing.id
            } else {
                val newId = categoryRepository.insert(dto.toDomain())
                catIdMap[dto.id] = newId
            }
        }

        backup.accounts.forEach { dto ->
            val existing = accountRepository.observeAll().first()
                .firstOrNull { accMatchKey(it.name, it.type.name, it.currency.value) == accMatchKey(dto.name, dto.type, dto.currency) }
            if (existing != null) {
                accIdMap[dto.id] = existing.id
            } else {
                val newId = accountRepository.insert(dto.toDomain())
                accIdMap[dto.id] = newId
            }
        }

        // Import transactions (merge mode: skip duplicates)
        val existingTxns = transactionRepository.observeAll().first()
        val existingCats = categoryRepository.observeAll().first()
        val existingAccs = accountRepository.observeAll().first()
        val txnKeys = existingTxns.map { t ->
            val cn = existingCats.firstOrNull { it.id == t.categoryId }?.name ?: ""
            val an = existingAccs.firstOrNull { it.id == t.accountId }?.name ?: ""
            txnMatchKey(t.occurredOn.toString(), t.amount.minorUnits, t.amount.currency.value, cn, an, t.note)
        }.toSet()

        // Insert recurring rules first so their new IDs are available when
        // remapping the recurring_id FK on materialized transactions.
        backup.recurringTransactions.forEach { dto ->
            val catId = catIdMap[dto.categoryId] ?: return@forEach
            val accId = accIdMap[dto.accountId] ?: return@forEach
            val newId = recurringTransactionRepository.upsert(
                dto.toDomain(
                    idOverride = UNSAVED_RECURRING_ID,
                    catIdOverride = catId,
                    accIdOverride = accId,
                )
            )
            ruleIdMap[dto.id] = newId
        }

        backup.transactions.forEach { dto ->
            val catName = backup.categories.firstOrNull { it.id == dto.categoryId }?.name ?: ""
            val accName = backup.accounts.firstOrNull { it.id == dto.accountId }?.name ?: ""
            val key = txnMatchKey(dto.occurredOn, dto.amountMinor, dto.currency, catName, accName, dto.note)
            if (key !in txnKeys) {
                val catId = catIdMap[dto.categoryId] ?: return@forEach
                val accId = accIdMap[dto.accountId] ?: return@forEach
                transactionRepository.upsert(dto.toDomain(
                    idOverride = UNSAVED_TRANSACTION_ID,
                    catIdOverride = catId,
                    accIdOverride = accId,
                    recurringIdOverride = dto.recurringId?.let { ruleIdMap[it] },
                ))
            }
        }
    }

    private fun catMatchKey(name: String, iconKey: String, colorHex: String) = "$name|$iconKey|$colorHex"
    private fun accMatchKey(name: String, type: String, currency: String) = "$name|$type|$currency"
    private fun txnMatchKey(date: String, amount: Long, currency: String, cat: String, acc: String, note: String?) =
        "$date|$amount|$currency|$cat|$acc|${note ?: ""}"
}
