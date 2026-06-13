# Phase 1 — Reusable themed `MmSnackbar` (task 4)

## Goal
Replace the Android-default-looking snackbar (bank reject undo) with a snackbar that matches the MoneyM look & feel, packaged as a reusable component in `core/ui`.

## Files
1. NEW `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmSnackbar.kt`
2. EDIT `feature/banksync/src/commonMain/kotlin/com/dv/moneym/feature/banksync/suggestions/BankSuggestionsScreen.kt`

No string changes (reuses `suggestions_rejected_snackbar`, `suggestions_undo`).

## 1. MmSnackbar.kt
Create a themed snackbar host. Use the design tokens via `com.dv.moneym.core.designsystem.MM` (`MM.colors`, `MM.type`, `MM.dimen`).

```kotlin
package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.dv.moneym.core.designsystem.MM

/**
 * Themed snackbar host matching the app surface/typography instead of the
 * platform default. Place inside a Box and align as needed.
 */
@Composable
fun MmSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Surface(
            color = colors.surface2,
            contentColor = colors.text,
            shape = RoundedCornerShape(space.radius_1x), // pick an existing radius token; see MoneyMDimensions
            border = BorderStroke(1.dp, colors.border),  // import androidx.compose.foundation.BorderStroke + dp
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.padding(space.padding_1x),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = space.padding_2x,
                    vertical = space.padding_1_5x,
                ),
                verticalAlignment = Alignment.CenterVertically, // import Alignment
            ) {
                Text(
                    text = data.visuals.message,
                    style = type.body,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                data.visuals.actionLabel?.let { label ->
                    Spacer(Modifier.width(space.padding_2x))
                    TextButton(onClick = { data.performAction() }) {
                        Text(text = label, style = type.body, color = colors.accent)
                    }
                }
            }
        }
    }
}
```

Notes for the builder:
- Open `core/designsystem/.../MoneyMDimensions.kt` and pick the closest existing radius token (e.g. `radius_1x` / `radius_1_5x`) and spacing tokens that actually exist — do NOT invent token names. Match the corner radius used by `MmCard`.
- Confirm `MM.colors` has `surface2`, `border`, `text`, `accent` (it does). If `surface2` feels too subtle vs the screen bg (`colors.bg`), use `colors.surface`.
- Keep imports explicit (project convention: import the class, no FQNs).
- No drag handle, no Material default elevation glow — flat themed surface with a hairline border, consistent with `MmCard`/sheets.

## 2. BankSuggestionsScreen.kt
- Replace the bare `SnackbarHost(...)` at lines ~252-255 with `MmSnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(space.padding_2x))`.
- Update the import: remove `androidx.compose.material3.SnackbarHost`, add `com.dv.moneym.core.ui.MmSnackbarHost`. Keep `SnackbarHostState`, `SnackbarDuration`, `SnackbarResult` imports (still used at lines 116-123).
- The `showSnackbar` call (lines 116-123) is unchanged.

## Verify
`./gradlew :core:ui:compileDebugKotlinAndroid :feature:banksync:compileDebugKotlinAndroid`
(Kotlin task names must be variant-qualified.)
