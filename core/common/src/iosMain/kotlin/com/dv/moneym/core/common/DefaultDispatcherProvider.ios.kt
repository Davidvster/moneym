package com.dv.moneym.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class DefaultDispatcherProvider actual constructor() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
    // Kotlin/Native has no Dispatchers.IO; use a bounded pool on Default instead
    override val io: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(64)
}
