package com.dv.moneym.feature.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dv.moneym.core.designsystem.MoneyMTheme

private val KEYS = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -2)  // -1 = empty, -2 = delete

@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val sp = MoneyMTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(sp.sm),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
        userScrollEnabled = false,
    ) {
        items(KEYS) { key ->
            when (key) {
                -1 -> Box(modifier = Modifier.aspectRatio(1.5f)) // empty cell
                -2 -> TextButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier.aspectRatio(1.5f),
                ) {
                    Text("⌫", style = MaterialTheme.typography.headlineSmall)
                }
                else -> FilledTonalButton(
                    onClick = { onDigit(key) },
                    enabled = enabled,
                    modifier = Modifier.aspectRatio(1.5f),
                ) {
                    Text(
                        text = "$key",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
fun PinDots(
    length: Int,
    filled: Int,
    modifier: Modifier = Modifier,
) {
    val sp = MoneyMTheme.spacing
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(sp.lg, Alignment.CenterHorizontally),
    ) {
        repeat(length) { i ->
            Text(
                text = if (i < filled) "●" else "○",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
