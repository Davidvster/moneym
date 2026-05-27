package com.dv.moneym.core.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Suppress("ComposableNaming")
@Composable
fun <T> Flow<T>.observeWithLifecycle(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (value: T) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(lifecycleState) {
            collect { action(it) }
        }
    }
}
