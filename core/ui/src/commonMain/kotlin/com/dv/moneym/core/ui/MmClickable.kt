package com.dv.moneym.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.dv.moneym.core.designsystem.MM

/**
 * Material click handling with a ripple state-layer, replacing the old
 * `detectTapGestures` + `.alpha(if (pressed)…)` opacity hack used across the app.
 *
 * Unlike `Modifier.clickable`, this drives the ripple from `detectTapGestures`'s
 * `onPress`, so the ripple appears the instant the finger lands — even inside a
 * scrolling list, where `clickable` otherwise delays the indication ~150ms to
 * disambiguate taps from scrolls. Feedback is therefore immediate and uniform
 * for buttons, rows, chips and keys alike.
 *
 * Apply **after** any `.clip(shape)` (or pass [shape]) so a bounded ripple is
 * masked to the shape. Use `bounded = false` (+ `radius`) for circular icon/key
 * taps.
 *
 * Default [rippleColor] is `MM.colors.text` so the ripple contrasts in both
 * light and dark themes; pass a contrasting color on dark/accent surfaces.
 */
@Composable
fun Modifier.mmClickable(
    enabled: Boolean = true,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    rippleColor: Color = Color.Unspecified,
    shape: Shape? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val color = if (rippleColor == Color.Unspecified) MM.colors.text else rippleColor
    val currentOnClick by rememberUpdatedState(onClick)

    val base = if (shape != null) this.clip(shape) else this
    if (!enabled) return base

    return base
        .indication(interaction, ripple(bounded = bounded, radius = radius, color = color))
        .semantics(mergeDescendants = true) {
            role?.let { this.role = it }
            onClick { currentOnClick(); true }
        }
        .pointerInput(interaction) {
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interaction.emit(press)
                    val released = tryAwaitRelease()
                    interaction.emit(
                        if (released) PressInteraction.Release(press)
                        else PressInteraction.Cancel(press),
                    )
                },
                onTap = { currentOnClick() },
            )
        }
}
