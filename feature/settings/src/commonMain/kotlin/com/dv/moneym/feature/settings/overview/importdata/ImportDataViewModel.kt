package com.dv.moneym.feature.settings.overview.importdata

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.AppLogger
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.settings.overview.importdata.usecase.PrepareImportPreviewUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_import_failed
import org.jetbrains.compose.resources.getString

class ImportDataViewModel(
    private val holder: CsvImportHolder,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val prepareImportPreview: PrepareImportPreviewUseCase,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val logger = AppLogger.tag("ImportData")

    private val _state by savedStateHandle.saved { MutableStateFlow(ImportDataUiState()) }

    val state: StateFlow<ImportDataUiState> = _state
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<ImportDataEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadAndParse()
    }

    private fun loadAndParse() {
        viewModelScope.launch {
            val content = holder.content
            val format = holder.format

            val accountsDeferred = async(dispatchers.io) { accountRepository.observeAll().first() }
            val categoriesDeferred = async(dispatchers.io) { categoryRepository.observeAll().first() }

            val accounts = accountsDeferred.await()
            val existingCategories = categoriesDeferred.await()

            val preview = withContext(dispatchers.io) {
                prepareImportPreview(content, format, accounts, existingCategories)
            }

            if (preview.parseError != null) {
                _state.update { it.copy(isParsing = false, parseError = preview.parseError) }
                return@launch
            }

            _state.update {
                it.copy(
                    isParsing = false,
                    availableAccounts = accounts,
                    availableCategories = existingCategories,
                    selectedAccountId = preview.selectedAccountId,
                    categoryMappings = preview.categoryMappings,
                    transactions = preview.transactions,
                )
            }
        }
    }

    fun onIntent(intent: ImportDataIntent) {
        when (intent) {
            is ImportDataIntent.AccountSelected -> {
                val account = _state.value.availableAccounts.firstOrNull { it.id == intent.id }
                    ?: return
                _state.update { state ->
                    state.copy(
                        selectedAccountId = intent.id,
                        transactions = state.transactions.map { tx ->
                            if (tx.currencyCode.isEmpty() || holder.format == CsvSourceFormat.EASY_HOME_FINANCE)
                                tx.copy(currencyCode = account.currency.value)
                            else tx
                        },
                    )
                }
            }

            is ImportDataIntent.CategoryMappingChanged -> {
                val category = if (intent.newCategoryId != null) {
                    _state.value.availableCategories.firstOrNull { it.id == intent.newCategoryId }
                } else null
                _state.update { state ->
                    state.copy(
                        categoryMappings = state.categoryMappings.map { m ->
                            if (m.csvName == intent.csvName) {
                                m.copy(
                                    mappedToCategoryId = intent.newCategoryId,
                                    mappedToCategoryName = category?.name,
                                )
                            } else m
                        },
                    )
                }
            }

            is ImportDataIntent.TransactionToggled ->
                _state.update { state ->
                    state.copy(
                        transactions = state.transactions.map { tx ->
                            if (tx.id == intent.id) tx.copy(isSelected = !tx.isSelected) else tx
                        },
                    )
                }

            ImportDataIntent.SelectAllToggled -> {
                val allSelected = _state.value.transactions.all { it.isSelected }
                _state.update { state ->
                    state.copy(transactions = state.transactions.map { it.copy(isSelected = !allSelected) })
                }
            }

            ImportDataIntent.ImportConfirmed -> executeImport()
        }
    }

    private fun executeImport() {
        val s = _state.value
        val accountId = s.selectedAccountId ?: return
        val account = s.availableAccounts.firstOrNull { it.id == accountId } ?: return
        val selectedTxns = s.transactions.filter { it.isSelected }
        if (selectedTxns.isEmpty()) return

        _state.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    val catIdMap = mutableMapOf<String, CategoryId>()
                    val now = clock.now()

                    for (mapping in s.categoryMappings) {
                        val catId = if (mapping.mappedToCategoryId != null) {
                            mapping.mappedToCategoryId
                        } else {
                            val categoryType = selectedTxns
                                .firstOrNull { it.categoryName == mapping.csvName }
                                ?.type ?: TransactionType.EXPENSE
                            categoryRepository.insert(
                                Category(
                                    id = CategoryId(0),
                                    name = mapping.csvName,
                                    type = categoryType,
                                    iconKey = Icon.Tag.key,
                                    colorHex = "#8A8A8A",
                                    isUserCreated = true,
                                    archived = false,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                        }
                        catIdMap[mapping.csvName] = catId
                    }

                    for (txn in selectedTxns) {
                        val catId = catIdMap[txn.categoryName] ?: continue
                        val currency = txn.currencyCode.ifEmpty { account.currency.value }
                        transactionRepository.upsert(
                            Transaction(
                                id = UNSAVED_TRANSACTION_ID,
                                type = txn.type,
                                amount = Money(txn.amountMinorUnits, CurrencyCode(currency)),
                                occurredOn = txn.date,
                                note = txn.note,
                                categoryId = catId,
                                accountId = accountId,
                                createdAt = now,
                                updatedAt = now,
                                paymentModeId = null,
                            )
                        )
                    }
                }
                _state.update { it.copy(isImporting = false) }
                _effects.send(ImportDataEffect.ImportDone)
            } catch (e: Exception) {
                logger.e(e) { "CSV import failed" }
                _state.update { it.copy(isImporting = false) }
                _effects.send(ImportDataEffect.ShowError(e.message ?: getString(Res.string.settings_import_failed)))
            }
        }
    }

}
