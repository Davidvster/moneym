package com.dv.moneym.feature.transactions.list.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DateStyle
import com.dv.moneym.core.common.formatDate
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
import com.dv.moneym.core.model.matches
import com.dv.moneym.core.model.selectedCategoryIds
import com.dv.moneym.core.model.selectedType
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.recurrence.RecurrenceMath
import com.dv.moneym.feature.transactions.list.DayGroup
import com.dv.moneym.feature.transactions.list.TransactionListEphemeralState
import com.dv.moneym.feature.transactions.list.TransactionUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.number

class TransactionPageViewModel(
    private val yearMonth: YearMonth,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val clock: AppClock,
    private val ephemeralState: TransactionListEphemeralState,
) : ViewModel() {

    private data class FilterBase(
        val filter: TransactionFilter,
        val prefs: TxDisplayPrefs,
        val pmEnabled: Boolean,
        val categories: List<Category>,
        val showPending: Boolean,
    ) {
        val catMap: Map<CategoryId, Category> = categories.associateBy { it.id }
    }

    private val selectedIds = MutableStateFlow<Set<TransactionId>>(emptySet())
    private val bulkSheet = MutableStateFlow<BulkSheetState>(BulkSheetState.None)
    private val bulkRateText = MutableStateFlow("")
    private val bulkRateError = MutableStateFlow(false)
    private var consumeNextPickerDismiss = false

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = combine(
        appSettingsRepository.observeLastTransactionFilter(),
        appSettingsRepository.observeTxDisplayPrefs(),
        appSettingsRepository.observePaymentModeEnabled(),
        categoryRepository.observeAll(),
        appSettingsRepository.observeShowPendingRecurring(),
    ) { filter, prefs, pmEnabled, cats, showPending ->
        FilterBase(filter, prefs, pmEnabled, cats, showPending)
    }.flatMapLatest { base ->
        combine(
            transactionRepository.observeByMonth(yearMonth.year, yearMonth.monthNumber),
            appSettingsRepository.observeSelectedAccountId(),
            accountRepository.observeAll(),
            paymentModeRepository.observeAll(),
            ephemeralState.searchQuery,
            recurringTransactionRepository.observeAll(),
            selectedIds,
            bulkSheet,
            bulkRateText,
            bulkRateError,
        ) { args ->
            @Suppress("UNCHECKED_CAST")
            val transactions = args[0] as List<Transaction>
            val selectedAccId = args[1] as Long
            @Suppress("UNCHECKED_CAST")
            val accounts = args[2] as List<Account>
            @Suppress("UNCHECKED_CAST")
            val paymentModes = args[3] as List<PaymentMode>
            val searchQuery = args[4] as String
            @Suppress("UNCHECKED_CAST")
            val rules = args[5] as List<RecurringTransaction>
            @Suppress("UNCHECKED_CAST")
            val currentSelectedIds = args[6] as Set<TransactionId>
            val currentBulkSheet = args[7] as BulkSheetState
            val currentBulkRateText = args[8] as String
            val currentBulkRateError = args[9] as Boolean

            val accountFilteredTxns = if (selectedAccId > 0L) {
                transactions.filter { it.accountId.value == selectedAccId }
            } else transactions

            val filtered = accountFilteredTxns
                .filter { tx ->
                    base.filter.matches(tx)
                }
                .filter { tx ->
                    if (searchQuery.isBlank()) true
                    else {
                        val q = searchQuery.trim().lowercase()
                        val catName = base.catMap[tx.categoryId]?.name?.lowercase() ?: ""
                        catName.contains(q) || (tx.note?.lowercase()?.contains(q) == true)
                    }
                }

            val pending = if (!base.showPending) emptyList()
            else buildPendingForMonth(
                rules = rules,
                ym = yearMonth,
                today = clock.today(),
                selectedAccId = selectedAccId,
                filter = base.filter,
                searchQuery = searchQuery,
                catMap = base.catMap,
            )

            val dayGroups = (filtered.map { tx ->
                tx.occurredOn to tx.toUiModel(base.catMap[tx.categoryId], paymentModes, base.pmEnabled)
            } + pending)
                .groupBy { it.first }
                .map { (date, pairs) ->
                    DayGroup(
                        date = date,
                        label = formatDate(date, DateStyle.Full),
                        transactions = pairs.map { it.second },
                    )
                }
                .sortedByDescending { it.date }

            val selectableIds = filtered.mapTo(mutableSetOf()) { it.id }
            val effectiveSelectedIds = currentSelectedIds.intersect(selectableIds)
            if (effectiveSelectedIds.size != currentSelectedIds.size) {
                selectedIds.value = effectiveSelectedIds
            }
            val selectedTransactions = filtered.filter { it.id in effectiveSelectedIds }
            val currencyTotals = selectedTransactions
                .groupBy { it.amount.currency.value }
                .map { (currency, rows) ->
                    SelectionCurrencyTotal(
                        currency = currency,
                        minorUnits = rows.sumOf { tx ->
                            if (tx.type == TransactionType.INCOME) tx.amount.minorUnits else -tx.amount.minorUnits
                        },
                    )
                }
                .sortedBy { it.currency }

            TransactionPageUiState(
                isLoading = false,
                dayGroups = dayGroups,
                isEmpty = dayGroups.isEmpty(),
                txDisplayPrefs = base.prefs,
                selection = TransactionSelectionUiState(
                    selectedIds = effectiveSelectedIds,
                    selectedCount = selectedTransactions.size,
                    currencyTotals = currencyTotals,
                    canMoveWallet = accounts.count { !it.archived } > 1,
                    canMovePaymentMode = base.pmEnabled && paymentModes.size > 1,
                ),
                availableCategories = base.categories.filter { !it.archived },
                availableAccounts = accounts.filter { !it.archived },
                paymentModes = paymentModes,
                paymentModeEnabled = base.pmEnabled,
                bulkSheet = currentBulkSheet,
                bulkRateText = currentBulkRateText,
                bulkRateError = currentBulkRateError,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TransactionPageUiState(),
    )

    internal fun onIntent(intent: TransactionPageIntent) {
        when (intent) {
            is TransactionPageIntent.TransactionPressed -> toggleSelectionIfActive(intent.id)
            is TransactionPageIntent.TransactionLongPressed -> startSelection(intent.id)
            TransactionPageIntent.ClearSelection -> clearSelection()
            TransactionPageIntent.DeleteRequested -> if (selectedIds.value.isNotEmpty()) {
                bulkSheet.value = BulkSheetState.DeleteConfirm
            }
            TransactionPageIntent.EditRequested -> if (selectedIds.value.isNotEmpty()) {
                bulkSheet.value = BulkSheetState.Actions
            }
            TransactionPageIntent.DismissBulkSheet -> dismissBulkSheet()
            TransactionPageIntent.ConfirmDelete -> applyDelete()
            TransactionPageIntent.PickCategoryRequested -> bulkSheet.value = BulkSheetState.CategoryPicker
            is TransactionPageIntent.CategoryPicked -> pickCategory(intent.id)
            TransactionPageIntent.ConfirmCategory -> applyCategory()
            TransactionPageIntent.PickWalletRequested -> bulkSheet.value = BulkSheetState.WalletPicker
            is TransactionPageIntent.WalletPicked -> pickWallet(intent.account)
            is TransactionPageIntent.WalletRateChanged -> {
                bulkRateText.value = intent.text.filter { it.isDigit() || it == '.' || it == ',' }
                bulkRateError.value = false
            }
            TransactionPageIntent.ConfirmWallet -> applyWallet()
            TransactionPageIntent.PickPaymentModeRequested -> bulkSheet.value = BulkSheetState.PaymentModePicker
            is TransactionPageIntent.PaymentModePicked -> pickPaymentMode(intent.id)
            TransactionPageIntent.ConfirmPaymentMode -> applyPaymentMode()
        }
    }

    private fun startSelection(id: TransactionId) {
        if (id.value <= 0L) return
        selectedIds.update { it + id }
    }

    private fun toggleSelectionIfActive(id: TransactionId) {
        if (selectedIds.value.isEmpty() || id.value <= 0L) return
        selectedIds.update { ids -> if (id in ids) ids - id else ids + id }
    }

    private fun clearSelection() {
        selectedIds.value = emptySet()
        dismissBulkSheet()
    }

    private fun dismissBulkSheet() {
        if (consumeNextPickerDismiss) {
            consumeNextPickerDismiss = false
            return
        }
        bulkSheet.value = BulkSheetState.None
        bulkRateText.value = ""
        bulkRateError.value = false
    }

    private fun selectedOrDismiss(): Set<TransactionId> {
        val ids = selectedIds.value
        if (ids.isEmpty()) dismissBulkSheet()
        return ids
    }

    private fun applyDelete() {
        val ids = selectedOrDismiss()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            transactionRepository.delete(ids)
            clearSelection()
        }
    }

    private fun pickCategory(id: CategoryId) {
        val category = state.value.availableCategories.firstOrNull { it.id == id } ?: return
        consumeNextPickerDismiss = true
        bulkSheet.value = BulkSheetState.CategoryConfirm(category)
    }

    private fun applyCategory() {
        val category = (bulkSheet.value as? BulkSheetState.CategoryConfirm)?.category ?: return
        val ids = selectedOrDismiss()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            transactionRepository.updateCategory(ids, category.id, category.type)
            clearSelection()
        }
    }

    private fun pickWallet(account: Account) {
        val ids = selectedOrDismiss()
        if (ids.isEmpty()) return
        val selectedCurrencies = currentSelectedCurrencies(ids)
        val requiresRate = selectedCurrencies.any { it != account.currency.value }
        bulkRateText.value = if (requiresRate) "1" else ""
        bulkRateError.value = false
        consumeNextPickerDismiss = true
        bulkSheet.value = BulkSheetState.WalletConfirm(account, requiresRate)
    }

    private fun applyWallet() {
        val sheet = bulkSheet.value as? BulkSheetState.WalletConfirm ?: return
        val ids = selectedOrDismiss()
        if (ids.isEmpty()) return
        val rate = if (sheet.requiresRate) {
            bulkRateText.value.replace(',', '.').toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?: run {
                    bulkRateError.value = true
                    return
                }
        } else null
        viewModelScope.launch {
            transactionRepository.updateAccount(
                ids = ids,
                accountId = sheet.account.id,
                currency = sheet.account.currency,
                rate = rate,
            )
            clearSelection()
        }
    }

    private fun pickPaymentMode(id: PaymentModeId) {
        val paymentMode = state.value.paymentModes.firstOrNull { it.id == id } ?: return
        bulkSheet.value = BulkSheetState.PaymentModeConfirm(paymentMode)
    }

    private fun applyPaymentMode() {
        val sheet = bulkSheet.value as? BulkSheetState.PaymentModeConfirm ?: return
        val ids = selectedOrDismiss()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            transactionRepository.updatePaymentMode(ids, sheet.paymentMode.id)
            clearSelection()
        }
    }

    private fun currentSelectedCurrencies(ids: Set<TransactionId>): Set<String> =
        state.value.dayGroups
            .flatMap { it.transactions }
            .filter { !it.isPending && it.id in ids }
            .mapTo(mutableSetOf()) { it.currency }

    private fun buildPendingForMonth(
        rules: List<RecurringTransaction>,
        ym: YearMonth,
        today: kotlinx.datetime.LocalDate,
        selectedAccId: Long,
        filter: TransactionFilter,
        searchQuery: String,
        catMap: Map<CategoryId, Category>,
    ): List<Pair<kotlinx.datetime.LocalDate, TransactionUiModel>> {
        val q = searchQuery.trim().lowercase()
        val selectedType = filter.selectedType()
        val selectedCatIds = filter.selectedCategoryIds()
        return rules.flatMap { rule ->
            if (selectedAccId > 0L && rule.accountId.value != selectedAccId) return@flatMap emptyList()
            if (selectedType != null && rule.type != selectedType) return@flatMap emptyList()
            if (selectedCatIds.isNotEmpty() && rule.categoryId !in selectedCatIds) return@flatMap emptyList()
            if (q.isNotBlank()) {
                val catName = catMap[rule.categoryId]?.name?.lowercase() ?: ""
                val noteMatch = rule.note?.lowercase()?.contains(q) == true
                if (!catName.contains(q) && !noteMatch) return@flatMap emptyList()
            }
            val dates = RecurrenceMath.occurrencesInMonth(
                rule = rule.rule,
                endCondition = rule.endCondition,
                startDate = rule.startDate,
                ym = ym,
            )
            val cursor = rule.lastMaterializedDate
            dates
                .filter { it > today }
                .filter { cursor == null || it > cursor }
                .map { date ->
                    val cat = catMap[rule.categoryId]
                    date to TransactionUiModel(
                        id = com.dv.moneym.core.model.TransactionId(0L),
                        type = rule.type,
                        amountFormatted = rule.amount.format(),
                        amountMinorUnits = rule.amount.minorUnits,
                        currency = rule.amount.currency.value,
                        isExpense = rule.type == TransactionType.EXPENSE,
                        categoryName = cat?.name ?: "—",
                        categoryColorHex = cat?.colorHex ?: "#8A8A8A",
                        categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                        note = rule.note,
                        occurredOn = date,
                        paymentModeName = null,
                        isPending = true,
                        recurringId = rule.id,
                        rowKey = "pending_${rule.id.value}_$date",
                    )
                }
        }
    }
}

private fun Transaction.toUiModel(
    category: Category?,
    paymentModes: List<PaymentMode>,
    paymentModeEnabled: Boolean,
) = TransactionUiModel(
    id = id,
    type = type,
    amountFormatted = amount.format(),
    amountMinorUnits = amount.minorUnits,
    currency = amount.currency.value,
    isExpense = type == TransactionType.EXPENSE,
    categoryName = category?.name ?: "—",
    categoryColorHex = category?.colorHex ?: "#8A8A8A",
    categoryIcon = Icon.fromKeyOrDefault(category?.iconKey ?: Icon.Dots.key),
    note = note,
    occurredOn = occurredOn,
    paymentModeName = if (paymentModeEnabled) {
        paymentModeId?.let { id -> paymentModes.firstOrNull { it.id == id }?.name }
    } else null,
    recurringId = recurringId,
    rowKey = id.value.toString(),
)
