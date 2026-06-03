package com.dv.moneym.feature.settings.overview.importdata

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.feature.settings.overview.importdata.usecase.PrepareImportPreviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ImportDataViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val clock = FixedClock(Instant.fromEpochMilliseconds(1_000_000L))

    private val moneymCsv = """
        date,type,amount,currency,category,account,note
        2026-01-15,EXPENSE,12.50,EUR,Groceries,Main,milk
        2026-01-16,INCOME,100.00,EUR,Salary,Main,
    """.trimIndent()

    private fun account(id: Long, currency: String = "EUR", isDefault: Boolean = true) = Account(
        id = AccountId(id),
        name = "Acc$id",
        type = AccountType.CASH,
        currency = CurrencyCode(currency),
        isDefault = isDefault,
        archived = false,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
    )

    private fun category(id: Long, name: String) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = "tag",
        colorHex = "#FFFFFF",
        isUserCreated = true,
        archived = false,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
    )

    private fun vm(
        holder: CsvImportHolder,
        categoryRepo: FakeCategoryRepository = FakeCategoryRepository(),
        accountRepo: FakeAccountRepository = FakeAccountRepository(),
        transactionRepo: FakeTransactionRepository = FakeTransactionRepository(),
    ) = ImportDataViewModel(
        holder,
        categoryRepo,
        accountRepo,
        transactionRepo,
        PrepareImportPreviewUseCase(),
        TestDispatcherProvider(testDispatcher),
        clock,
        SavedStateHandle(),
    )

    private fun holder(content: String, format: CsvSourceFormat = CsvSourceFormat.MONEYM) =
        CsvImportHolder().apply { this.content = content; this.format = format }

    @Test
    fun parsesContentAndPopulatesPreview() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val vm = vm(holder(moneymCsv), accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            assertNull(s.parseError)
            assertEquals(2, s.transactions.size)
            assertEquals(AccountId(1), s.selectedAccountId)
            assertEquals(2, s.categoryMappings.size)
        }
    }

    @Test
    fun parseErrorIsSurfaced() = runTest(testDispatcher) {
        val vm = vm(holder("garbage,header\n1,2"))
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            assertNotNull(s.parseError)
            assertTrue(s.transactions.isEmpty())
        }
    }

    @Test
    fun categoryMappingMatchesExistingCategoryByName() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val cats = FakeCategoryRepository().apply { addAll(listOf(category(7, "Groceries"))) }
        val vm = vm(holder(moneymCsv), categoryRepo = cats, accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            val mapping = s.categoryMappings.first { it.csvName == "Groceries" }
            assertEquals(CategoryId(7), mapping.mappedToCategoryId)
        }
    }

    @Test
    fun accountSelectedUpdatesEmptyCurrencyTransactions() = runTest(testDispatcher) {
        val ehfCsv = "id;date;wallet;category;x;note;amount\n1;2026-01-10;W;Food;y;n;-5.00"
        val accounts = FakeAccountRepository().apply {
            addAll(listOf(account(1, "EUR"), account(2, "USD", isDefault = false)))
        }
        val vm = vm(holder(ehfCsv, CsvSourceFormat.EASY_HOME_FINANCE), accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            vm.onIntent(ImportDataIntent.AccountSelected(AccountId(2)))
            var after = awaitItem()
            while (after.selectedAccountId != AccountId(2)) after = awaitItem()
            assertEquals("USD", after.transactions.first().currencyCode)
        }
    }

    @Test
    fun categoryMappingChangedUpdatesMapping() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val cats = FakeCategoryRepository().apply { addAll(listOf(category(9, "Other"))) }
        val vm = vm(holder(moneymCsv), categoryRepo = cats, accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            vm.onIntent(ImportDataIntent.CategoryMappingChanged("Groceries", CategoryId(9)))
            val after = awaitItem()
            val mapping = after.categoryMappings.first { it.csvName == "Groceries" }
            assertEquals(CategoryId(9), mapping.mappedToCategoryId)
            assertEquals("Other", mapping.mappedToCategoryName)
        }
    }

    @Test
    fun transactionToggledFlipsSelection() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val vm = vm(holder(moneymCsv), accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            assertTrue(s.transactions.first().isSelected)
            vm.onIntent(ImportDataIntent.TransactionToggled(s.transactions.first().id))
            val after = awaitItem()
            assertFalse(after.transactions.first().isSelected)
        }
    }

    @Test
    fun selectAllToggledDeselectsWhenAllSelected() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val vm = vm(holder(moneymCsv), accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            assertTrue(s.transactions.all { it.isSelected })
            vm.onIntent(ImportDataIntent.SelectAllToggled)
            val after = awaitItem()
            assertTrue(after.transactions.none { it.isSelected })
        }
    }

    @Test
    fun importConfirmedInsertsTransactionsAndEmitsDone() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val txns = FakeTransactionRepository()
        val vm = vm(holder(moneymCsv), accountRepo = accounts, transactionRepo = txns)

        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        vm.effects.test {
            vm.onIntent(ImportDataIntent.ImportConfirmed)
            assertEquals(ImportDataEffect.ImportDone, awaitItem())
        }
        assertFalse(vm.state.value.isImporting)
    }

    @Test
    fun importConfirmedWithNoSelectionIsIgnored() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(1))) }
        val vm = vm(holder(moneymCsv), accountRepo = accounts)
        vm.state.test {
            skipItems(1)
            var s = awaitItem()
            while (s.isParsing) s = awaitItem()
            vm.onIntent(ImportDataIntent.SelectAllToggled)
            awaitItem()
            vm.onIntent(ImportDataIntent.ImportConfirmed)
            expectNoEvents()
            assertFalse(vm.state.value.isImporting)
        }
    }
}
