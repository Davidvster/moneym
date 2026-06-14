package com.dv.moneym.feature.settings.overview.export

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.data.backup.BackupExporter
import com.dv.moneym.data.backup.BackupImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val categories = FakeCategoryRepository()
    private val accounts = FakeAccountRepository()
    private val transactions = FakeTransactionRepository()
    private val budgets = FakeBudgetRepository()
    private val recurring = FakeRecurringTransactionRepository()
    private val settings = FakeAppSettings()

    private fun exporter() = BackupExporter(categories, accounts, transactions, budgets, recurring, settings)
    private fun importer() = BackupImporter(categories, accounts, transactions, recurring)

    private fun vm() = ExportViewModel(
        exporter(),
        importer(),
        TestDispatcherProvider(testDispatcher),
        SavedStateHandle(),
    )

    private fun seedDefaultAccount() {
        accounts.addAll(
            listOf(
                Account(
                    id = AccountId(1),
                    name = "Main",
                    type = AccountType.CASH,
                    currency = CurrencyCode("EUR"),
                    isDefault = true,
                    archived = false,
                    createdAt = Instant.DISTANT_PAST,
                    updatedAt = Instant.DISTANT_PAST,
                )
            )
        )
    }

    @Test
    fun exportJsonTogglesExportingAndEmitsReadyEffect() = runTest(testDispatcher) {
        seedDefaultAccount()
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.effects.test {
            vm.onIntent(ExportIntent.SetExportFormat(csv = false))
            vm.onIntent(ExportIntent.ExportRequested)
            val effect = awaitItem()
            assertIs<ExportEffect.ExportReady>(effect)
            assertEquals("moneym_backup.json", effect.fileName)
            assertEquals("application/json", effect.mimeType)
            assertTrue(effect.content.contains("EUR"))
            assertFalse(vm.state.value.isExporting)
        }
    }

    @Test
    fun exportCsvEmitsReadyEffectWithCsvMime() = runTest(testDispatcher) {
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.effects.test {
            vm.onIntent(ExportIntent.SetExportFormat(csv = true))
            vm.onIntent(ExportIntent.ExportRequested)
            val effect = awaitItem()
            assertIs<ExportEffect.ExportReady>(effect)
            assertEquals("moneym_export.csv", effect.fileName)
            assertEquals("text/csv", effect.mimeType)
            assertTrue(effect.content.startsWith("date,type,amount"))
            assertFalse(vm.state.value.isExporting)
        }
    }

    @Test
    fun importRequestedEmitsImportEffect() = runTest(testDispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onIntent(ExportIntent.ImportRequested)
            assertEquals(ExportEffect.ImportRequested, awaitItem())
        }
    }

    @Test
    fun importJsonChangedResetsPreviewAndError() = runTest(testDispatcher) {
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(ExportIntent.ImportJsonChanged("{}"))
        runCurrent()
        assertEquals("{}", vm.state.value.importJson)
        assertNull(vm.state.value.importPreview)
        assertNull(vm.state.value.importError)
    }

    @Test
    fun previewBlankJsonIsIgnored() = runTest(testDispatcher) {
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(ExportIntent.PreviewImportRequested)
        advanceUntilIdle()
        assertNull(vm.state.value.importPreview)
        assertNull(vm.state.value.importError)
    }

    @Test
    fun previewInvalidJsonSetsError() = runTest(testDispatcher) {
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(ExportIntent.ImportJsonChanged("not json"))
        vm.onIntent(ExportIntent.PreviewImportRequested)
        advanceUntilIdle()
        assertNotNull(vm.state.value.importError)
        assertNull(vm.state.value.importPreview)
    }

    @Test
    fun previewValidJsonSetsPreview() = runTest(testDispatcher) {
        seedDefaultAccount()
        val json = exporter().exportToJson()
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(ExportIntent.ImportJsonChanged(json))
        vm.onIntent(ExportIntent.PreviewImportRequested)
        advanceUntilIdle()
        assertNotNull(vm.state.value.importPreview)
        assertTrue(vm.state.value.importPreview!!.isValid)
        assertNull(vm.state.value.importError)
    }

    @Test
    fun applyImportClearsStateAndShowsSuccess() = runTest(testDispatcher) {
        seedDefaultAccount()
        val json = exporter().exportToJson()
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(ExportIntent.ImportJsonChanged(json))
        vm.onIntent(ExportIntent.ApplyImportRequested)
        advanceUntilIdle()
        val s = vm.state.value
        assertFalse(s.isImporting)
        assertTrue(s.showImportSuccess)
        assertEquals("", s.importJson)
        assertNull(s.importPreview)
    }

    @Test
    fun clearImportResetsImportFields() = runTest(testDispatcher) {
        val vm = vm()
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(ExportIntent.ImportJsonChanged("{}"))
        vm.onIntent(ExportIntent.ClearImport)
        runCurrent()
        val s = vm.state.value
        assertEquals("", s.importJson)
        assertNull(s.importPreview)
        assertNull(s.importError)
        assertFalse(s.showImportSuccess)
    }

    @Test
    fun clearExportIsNoOp() = runTest(testDispatcher) {
        val vm = vm()
        val before = vm.state.value
        vm.onIntent(ExportIntent.ClearExport)
        assertEquals(before, vm.state.value)
    }
}
