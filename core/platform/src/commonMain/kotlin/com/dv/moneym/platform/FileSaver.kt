package com.dv.moneym.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFileSaver(onSaved: (path: String?) -> Unit): (bytes: ByteArray, fileName: String) -> Unit

@Composable
expect fun rememberFolderPicker(onResult: (uri: String?) -> Unit): () -> Unit
