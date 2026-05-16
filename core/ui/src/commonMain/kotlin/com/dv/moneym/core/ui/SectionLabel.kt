package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    Text(
        text = text.uppercase(),
        style = type.micro,
        color = colors.text3,
        modifier = modifier,
    )
}
