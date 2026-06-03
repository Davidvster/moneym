package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

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

@Preview
@Composable
private fun MmErrorDialogPreview() {
    MoneyMTheme {
        MmErrorDialog(
            title = "Something went wrong",
            message = "Could not save your transaction. Please try again.",
            confirmText = "OK",
            onDismiss = {},
        )
    }
}
