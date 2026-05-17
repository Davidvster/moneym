# Phase 1 — Refactor Grid System (MoneyMSpacing + MoneyMRadius)

## Goal
Replace the current `MoneyMSpacing` and `MoneyMRadius` classes with a flat set of named constants matching the requested grid values. Update `MM` object accordingly.

## Files to Edit

- `/Users/davidvalic/Developer/MoneyM2/core/designsystem/src/commonMain/kotlin/com/dv/moneym/core/designsystem/MoneyMSpacing.kt`
- `/Users/davidvalic/Developer/MoneyM2/core/designsystem/src/commonMain/kotlin/com/dv/moneym/core/designsystem/MoneyMRadius.kt`

## Exact Changes Needed

### MoneyMSpacing.kt

Replace the entire file content with:

```kotlin
package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoneyMSpacing(
    val padding_0_25x: Dp = 2.dp,
    val padding_0_5x: Dp  = 4.dp,
    val padding_1x: Dp    = 8.dp,
    val padding_1_25x: Dp = 10.dp,
    val padding_1_5x: Dp  = 12.dp,
    val padding_2x: Dp    = 16.dp,
    val padding_3x: Dp    = 24.dp,
    val padding_4x: Dp    = 32.dp,
    val padding_5x: Dp    = 40.dp,
    val padding_6x: Dp    = 48.dp,
    val padding_7x: Dp    = 56.dp,
    val padding_8x: Dp    = 64.dp,
    val padding_9x: Dp    = 72.dp,
    val padding_10x: Dp   = 80.dp,
) {
    // Legacy aliases — kept for backward compatibility during migration
    val xxs: Dp get() = padding_0_25x
    val xs: Dp  get() = padding_0_5x
    val sm: Dp  get() = padding_1x
    val md: Dp  get() = padding_1_5x
    val lg: Dp  get() = padding_2x
    val xl: Dp  get() = padding_3x
    val xxl: Dp get() = padding_4x
    // Additional legacy s-series aliases
    val s1: Dp  get() = padding_0_5x
    val s2: Dp  get() = padding_1x
    val s3: Dp  get() = padding_1_5x
    val s4: Dp  get() = padding_2x
    val s5: Dp  get() = 20.dp
    val s6: Dp  get() = padding_3x
    val s8: Dp  get() = padding_4x
    val s10: Dp get() = padding_5x
    val s12: Dp get() = padding_6x
}

val LocalMoneyMSpacing = staticCompositionLocalOf { MoneyMSpacing() }
```

### MoneyMRadius.kt

Replace the entire file content with:

```kotlin
package com.dv.moneym.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class MoneyMRadius(
    val radius_0_5x: RoundedCornerShape = RoundedCornerShape(4.dp),
    val radius_1x: RoundedCornerShape   = RoundedCornerShape(8.dp),
    val radius_2x: RoundedCornerShape   = RoundedCornerShape(16.dp),
    val radius_3x: RoundedCornerShape   = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape        = RoundedCornerShape(50),
) {
    // Legacy aliases — kept for backward compatibility
    val xs: RoundedCornerShape  get() = radius_0_5x
    val sm: RoundedCornerShape  get() = radius_1x
    val md: RoundedCornerShape  get() = RoundedCornerShape(12.dp)
    val lg: RoundedCornerShape  get() = radius_2x
    val xl: RoundedCornerShape  get() = RoundedCornerShape(20.dp)
}

val LocalMoneyMRadius = staticCompositionLocalOf { MoneyMRadius() }
```

## Notes
- All existing code uses the legacy aliases (`MM.space.sm`, `MM.radius.md`, etc.) — the aliases ensure no breaks.
- Do NOT change any call sites in this phase.
- The `MM.kt` object does NOT need changes since it references `LocalMoneyMSpacing` and `LocalMoneyMRadius` which remain.

## Acceptance Criteria
1. `MoneyMSpacing` has all new `padding_*` properties with correct dp values.
2. `MoneyMRadius` has all new `radius_*` properties with correct dp values.
3. All legacy aliases (`xxs`, `xs`, `sm`, `md`, `lg`, `xl`, `xxl`, `s1`–`s12`) still exist and map correctly.
4. `./gradlew :composeApp:assembleDebug` passes with no errors.
