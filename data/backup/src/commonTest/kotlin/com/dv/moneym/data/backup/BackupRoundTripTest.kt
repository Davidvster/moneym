package com.dv.moneym.data.backup

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.ImportMode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeOverviewRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewLayoutBlock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupRoundTripTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val eur = CurrencyCode("EUR")

    private fun makeCategory(id: Long) = Category(
        id = CategoryId(id), name = "Cat$id", iconKey = Icon.Dots.key, colorHex = "#8A8A8A",
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
    private fun makeWidget() = OverviewAiWidget(
        id = 0,
        title = "Cash flow",
        prompt = "Summarize cash flow",
        a2uiJson = """{"type":"metric"}""",
        enabled = true,
        sortOrder = 10,
        createdAt = epoch,
        updatedAt = epoch,
        lastGeneratedAt = epoch,
        lastGenerationEngineId = "local",
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

        val budgetRepo = FakeBudgetRepository()
        val recurringRepo = FakeRecurringTransactionRepository()
        val overviewRepo = FakeOverviewRepository()
        overviewRepo.replaceLayout(
            listOf(
                OverviewLayoutBlock(OverviewBlockId("totals"), sortOrder = 0, visible = true),
                OverviewLayoutBlock(OverviewBlockId("monthly_spend"), sortOrder = 1, visible = false),
            )
        )
        overviewRepo.upsertAiWidget(makeWidget())

        val exporter = BackupExporter(catRepo, accRepo, txnRepo, budgetRepo, recurringRepo, overviewRepo, settings)
        val json = exporter.exportToJson()
        assertTrue(json.contains("\"version\":1"))
        assertTrue(json.contains("Cat1"))
        assertTrue(json.contains("Cash flow"))

        // Fresh repos for import
        val catRepo2 = FakeCategoryRepository()
        val accRepo2 = FakeAccountRepository()
        val txnRepo2 = FakeTransactionRepository()
        val recurringRepo2 = FakeRecurringTransactionRepository()
        val overviewRepo2 = FakeOverviewRepository()
        val importer = BackupImporter(catRepo2, accRepo2, txnRepo2, recurringRepo2, overviewRepo2)

        val preview = importer.previewFromJson(json)
        assertTrue(preview.isValid)
        assertEquals(2, preview.categories.new)
        assertEquals(1, preview.accounts.new)
        assertEquals(2, preview.transactions.new)

        importer.applyFromJson(json, ImportMode.MERGE)
        assertEquals(2, catRepo2.categories.size)
        assertEquals(1, accRepo2.accounts.size)
        assertEquals(2, txnRepo2.transactions.size)
        assertEquals(listOf("totals", "monthly_spend"), overviewRepo2.layout.blocks.map { it.blockId.value })
        assertEquals(1, overviewRepo2.widgets.size)
        assertEquals("Cash flow", overviewRepo2.widgets.first().title)
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

        val budgetRepo = FakeBudgetRepository()
        val recurringRepo = FakeRecurringTransactionRepository()
        val overviewRepo = FakeOverviewRepository()
        val exporter = BackupExporter(catRepo, accRepo, txnRepo, budgetRepo, recurringRepo, overviewRepo, settings)
        val json = exporter.exportToJson()

        // Import into repos that already have the same data
        val importer = BackupImporter(catRepo, accRepo, txnRepo, recurringRepo, overviewRepo)
        val preview = importer.previewFromJson(json)

        assertEquals(0, preview.categories.new)
        assertEquals(1, preview.categories.duplicate)
        assertEquals(0, preview.transactions.new)
    }
}
