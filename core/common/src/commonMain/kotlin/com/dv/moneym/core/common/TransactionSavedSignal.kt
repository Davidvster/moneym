package com.dv.moneym.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.LocalDate

// App-wide one-shot signal emitted when a new transaction is saved, carrying the date it
// was filed under. The transaction list observes this to jump to that transaction's month.
class TransactionSavedSignal {
    private val _savedDates = MutableSharedFlow<LocalDate>(extraBufferCapacity = 1)
    val savedDates: SharedFlow<LocalDate> = _savedDates.asSharedFlow()

    fun notifySaved(date: LocalDate) {
        _savedDates.tryEmit(date)
    }
}
