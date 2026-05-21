package com.dv.moneym.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePicker(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        val content = uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
        }
        callback.value(content)
    }
    return { launcher.launch("*/*") }
}
