package com.dv.moneym.data.banksync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BankAuthCallbackBus {
    private val _callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)
    val callbacks: SharedFlow<String> = _callbacks

    fun emit(url: String) {
        _callbacks.tryEmit(url)
    }
}
