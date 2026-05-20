package com.dv.moneym.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberBinaryFilePicker(onResult: (ByteArray?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)

    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val bytes = url?.let { u ->
                    val data = NSData.create(contentsOfURL = u) ?: return@let null
                    val length = data.length.toInt()
                    if (length == 0) return@let ByteArray(0)
                    ByteArray(length).also { array ->
                        array.usePinned { pinned ->
                            memcpy(pinned.addressOf(0), data.bytes, data.length)
                        }
                    }
                }
                callback.value(bytes)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                callback.value(null)
            }
        }
    }

    return {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem))
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}
