package com.dv.moneym.core.uigraphs.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/** Refined ease-out curve (no overshoot) shared by every chart's draw-on motion. */
val ChartEase = CubicBezierEasing(0.33f, 0f, 0f, 1f)

object ChartDurations {
    const val Entry = 600
    const val Select = 240
    const val Seek = 160
}

/**
 * Drives a 0f -> 1f entrance progress that restarts whenever [key] changes
 * (e.g. swiping to another month/year). Charts multiply their geometry by this
 * value for a uniform draw-on reveal.
 */
@Composable
fun rememberChartEntryProgress(key: Any?): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(key) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(ChartDurations.Entry, easing = ChartEase))
    }
    return anim.value
}

/**
 * Per-item staggered progress for cascading reveals. Returns each item's local
 * 0f..1f progress given the global [progress], the item [index] and item [count].
 * Earlier items finish first; the tail spans [spread] of the timeline.
 */
fun staggeredProgress(progress: Float, index: Int, count: Int, spread: Float = 0.4f): Float {
    if (count <= 1) return progress
    val start = (index.toFloat() / (count - 1)) * spread
    val end = start + (1f - spread)
    if (progress <= start) return 0f
    if (progress >= end) return 1f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}
