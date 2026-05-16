package com.dv.moneym.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

enum class MmSegmentedSize { Sm, Md }

@Composable
fun MmSegmented(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: MmSegmentedSize = MmSegmentedSize.Md,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.radius

    val trackHeight = when (size) {
        MmSegmentedSize.Md -> 36.dp
        MmSegmentedSize.Sm -> 32.dp
    }
    val innerPadding = 3.dp

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "segmented_selection",
    )

    BoxWithConstraints(
        modifier = modifier
            .height(trackHeight)
            .clip(radius.pill)
            .background(colors.surface2, radius.pill)
            .padding(innerPadding),
    ) {
        val totalWidth = maxWidth
        val pillWidth = totalWidth / options.size
        val pillOffset = pillWidth * animatedIndex

        // Animated selected pill
        Box(
            modifier = Modifier
                .width(pillWidth)
                .fillMaxHeight()
                .offset(x = pillOffset)
                .shadow(1.dp, radius.pill)
                .clip(radius.pill)
                .background(colors.bg, radius.pill),
        )

        // Labels row
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(index) {
                            detectTapGestures(onTap = { onOptionSelected(index) })
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
}
