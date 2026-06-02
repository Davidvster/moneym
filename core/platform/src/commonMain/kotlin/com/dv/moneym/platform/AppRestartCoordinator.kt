package com.dv.moneym.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives an in-process app restart used by backup restore: tear down the Compose UI (so all
 * DB-bound Flows/ViewModels cancel), swap the database files while Koin is still alive, restart
 * the Koin graph, then remount the UI with the fresh graph.
 *
 * Deliberately a plain object, not a Koin singleton — it must outlive [restartKoin]'s `stopKoin()`.
 * [scope] and [restartKoin] are wired once at startup by the app entry point.
 */
object AppRestartCoordinator {
    enum class Phase { Idle, Restarting }

    private val _phase = MutableStateFlow(Phase.Idle)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> = _epoch.asStateFlow()

    lateinit var scope: CoroutineScope
    lateinit var restartKoin: suspend () -> Unit

    fun restart(swap: suspend () -> Unit) {
        scope.launch {
            _phase.value = Phase.Restarting
            delay(50)
            swap()
            restartKoin()
            _epoch.value += 1
            _phase.value = Phase.Idle
        }
    }
}
