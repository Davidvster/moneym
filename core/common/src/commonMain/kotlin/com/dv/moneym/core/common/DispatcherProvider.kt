package com.dv.moneym.core.common

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

expect class DefaultDispatcherProvider() : DispatcherProvider
