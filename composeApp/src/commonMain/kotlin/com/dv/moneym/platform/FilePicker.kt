package com.dv.moneym.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(onResult: (String?) -> Unit): () -> Unit
