package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val dividerColor = colors.divider

    Column(modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MM.dimen.padding_7x)
                .drawBehind {
                    if (showDivider) {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth,
                        )
                    }
                },
        ) {
            // Leading: back chevron
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = MM.dimen.padding_1x),
                ) {
                    MmIconButton(
                        icon = MmIcons.chevronLeft,
                        onClick = onBack,
                        contentDescription = "Back",
                    )
                }
            }

            // Center: title
            Text(
                text = title,
                style = type.title3,
                color = colors.text,
                modifier = Modifier.align(Alignment.Center),
            )

            // Trailing slot
            if (trailingContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = MM.dimen.padding_1x),
                ) {
                    trailingContent()
                }
            }
        }
    } // end Column
}
