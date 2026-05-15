package com.dv.moneym.core.testing

import com.dv.moneym.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class TestDispatcherProvider(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}

fun runTestWithDispatchers(
    dispatcher: TestDispatcher = StandardTestDispatcher(),
    block: suspend TestScope.(TestDispatcherProvider) -> Unit,
) = runTest(dispatcher) {
    block(TestDispatcherProvider(dispatcher))
}
