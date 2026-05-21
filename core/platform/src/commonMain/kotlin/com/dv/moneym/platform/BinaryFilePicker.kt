package com.dv.moneym.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBinaryFilePicker(onResult: (ByteArray?) -> Unit): () -> Unit
