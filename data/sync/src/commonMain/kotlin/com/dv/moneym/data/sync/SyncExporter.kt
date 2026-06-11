package com.dv.moneym.data.sync

import co.touchlab.kermit.Logger
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlin.time.Clock

class SyncExporter(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val deviceIdentity: DeviceIdentity,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    suspend fun export(): SyncSnapshot {
        val accountRows = accountRepository.exportForSync()
        val categoryRows = categoryRepository.exportForSync()
        val paymentModeRows = paymentModeRepository.exportForSync()
        val transactionRows = transactionRepository.exportForSync()
        val recurringRows = recurringTransactionRepository.exportForSync()
        val budgetRows = budgetRepository.exportForSync()

        val accMap = accountRows.idToSyncId { it.id to it.syncId }
        val catMap = categoryRows.idToSyncId { it.id to it.syncId }
        val pmMap = paymentModeRows.idToSyncId { it.id to it.syncId }
        val recMap = recurringRows.idToSyncId { it.id to it.syncId }

        val accounts = accountRows.mapNotNull { row ->
            val syncId = row.syncId ?: return@mapNotNull skip("account", row.id)
            SyncAccount(
                syncId = syncId,
                name = row.name,
                type = row.type,
                currency = row.currency,
                isDefault = row.isDefault,
                archived = row.archived,
                colorHex = row.colorHex,
                deleted = row.deleted,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }

        val categories = categoryRows.mapNotNull { row ->
            val syncId = row.syncId ?: return@mapNotNull skip("category", row.id)
            SyncCategory(
                syncId = syncId,
                name = row.name,
                iconKey = row.iconKey,
                colorHex = row.colorHex,
                isUserCreated = row.isUserCreated,
                archived = row.archived,
                categoryType = row.categoryType,
                deleted = row.deleted,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }

        val paymentModes = paymentModeRows.mapNotNull { row ->
            val syncId = row.syncId ?: return@mapNotNull skip("paymentMode", row.id)
            SyncPaymentMode(
                syncId = syncId,
                name = row.name,
                deleted = row.deleted,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }

        val transactions = transactionRows.mapNotNull { row ->
            val syncId = row.syncId ?: return@mapNotNull skip("transaction", row.id)
            val categorySyncId = catMap[row.categoryId] ?: return@mapNotNull skipFk("transaction", row.id, "category", row.categoryId)
            val accountSyncId = accMap[row.accountId] ?: return@mapNotNull skipFk("transaction", row.id, "account", row.accountId)
            val paymentModeSyncId = row.paymentModeId?.let { pmId ->
                pmMap[pmId] ?: return@mapNotNull skipFk("transaction", row.id, "paymentMode", pmId)
            }
            val recurringSyncId = row.recurringId?.let { recId ->
                recMap[recId] ?: return@mapNotNull skipFk("transaction", row.id, "recurring", recId)
            }
            SyncTransaction(
                syncId = syncId,
                type = row.type,
                amountMinor = row.amountMinor,
                currency = row.currency,
                occurredOn = row.occurredOn,
                note = row.note,
                categorySyncId = categorySyncId,
                accountSyncId = accountSyncId,
                paymentModeSyncId = paymentModeSyncId,
                recurringSyncId = recurringSyncId,
                deleted = row.deleted,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                externalId = row.externalId,
            )
        }

        val recurring = recurringRows.mapNotNull { row ->
            val syncId = row.syncId ?: return@mapNotNull skip("recurring", row.id)
            val categorySyncId = catMap[row.categoryId] ?: return@mapNotNull skipFk("recurring", row.id, "category", row.categoryId)
            val accountSyncId = accMap[row.accountId] ?: return@mapNotNull skipFk("recurring", row.id, "account", row.accountId)
            val paymentModeSyncId = row.paymentModeId?.let { pmId ->
                pmMap[pmId] ?: return@mapNotNull skipFk("recurring", row.id, "paymentMode", pmId)
            }
            SyncRecurring(
                syncId = syncId,
                type = row.type,
                amountMinor = row.amountMinor,
                currency = row.currency,
                note = row.note,
                categorySyncId = categorySyncId,
                accountSyncId = accountSyncId,
                paymentModeSyncId = paymentModeSyncId,
                startDate = row.startDate,
                freqUnit = row.freqUnit,
                freqInterval = row.freqInterval,
                dayOfWeek = row.dayOfWeek,
                dayOfMonth = row.dayOfMonth,
                useLastDay = row.useLastDay,
                endKind = row.endKind,
                endCount = row.endCount,
                endDate = row.endDate,
                lastMaterializedDate = row.lastMaterializedDate,
                deleted = row.deleted,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }

        val budgets = budgetRows.mapNotNull { row ->
            val syncId = row.syncId ?: return@mapNotNull skip("budget", row.id)
            val accountSyncId = accMap[row.accountId] ?: return@mapNotNull skipFk("budget", row.id, "account", row.accountId)
            val categorySyncId = row.categoryId?.let { catId ->
                catMap[catId] ?: return@mapNotNull skipFk("budget", row.id, "category", catId)
            }
            SyncBudget(
                syncId = syncId,
                name = row.name,
                amountMinor = row.amountMinor,
                currency = row.currency,
                categorySyncId = categorySyncId,
                accountSyncId = accountSyncId,
                periodType = row.periodType,
                startYearMonth = row.startYearMonth,
                recurringMonths = row.recurringMonths,
                deleted = row.deleted,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
            )
        }

        return SyncSnapshot(
            formatVersion = 1,
            generatedAtMs = nowMs(),
            originDeviceId = deviceIdentity.deviceId(),
            accounts = accounts,
            categories = categories,
            paymentModes = paymentModes,
            recurring = recurring,
            budgets = budgets,
            transactions = transactions,
        )
    }

    private inline fun <T> List<T>.idToSyncId(selector: (T) -> Pair<Long, String?>): Map<Long, String> =
        buildMap {
            this@idToSyncId.forEach { item ->
                val (id, syncId) = selector(item)
                if (syncId == null) {
                    logger.w { "Sync export: dropping row id=$id with null syncId during map build" }
                } else {
                    put(id, syncId)
                }
            }
        }

    private fun <T> skip(table: String, id: Long): T? {
        logger.w { "Sync export: skipping $table id=$id with null syncId" }
        return null
    }

    private fun <T> skipFk(table: String, id: Long, fkName: String, fkId: Long): T? {
        logger.w { "Sync export: skipping $table id=$id — unresolvable $fkName FK id=$fkId" }
        return null
    }

    private companion object {
        val logger = Logger.withTag("SyncExporter")
    }
}
