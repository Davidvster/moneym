package com.dv.moneym.feature.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.OverviewPeriodMode
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class OverviewViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val today get() = clock.today()

    private val _currentPeriod by savedStateHandle.saved {
        MutableStateFlow<OverviewPeriod>(OverviewPeriod.Month(YearMonth(today.year, today.monthNumber)))
    }
    private val _spendingFilter = MutableStateFlow(SpendingFilter.Expenses)
    private val _selectedAccountId by savedStateHandle.saved { MutableStateFlow<Long>(-1L) }

    private val _dateBounds: StateFlow<Pair<String?, String?>> = transactionRepository
        .getTransactionDates()
        .map { dates -> dates.minOrNull()?.toString() to dates.maxOrNull()?.toString() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null to null)

    private val _transactionDateIsos = transactionRepository.getTransactionDates()
        .map { dates -> dates.map { it.toString() }.toSet() }

    private val _earliestMonth: StateFlow<YearMonth?> = transactionRepository
        .getTransactionDates()
        .map { dates -> dates.minOrNull()?.let { YearMonth(it.year, it.monthNumber) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun init() {
        viewModelScope.launch {
            val saved = appSettingsRepository.observeLastOverviewPeriod().first()
            when (saved) {
                OverviewPeriodMode.Year -> _currentPeriod.update {
                    OverviewPeriod.Year(today.year)
                }
                OverviewPeriodMode.DateRange -> {}
                OverviewPeriodMode.Month -> {}
            }
        }
        viewModelScope.launch {
            appSettingsRepository.observeSelectedAccountId().collect { id ->
                _selectedAccountId.value = id
            }
        }
    }

    internal val state: StateFlow<OverviewUiState> = combine(
        _currentPeriod.onStart { init() },
        _spendingFilter,
        _earliestMonth,
        _dateBounds,
        combine(_selectedAccountId, accountRepository.observeAll()) { id, accs -> id to accs },
    ) { period, spendingFilter, earliestMonth, (minIso, maxIso), (selectedAccId, accounts) ->
        val todayYearMonth = YearMonth(today.year, today.monthNumber)
        val monthAnchor = earliestMonth ?: todayYearMonth
        val yearAnchor = earliestMonth?.year ?: today.year

        val monthCurrentPage = when (val p = period) {
            is OverviewPeriod.Month -> yearMonthToPage(p.yearMonth, monthAnchor)
            else -> yearMonthToPage(todayYearMonth, monthAnchor)
        }
        val monthPageCount = yearMonthToPage(todayYearMonth, monthAnchor) + 1 + 120

        val yearCurrentPage = when (val p = period) {
            is OverviewPeriod.Year -> yearToPage(p.year, yearAnchor)
            else -> yearToPage(today.year, yearAnchor)
        }
        val yearPageCount = yearToPage(today.year, yearAnchor) + 1 + 10

        val canGoBack = when (val p = period) {
            is OverviewPeriod.Month -> {
                val minDate = minIso?.let { LocalDate.parse(it) }
                if (minDate == null) true
                else YearMonth(p.yearMonth.year, p.yearMonth.monthNumber) >
                    YearMonth(minDate.year, minDate.monthNumber)
            }
            is OverviewPeriod.Year -> {
                val minYear = minIso?.let { LocalDate.parse(it).year }
                minYear == null || p.year > minYear
            }
            is OverviewPeriod.DateRange -> false
        }

        val currency = (if (selectedAccId > 0L) accounts.find { it.id.value == selectedAccId }
            else accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())
            ?.currency?.value ?: "USD"

        OverviewUiState(
            currentPeriod = period,
            canGoBack = canGoBack,
            spendingFilter = spendingFilter,
            monthAnchor = monthAnchor,
            monthCurrentPage = monthCurrentPage,
            monthPageCount = monthPageCount,
            yearAnchor = yearAnchor,
            yearCurrentPage = yearCurrentPage,
            yearPageCount = yearPageCount,
            minSelectableDateIso = minIso,
            maxSelectableDateIso = maxIso,
            currency = currency,
        )
    }
        .combine(_transactionDateIsos) { s, isos -> s.copy(transactionDateIsos = isos) }
        .stateIn(viewModelScope, SharingStarted.Lazily, OverviewUiState())

    internal fun onIntent(intent: OverviewIntent) {
        when (intent) {
            OverviewIntent.PreviousPeriod -> {
                val minIso = _dateBounds.value.first
                _currentPeriod.update { period ->
                    val prev = period.previous()
                    if (minIso != null) {
                        val minDate = LocalDate.parse(minIso)
                        when (prev) {
                            is OverviewPeriod.Month ->
                                if (YearMonth(prev.yearMonth.year, prev.yearMonth.monthNumber) <
                                    YearMonth(minDate.year, minDate.monthNumber)) period else prev
                            is OverviewPeriod.Year ->
                                if (prev.year < minDate.year) period else prev
                            else -> prev
                        }
                    } else prev
                }
            }

            OverviewIntent.NextPeriod -> _currentPeriod.update { it.next() }

            OverviewIntent.TogglePeriod -> {
                _currentPeriod.update { period ->
                    val newPeriod = when (period) {
                        is OverviewPeriod.Month -> OverviewPeriod.Year(period.yearMonth.year)
                        is OverviewPeriod.Year -> OverviewPeriod.Month(
                            YearMonth(period.year, today.monthNumber)
                        )
                        is OverviewPeriod.DateRange -> OverviewPeriod.Month(
                            YearMonth(today.year, today.monthNumber)
                        )
                    }
                    persistPeriod(newPeriod)
                    newPeriod
                }
            }

            is OverviewIntent.PeriodSelected -> {
                _currentPeriod.value = intent.period
                persistPeriod(intent.period)
            }

            is OverviewIntent.DateRangeSelected -> {
                val newPeriod = OverviewPeriod.DateRange(
                    startYear = intent.startYear,
                    startMonth = intent.startMonth,
                    startDay = intent.startDay,
                    endYear = intent.endYear,
                    endMonth = intent.endMonth,
                    endDay = intent.endDay,
                )
                _currentPeriod.value = newPeriod
                persistPeriod(newPeriod)
            }

            is OverviewIntent.SpendingFilterChanged -> _spendingFilter.value = intent.filter

            is OverviewIntent.MonthPagerSwiped -> {
                _currentPeriod.update { OverviewPeriod.Month(intent.yearMonth) }
            }

            is OverviewIntent.YearPagerSwiped -> {
                _currentPeriod.update { OverviewPeriod.Year(intent.year) }
            }
        }
    }

    private fun persistPeriod(period: OverviewPeriod) {
        val mode = when (period) {
            is OverviewPeriod.Month -> OverviewPeriodMode.Month
            is OverviewPeriod.Year -> OverviewPeriodMode.Year
            is OverviewPeriod.DateRange -> OverviewPeriodMode.DateRange
        }
        viewModelScope.launch { appSettingsRepository.setLastOverviewPeriod(mode) }
    }

    private fun OverviewPeriod.previous(): OverviewPeriod = when (this) {
        is OverviewPeriod.Month -> OverviewPeriod.Month(yearMonth.previous())
        is OverviewPeriod.Year -> OverviewPeriod.Year(year - 1)
        is OverviewPeriod.DateRange -> this
    }

    private fun OverviewPeriod.next(): OverviewPeriod = when (this) {
        is OverviewPeriod.Month -> OverviewPeriod.Month(yearMonth.next())
        is OverviewPeriod.Year -> OverviewPeriod.Year(year + 1)
        is OverviewPeriod.DateRange -> this
    }
}
