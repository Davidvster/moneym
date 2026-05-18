package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dv.moneym.core.designsystem.MM

@Composable
fun SectionLabel(
    text: String,
    color: Color = MM.colors.text3,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MM.type.micro,
        color = color,
        modifier = modifier,
    )
}
