package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmErrorDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
) {
    MmDialog(
        title = title,
        confirmText = confirmText,
        onConfirm = onDismiss,
        onDismiss = onDismiss,
    ) {
        Text(message, style = MM.type.body, color = MM.colors.text2)
    }
}
