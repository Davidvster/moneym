package com.dv.moneym.feature.transactions.list.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.onHorizontalSwipe(
    thresholdDp: Float = 60f,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    val thresholdPx = thresholdDp * density
    var totalX = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalX = 0f },
        onDragEnd = {
            when {
                totalX > thresholdPx -> onSwipeRight()
                totalX < -thresholdPx -> onSwipeLeft()
            }
            totalX = 0f
        },
        onHorizontalDrag = { change, delta ->
            change.consume()
            totalX += delta
        },
    )
}
