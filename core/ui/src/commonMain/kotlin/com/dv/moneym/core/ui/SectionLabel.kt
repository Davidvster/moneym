package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

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

@Preview
@Composable
private fun SectionLabelPreview() {
    MoneyMTheme {
        SectionLabel(text = "Section", modifier = Modifier.padding(MM.dimen.padding_2x))
    }
}
