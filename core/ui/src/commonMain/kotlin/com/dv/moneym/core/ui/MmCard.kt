package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmCard(
    modifier: Modifier = Modifier,
    padded: Boolean = false,
    shape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val colors = MM.colors
    val radius = MM.radius
    val resolvedShape = shape ?: radius.lg

    Column(
        modifier = modifier
            .shadow(1.dp, resolvedShape)
            .clip(resolvedShape)
            .background(colors.surface, resolvedShape)
            .border(1.dp, colors.border, resolvedShape)
            .then(if (padded) Modifier.padding(20.dp) else Modifier),
    ) {
        content()
    }
}

@Preview
@Composable
private fun MmCardPreview() {
    MoneyMTheme {
        MmCard(Modifier.padding(16.dp).fillMaxWidth(), padded = true) {
            Text("Card content", style = MM.type.body, color = MM.colors.text)
        }
    }
}
