package com.dv.moneym.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIModalPresentationFormSheet

@Composable
actual fun rememberFileSaver(onSaved: (String?) -> Unit): (ByteArray, String) -> Unit {
    val scope = rememberCoroutineScope()
    val filePlatform = remember { FilePlatform() }
    val callback = rememberUpdatedState(onSaved)

    return { bytes, fileName ->
        scope.launch {
            val path = filePlatform.saveFileLocallyBinary(fileName, bytes)
            withContext(Dispatchers.Main) {
                if (path != null) {
                    val fileUrl = NSURL.fileURLWithPath(path)
                    val activityVc = UIActivityViewController(
                        activityItems = listOf(fileUrl),
                        applicationActivities = null,
                    )
                    activityVc.modalPresentationStyle = UIModalPresentationFormSheet
                    topViewController()?.presentViewController(activityVc, animated = true, completion = null)
                }
            }
            callback.value(path)
        }
    }
}

@Composable
actual fun rememberFolderPicker(onResult: (String?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    return { callback.value("default") }
}
