package com.dv.moneym.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.mmStaggeredAppear(index: Int, baseDelayMs: Int = 50): Modifier = composed {
    // Snap to the settled state under inspection (Compose @Preview / Paparazzi), where the
    // animation clock never advances and the content would otherwise stay invisible.
    if (LocalInspectionMode.current) return@composed this
    val alpha = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 12.dp.toPx() }
    val translation = remember { Animatable(offsetPx) }
    LaunchedEffect(Unit) {
        delay(index.toLong() * baseDelayMs)
        launch {
            alpha.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
        }
        launch {
            translation.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
        }
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = translation.value
    }
}
