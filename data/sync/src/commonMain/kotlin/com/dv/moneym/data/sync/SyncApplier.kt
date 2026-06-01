package com.dv.moneym.data.sync

import co.touchlab.kermit.Logger
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.accounts.AccountSyncRow
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.budgets.BudgetSyncRow
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.categories.CategorySyncRow
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.PaymentModeSyncRow
import com.dv.moneym.data.transactions.RecurringSyncRow
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.TransactionSyncRow

/**
 * Applies a reconciled [SyncSnapshot] (adds + winning edits, no tombstones) to the local
 * repositories in FK-safe order: paymentModes -> categories -> accounts -> recurring -> budgets
 * -> transactions. FK syncIds are resolved to local Long PKs via maps seeded from each parent
 * repository's current export and updated as parents are applied. A row whose required FK can't
 * be resolved is skipped and logged; a nullable FK that is absent resolves to null.
 */
class SyncApplier(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val budgetRepository: BudgetRepository,
) {

    suspend fun apply(toApply: SyncSnapshot) {
        val accountIds = accountRepository.exportForSync().associate { it.syncId!! to it.id }.toMutableMap()
        val categoryIds = categoryRepository.exportForSync().associate { it.syncId!! to it.id }.toMutableMap()
        val paymentModeIds = paymentModeRepository.exportForSync().associate { it.syncId!! to it.id }.toMutableMap()
        val recurringIds = recurringTransactionRepository.exportForSync().associate { it.syncId!! to it.id }.toMutableMap()

        for (pm in toApply.paymentModes) {
            val id = paymentModeRepository.upsertFromSync(
                PaymentModeSyncRow(
                    id = 0,
                    syncId = pm.syncId,
                    name = pm.name,
                    deleted = pm.deleted,
                    createdAt = pm.createdAt,
                    updatedAt = pm.updatedAt,
                )
            )
            paymentModeIds[pm.syncId] = id
        }

        for (cat in toApply.categories) {
            val id = categoryRepository.upsertFromSync(
                CategorySyncRow(
                    id = 0,
                    syncId = cat.syncId,
                    name = cat.name,
                    iconKey = cat.iconKey,
                    colorHex = cat.colorHex,
                    isUserCreated = cat.isUserCreated,
                    archived = cat.archived,
                    categoryType = cat.categoryType,
                    deleted = cat.deleted,
                    createdAt = cat.createdAt,
                    updatedAt = cat.updatedAt,
                )
            )
            categoryIds[cat.syncId] = id
        }

        for (acc in toApply.accounts) {
            val id = accountRepository.upsertFromSync(
                AccountSyncRow(
                    id = 0,
                    syncId = acc.syncId,
                    name = acc.name,
                    type = acc.type,
                    currency = acc.currency,
                    isDefault = acc.isDefault,
                    archived = acc.archived,
                    colorHex = acc.colorHex,
                    deleted = acc.deleted,
                    createdAt = acc.createdAt,
                    updatedAt = acc.updatedAt,
                )
            )
            accountIds[acc.syncId] = id
        }

        for (rec in toApply.recurring) {
            val categoryId = categoryIds[rec.categorySyncId]
                ?: skip("recurring", rec.syncId, "category", rec.categorySyncId)
            val accountId = accountIds[rec.accountSyncId]
                ?: skip("recurring", rec.syncId, "account", rec.accountSyncId)
            if (categoryId == null || accountId == null) continue
            val paymentModeId = rec.paymentModeSyncId?.let { pmSyncId ->
                paymentModeIds[pmSyncId] ?: run {
                    skip("recurring", rec.syncId, "paymentMode", pmSyncId); return@let MISSING
                }
            }
            if (paymentModeId == MISSING) continue
            val id = recurringTransactionRepository.upsertFromSync(
                RecurringSyncRow(
                    id = 0,
                    syncId = rec.syncId,
                    type = rec.type,
                    amountMinor = rec.amountMinor,
                    currency = rec.currency,
                    note = rec.note,
                    categoryId = categoryId,
                    accountId = accountId,
                    paymentModeId = paymentModeId,
                    startDate = rec.startDate,
                    freqUnit = rec.freqUnit,
                    freqInterval = rec.freqInterval,
                    dayOfWeek = rec.dayOfWeek,
                    dayOfMonth = rec.dayOfMonth,
                    useLastDay = rec.useLastDay,
                    endKind = rec.endKind,
                    endCount = rec.endCount,
                    endDate = rec.endDate,
                    lastMaterializedDate = rec.lastMaterializedDate,
                    deleted = rec.deleted,
                    createdAt = rec.createdAt,
                    updatedAt = rec.updatedAt,
                )
            )
            recurringIds[rec.syncId] = id
        }

        for (budget in toApply.budgets) {
            val accountId = accountIds[budget.accountSyncId]
                ?: skip("budget", budget.syncId, "account", budget.accountSyncId)
            if (accountId == null) continue
            val categoryId = budget.categorySyncId?.let { catSyncId ->
                categoryIds[catSyncId] ?: run {
                    skip("budget", budget.syncId, "category", catSyncId); return@let MISSING
                }
            }
            if (categoryId == MISSING) continue
            budgetRepository.upsertFromSync(
                BudgetSyncRow(
                    id = 0,
                    syncId = budget.syncId,
                    name = budget.name,
                    amountMinor = budget.amountMinor,
                    currency = budget.currency,
                    categoryId = categoryId,
                    accountId = accountId,
                    periodType = budget.periodType,
                    startYearMonth = budget.startYearMonth,
                    recurringMonths = budget.recurringMonths,
                    deleted = budget.deleted,
                    createdAt = budget.createdAt,
                    updatedAt = budget.updatedAt,
                )
            )
        }

        for (tx in toApply.transactions) {
            val categoryId = categoryIds[tx.categorySyncId]
                ?: skip("transaction", tx.syncId, "category", tx.categorySyncId)
            val accountId = accountIds[tx.accountSyncId]
                ?: skip("transaction", tx.syncId, "account", tx.accountSyncId)
            if (categoryId == null || accountId == null) continue
            val paymentModeId = tx.paymentModeSyncId?.let { pmSyncId ->
                paymentModeIds[pmSyncId] ?: run {
                    skip("transaction", tx.syncId, "paymentMode", pmSyncId); return@let MISSING
                }
            }
            if (paymentModeId == MISSING) continue
            val recurringId = tx.recurringSyncId?.let { recSyncId ->
                recurringIds[recSyncId] ?: run {
                    skip("transaction", tx.syncId, "recurring", recSyncId); return@let MISSING
                }
            }
            if (recurringId == MISSING) continue
            transactionRepository.upsertFromSync(
                TransactionSyncRow(
                    id = 0,
                    syncId = tx.syncId,
                    type = tx.type,
                    amountMinor = tx.amountMinor,
                    currency = tx.currency,
                    occurredOn = tx.occurredOn,
                    note = tx.note,
                    categoryId = categoryId,
                    accountId = accountId,
                    paymentModeId = paymentModeId,
                    recurringId = recurringId,
                    deleted = tx.deleted,
                    createdAt = tx.createdAt,
                    updatedAt = tx.updatedAt,
                )
            )
        }
    }

    private fun skip(table: String, syncId: String, fk: String, fkSyncId: String): Long? {
        logger.w { "Sync apply: skipping $table syncId=$syncId — unresolved $fk FK syncId=$fkSyncId" }
        return null
    }

    private companion object {
        val logger = Logger.withTag("SyncApplier")

        // Sentinel marking a present-but-unresolvable nullable FK so the row is skipped
        // (as opposed to a genuinely absent nullable FK which resolves to null).
        const val MISSING = Long.MIN_VALUE
    }
}
