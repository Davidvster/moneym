package com.dv.moneym.feature.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DateStyle
import com.dv.moneym.core.common.formatDate
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

internal data class TransactionPageUiState(
    val isLoading: Boolean = true,
    val dayGroups: List<DayGroup> = emptyList(),
    val isEmpty: Boolean = false,
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
)

class TransactionPageViewModel(
    private val yearMonth: YearMonth,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val appSettingsRepository: AppSettingsRepository,
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
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = combine(
        appSettingsRepository.observeLastTransactionFilter(),
        appSettingsRepository.observeTxDisplayPrefs(),
        appSettingsRepository.observePaymentModeEnabled(),
        categoryRepository.observeAll(),
    ) { filter, prefs, pmEnabled, cats ->
        FilterBase(filter, prefs, pmEnabled, cats.associateBy { it.id })
    }.flatMapLatest { base ->
        val txnFlow = if (base.filter == TransactionFilter.None) {
            transactionRepository.observeByMonth(yearMonth.year, yearMonth.monthNumber)
        } else {
            transactionRepository.observeFiltered(base.filter).map { all ->
                all.filter {
                    it.occurredOn.year == yearMonth.year &&
                        it.occurredOn.monthNumber == yearMonth.monthNumber
                }
            }
        }
        combine(
            txnFlow,
            appSettingsRepository.observeSelectedAccountId(),
            accountRepository.observeAll(),
            paymentModeRepository.observeAll(),
            ephemeralFilters,
        ) { transactions, selectedAccId, accounts, paymentModes, (searchQuery, selectedCatIds) ->
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

            val dayGroups = filtered
                .groupBy { it.occurredOn }
                .map { (date, txns) ->
                    DayGroup(
                        date = date,
                        label = formatDate(date, DateStyle.Full),
                        transactions = txns.map {
                            it.toUiModel(base.catMap[it.categoryId], paymentModes, base.pmEnabled)
                        },
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
)
