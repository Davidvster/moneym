package com.dv.moneym.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFilePicker(onResult: (String?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)

    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val content = url?.let {
                    @Suppress("UNCHECKED_CAST")
                    NSString.stringWithContentsOfURL(it, NSUTF8StringEncoding, null) as? String
                }
                callback.value(content)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                callback.value(null)
            }
        }
    }

    return {
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeItem),
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        dispatch_async(dispatch_get_main_queue()) {
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
