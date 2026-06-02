package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.data.transactions.PaymentModeSyncRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Proves the Phase-8 deterministic-seed-syncId fix: two devices that each seed defaults
 * independently (different localized names, different clocks) produce identical seed syncIds,
 * so reconcile finds nothing new to apply — no doubling of categories / accounts / payment modes.
 */
class SeedMergeTest {

    private val clock = FixedClock(Instant.fromEpochMilliseconds(1_000L))

    private val englishCategoryNames = listOf(
        "Groceries", "Eating out", "Rent", "Transport", "Utilities",
        "Health", "Entertainment", "Shopping", "Other",
        "Salary", "Payment", "Gift", "Other",
    )
    private val germanCategoryNames = englishCategoryNames.map { "$it-de" }

    private val paymentModeSeeds = listOf(
        "seed-paymentmode-cash" to "Cash",
        "seed-paymentmode-card" to "Card",
        "seed-paymentmode-transfer" to "Transfer",
    )

    private class Device {
        val accounts = FakeAccountRepository()
        val categories = FakeCategoryRepository()
        val paymentModes = FakePaymentModeRepository()
        val transactions = FakeTransactionRepository()
        val recurring = FakeRecurringTransactionRepository()
        val budgets = FakeBudgetRepository()
    }

    private suspend fun seedPaymentModes(device: Device, now: Long, namePrefix: String) {
        paymentModeSeeds.forEach { (syncId, name) ->
            device.paymentModes.upsertFromSync(
                PaymentModeSyncRow(
                    id = 0,
                    syncId = syncId,
                    name = "$namePrefix$name",
                    deleted = false,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    private suspend fun seedDevice(device: Device, categoryNames: List<String>, namePrefix: String, accountName: String, now: Long) {
        SeedCategoriesUseCase(device.categories, { categoryNames }, nowMs = { now })()
        SeedAccountsUseCase(device.accounts, InMemoryAppSettings(), clock, { accountName })()
        seedPaymentModes(device, now, namePrefix)
    }

    private fun exporter(device: Device, deviceId: String): SyncExporter {
        val settings = InMemoryAppSettings()
        settings.putString(PrefKeys.DEVICE_ID, deviceId)
        return SyncExporter(
            accountRepository = device.accounts,
            categoryRepository = device.categories,
            paymentModeRepository = device.paymentModes,
            transactionRepository = device.transactions,
            recurringTransactionRepository = device.recurring,
            budgetRepository = device.budgets,
            deviceIdentity = DeviceIdentity(settings),
            nowMs = { 999L },
        )
    }

    @Test
    fun twoIndependentlySeededDevicesReconcileToNoNewSeedAdds() = runTestWithDispatchers {
        val deviceA = Device()
        val deviceB = Device()
        seedDevice(deviceA, englishCategoryNames, namePrefix = "", accountName = "Main", now = 1_000L)
        seedDevice(deviceB, germanCategoryNames, namePrefix = "de-", accountName = "Hauptkonto", now = 2_000L)

        val local = exporter(deviceA, "device-A").export()
        val remote = exporter(deviceB, "device-B").export()

        val result = SyncReconciler().reconcile(local = local, remote = remote)

        // Doubling = a remote seed row whose syncId is absent locally → a NEW add.
        // Because both devices use the same deterministic seed syncIds, every reconciled
        // row (if any) is an LWW edit of an existing row, never a new duplicate.
        val localAccountIds = local.accounts.map { it.syncId }.toSet()
        val localCategoryIds = local.categories.map { it.syncId }.toSet()
        val localPaymentModeIds = local.paymentModes.map { it.syncId }.toSet()

        assertEquals(emptyList(), result.toApply.accounts.map { it.syncId }.filterNot { it in localAccountIds }, "no new account adds")
        assertEquals(emptyList(), result.toApply.categories.map { it.syncId }.filterNot { it in localCategoryIds }, "no new category adds")
        assertEquals(emptyList(), result.toApply.paymentModes.map { it.syncId }.filterNot { it in localPaymentModeIds }, "no new payment-mode adds")
        assertTrue(result.pendingDeletions.isEmpty(), "no pending deletions")
    }

    @Test
    fun identicalSeedsReconcileToCompletelyEmptyApply() = runTestWithDispatchers {
        // Same names, same clock → byte-identical seed snapshots → reconcile is a total no-op.
        val deviceA = Device()
        val deviceB = Device()
        seedDevice(deviceA, englishCategoryNames, namePrefix = "", accountName = "Main", now = 1_000L)
        seedDevice(deviceB, englishCategoryNames, namePrefix = "", accountName = "Main", now = 1_000L)

        val result = SyncReconciler().reconcile(
            local = exporter(deviceA, "device-A").export(),
            remote = exporter(deviceB, "device-B").export(),
        )

        assertTrue(result.toApply.accounts.isEmpty())
        assertTrue(result.toApply.categories.isEmpty())
        assertTrue(result.toApply.paymentModes.isEmpty())
        assertTrue(result.pendingDeletions.isEmpty())
    }

    @Test
    fun seedSyncIdsAreIdenticalAcrossLanguages() = runTestWithDispatchers {
        val deviceA = Device()
        val deviceB = Device()
        seedDevice(deviceA, englishCategoryNames, namePrefix = "", accountName = "Main", now = 1_000L)
        seedDevice(deviceB, germanCategoryNames, namePrefix = "de-", accountName = "Hauptkonto", now = 2_000L)

        assertEquals(
            deviceA.categories.exportForSync().map { it.syncId },
            deviceB.categories.exportForSync().map { it.syncId },
        )
        assertEquals(
            deviceA.accounts.exportForSync().map { it.syncId },
            deviceB.accounts.exportForSync().map { it.syncId },
        )
        assertEquals(
            deviceA.paymentModes.exportForSync().map { it.syncId },
            deviceB.paymentModes.exportForSync().map { it.syncId },
        )
    }
}
