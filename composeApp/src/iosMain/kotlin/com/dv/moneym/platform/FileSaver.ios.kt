package com.dv.moneym.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

@Composable
actual fun rememberFileSaver(onSaved: (String?) -> Unit): (ByteArray, String) -> Unit {
    val scope = rememberCoroutineScope()
    val filePlatform = remember { FilePlatform() }
    val callback = rememberUpdatedState(onSaved)

    return { bytes, fileName ->
        scope.launch {
            val path = filePlatform.saveFileLocallyBinary(fileName, bytes)
            callback.value(path)
        }
    }
}
