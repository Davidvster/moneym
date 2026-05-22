package com.dv.moneym.platform

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileSaver(onSaved: (String?) -> Unit): (ByteArray, String) -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onSaved)
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val bytes = pendingBytes
        pendingBytes = null
        val path = uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes ?: ByteArray(0)) }
                it.toString()
            }.getOrNull()
        }
        callback.value(path)
    }

    return { bytes, fileName ->
        pendingBytes = bytes
        launcher.launch(fileName)
    }
}

@Composable
actual fun rememberFolderPicker(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        callback.value(uri?.toString())
    }

    return { launcher.launch(null) }
}
