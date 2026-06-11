package com.dv.moneym.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

enum class MmSegmentedSize { Sm, Md }

/**
 * Pill-shaped segmented control.
 *
 * @param fillWidth When true, expands to fill available width (e.g. full-width filters).
 *                  When false (default), wraps content using a fixed per-option width.
 */
@Composable
fun MmSegmented(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: MmSegmentedSize = MmSegmentedSize.Md,
    fillWidth: Boolean = false,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen
    val haptic = LocalHapticFeedback.current

    val trackHeight: Dp = when (size) {
        MmSegmentedSize.Md -> 36.dp
        MmSegmentedSize.Sm -> MM.dimen.padding_4x
    }
    val innerPadding = 3.dp

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "segmented_selection",
    )

    if (fillWidth) {
        // Expand to fill parent; pill width derived from available space.
        BoxWithConstraints(
            modifier = modifier
                .height(trackHeight)
                .clip(radius.pill)
                .background(colors.surface2, radius.pill)
                .padding(innerPadding),
        ) {
            val pillWidth = maxWidth / options.size
            val pillOffset = pillWidth * animatedIndex

            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .fillMaxHeight()
                    .offset(x = pillOffset)
                    .shadow(1.dp, radius.pill)
                    .clip(radius.pill)
                    .background(colors.bg, radius.pill),
            )
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (index != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                                onOptionSelected(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            style = type.caption.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) colors.text else colors.text2,
                            ),
                        )
                    }
                }
            }
        }
    } else {
        // Wrap content; every option gets the same slot width, sized to the widest label
        // so no locale's text is clipped while the pill still relies on equal-width slots.
        val fallbackWidth: Dp = when (size) {
            MmSegmentedSize.Md -> 52.dp
            MmSegmentedSize.Sm -> 44.dp
        }
        val hPadding = MM.dimen.padding_2x
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val labelStyle = type.caption.copy(fontWeight = FontWeight.SemiBold)
        val optionWidth: Dp = remember(options, labelStyle, hPadding, fallbackWidth) {
            val widest = options.maxOfOrNull {
                with(density) { textMeasurer.measure(it, labelStyle).size.width.toDp() }
            } ?: 0.dp
            maxOf(widest + hPadding * 2, fallbackWidth)
        }
        val trackWidth = optionWidth * options.size + innerPadding * 2
        val pillOffset = optionWidth * animatedIndex

        Box(
            modifier = modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(radius.pill)
                .background(colors.surface2, radius.pill)
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .width(optionWidth)
                    .fillMaxHeight()
                    .offset(x = pillOffset)
                    .shadow(1.dp, radius.pill)
                    .clip(radius.pill)
                    .background(colors.bg, radius.pill),
            )
            Row {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .width(optionWidth)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (index != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                                onOptionSelected(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            style = type.caption.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) colors.text else colors.text2,
                            ),
                            modifier = Modifier.padding(horizontal = hPadding),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MmSegmentedPreview() {
    MoneyMTheme {
        Column(
            Modifier.padding(MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)
        ) {
            MmSegmented(listOf("Month", "Year"), selectedIndex = 0, onOptionSelected = {})
            MmSegmented(
                listOf("All", "Expenses", "Income"),
                selectedIndex = 1,
                onOptionSelected = {},
                fillWidth = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
