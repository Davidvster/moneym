package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = type.title3, color = colors.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x), content = content)
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmText, color = if (confirmEnabled) colors.accent else colors.text3)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(it, color = colors.text2)
                }
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}
