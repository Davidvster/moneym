package com.dv.moneym.data.backup

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupRoundTripTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val eur = CurrencyCode("EUR")

    private fun makeCategory(id: Long) = Category(
        id = CategoryId(id), name = "Cat$id", iconKey = "dots", colorHex = "#8A8A8A",
        isUserCreated = true, archived = false, createdAt = epoch, updatedAt = epoch,
    )
    private fun makeAccount(id: Long) = Account(
        id = AccountId(id), name = "Acc$id", type = AccountType.CASH, currency = eur,
        isDefault = id == 1L, archived = false, createdAt = epoch, updatedAt = epoch,
    )
    private fun makeTxn(catId: Long, accId: Long, amount: Long) = Transaction(
        id = UNSAVED_TRANSACTION_ID, type = TransactionType.EXPENSE,
        amount = Money(amount, eur), occurredOn = LocalDate(2026, 5, 1),
        note = "test", categoryId = CategoryId(catId), accountId = AccountId(accId),
        createdAt = epoch, updatedAt = epoch,
    )

    @Test
    fun jsonRoundTripPreservesAllData() = runTestWithDispatchers {
        val catRepo = FakeCategoryRepository()
        val accRepo = FakeAccountRepository()
        val txnRepo = FakeTransactionRepository()
        val settings = FakeAppSettings()

        catRepo.addAll(listOf(makeCategory(1), makeCategory(2)))
        accRepo.addAll(listOf(makeAccount(1)))
        txnRepo.upsert(makeTxn(catId = 1, accId = 1, amount = 1000))
        txnRepo.upsert(makeTxn(catId = 2, accId = 1, amount = 2500))

        val exporter = BackupExporter(catRepo, accRepo, txnRepo, settings)
        val json = exporter.exportToJson()
        assertTrue(json.contains("\"version\":1"))
        assertTrue(json.contains("Cat1"))

        // Fresh repos for import
        val catRepo2 = FakeCategoryRepository()
        val accRepo2 = FakeAccountRepository()
        val txnRepo2 = FakeTransactionRepository()
        val importer = BackupImporter(catRepo2, accRepo2, txnRepo2)

        val preview = importer.previewFromJson(json)
        assertTrue(preview.isValid)
        assertEquals(2, preview.categories.new)
        assertEquals(1, preview.accounts.new)
        assertEquals(2, preview.transactions.new)

        importer.applyFromJson(json, ImportMode.MERGE)
        assertEquals(2, catRepo2.categories.size)
        assertEquals(1, accRepo2.accounts.size)
        assertEquals(2, txnRepo2.transactions.size)
    }

    @Test
    fun mergeModeSkipsDuplicates() = runTestWithDispatchers {
        val catRepo = FakeCategoryRepository()
        val accRepo = FakeAccountRepository()
        val txnRepo = FakeTransactionRepository()
        val settings = FakeAppSettings()

        catRepo.addAll(listOf(makeCategory(1)))
        accRepo.addAll(listOf(makeAccount(1)))
        txnRepo.upsert(makeTxn(catId = 1, accId = 1, amount = 1000))

        val exporter = BackupExporter(catRepo, accRepo, txnRepo, settings)
        val json = exporter.exportToJson()

        // Import into repos that already have the same data
        val importer = BackupImporter(catRepo, accRepo, txnRepo)
        val preview = importer.previewFromJson(json)

        assertEquals(0, preview.categories.new)
        assertEquals(1, preview.categories.duplicate)
        assertEquals(0, preview.transactions.new)
    }
}
