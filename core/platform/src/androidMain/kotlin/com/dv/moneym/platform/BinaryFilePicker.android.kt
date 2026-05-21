package com.dv.moneym.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBinaryFilePicker(onResult: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        val bytes = uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() }
            }.getOrNull()
        }
        callback.value(bytes)
    }
    return { launcher.launch("*/*") }
}
