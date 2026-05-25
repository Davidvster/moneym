package com.dv.moneym.feature.transactions.list.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DateStyle
import com.dv.moneym.core.common.formatDate
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val ephemeralFilters = combine(
        ephemeralState.searchQuery,
        ephemeralState.selectedCategoryIds,
    ) { q, ids -> q to ids }

    private data class FilterBase(
        val filter: TransactionFilter,
        val prefs: TxDisplayPrefs,
        val pmEnabled: Boolean,
        val catMap: Map<CategoryId, Category>,
        val showPending: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = combine(
        appSettingsRepository.observeLastTransactionFilter(),
        appSettingsRepository.observeTxDisplayPrefs(),
        appSettingsRepository.observePaymentModeEnabled(),
        categoryRepository.observeAll(),
        appSettingsRepository.observeShowPendingRecurring(),
    ) { filter, prefs, pmEnabled, cats, showPending ->
        FilterBase(filter, prefs, pmEnabled, cats.associateBy { it.id }, showPending)
    }.flatMapLatest { base ->
        val txnFlow = if (base.filter == TransactionFilter.None) {
            transactionRepository.observeByMonth(yearMonth.year, yearMonth.monthNumber)
        } else {
            transactionRepository.observeFiltered(base.filter).map { all ->
                all.filter {
                    it.occurredOn.year == yearMonth.year &&
                            it.occurredOn.month.number == yearMonth.monthNumber
                }
            }
        }
        combine(
            txnFlow,
            appSettingsRepository.observeSelectedAccountId(),
            accountRepository.observeAll(),
            paymentModeRepository.observeAll(),
            ephemeralFilters,
            recurringTransactionRepository.observeAll(),
        ) { args ->
            @Suppress("UNCHECKED_CAST")
            val transactions = args[0] as List<Transaction>
            val selectedAccId = args[1] as Long
            val paymentModes = args[3] as List<PaymentMode>
            val (searchQuery, selectedCatIds) = args[4] as Pair<String, Set<CategoryId>>
            val rules = args[5] as List<RecurringTransaction>

            val accountFilteredTxns = if (selectedAccId > 0L) {
                transactions.filter { it.accountId.value == selectedAccId }
            } else transactions

            val filtered = accountFilteredTxns
                .filter { tx ->
                    selectedCatIds.isEmpty() || tx.categoryId in selectedCatIds
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
                selectedCatIds = selectedCatIds,
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

            TransactionPageUiState(
                isLoading = false,
                dayGroups = dayGroups,
                isEmpty = dayGroups.isEmpty(),
                txDisplayPrefs = base.prefs,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TransactionPageUiState(),
    )

    private fun buildPendingForMonth(
        rules: List<RecurringTransaction>,
        ym: YearMonth,
        today: kotlinx.datetime.LocalDate,
        selectedAccId: Long,
        selectedCatIds: Set<CategoryId>,
        searchQuery: String,
        catMap: Map<CategoryId, Category>,
    ): List<Pair<kotlinx.datetime.LocalDate, TransactionUiModel>> {
        val q = searchQuery.trim().lowercase()
        return rules.flatMap { rule ->
            if (selectedAccId > 0L && rule.accountId.value != selectedAccId) return@flatMap emptyList()
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
