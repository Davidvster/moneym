# Phase 7 — Replace Raw .dp Literals with Grid Constants

## Goal
Replace raw `.dp` literals in all feature UI files with the canonical `MM.space.*` and `MM.radius.*` grid constants added in Phase 1, where a match exists. Only replace values that exactly match a grid constant. Leave unique/non-standard values as-is.

## Mapping Reference

From Phase 1, `MM.space` now has (via `MoneyMSpacing`):
```
padding_0_25x = 2.dp
padding_0_5x  = 4.dp
padding_1x    = 8.dp
padding_1_25x = 10.dp
padding_1_5x  = 12.dp
padding_2x    = 16.dp
padding_3x    = 24.dp
padding_4x    = 32.dp
padding_5x    = 40.dp
padding_6x    = 48.dp
padding_7x    = 56.dp
padding_8x    = 64.dp
padding_9x    = 72.dp
padding_10x   = 80.dp

Legacy aliases still work:
sm = 8.dp, md = 12.dp, lg = 16.dp, xl = 24.dp, xxl = 32.dp
```

From Phase 1, `MM.radius` now has:
```
radius_0_5x = 4.dp corner
radius_1x   = 8.dp corner
radius_2x   = 16.dp corner
radius_3x   = 24.dp corner
pill        = 50% corner
```

## Files to Edit

Edit ALL of these files (read each one first, then make targeted replacements):

1. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`
2. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/SettingsScreen.kt`
3. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/CurrencyPickerScreen.kt`
4. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/LanguagePickerScreen.kt`
5. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/TxListDisplayScreen.kt`
6. `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/ui/CategoryListScreen.kt`
7. `/Users/davidvalic/Developer/MoneyM2/feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt`
8. `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt`
9. `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinSetupScreen.kt`
10. `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinUnlockScreen.kt`
11. `/Users/davidvalic/Developer/MoneyM2/feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/ui/OnboardingScreen.kt`

## Replacement Rules

Inside `@Composable` functions where `MM.space` is accessible (i.e., inside composable context), replace:

### Padding values (use `MM.space.*`):
- `2.dp` → `MM.space.padding_0_25x` (only when used as spacing/padding, not sizes)
- `4.dp` → `MM.space.padding_0_5x`
- `8.dp` → `MM.space.padding_1x`
- `10.dp` → `MM.space.padding_1_25x`
- `12.dp` → `MM.space.padding_1_5x`
- `16.dp` → `MM.space.padding_2x`
- `24.dp` → `MM.space.padding_3x`
- `32.dp` → `MM.space.padding_4x`
- `40.dp` → `MM.space.padding_5x`
- `48.dp` → `MM.space.padding_6x`
- `56.dp` → `MM.space.padding_7x`
- `64.dp` → `MM.space.padding_8x`

### Shape/radius values (use `MM.radius.*`):
- `RoundedCornerShape(8.dp)` → `MM.radius.radius_1x`
- `RoundedCornerShape(16.dp)` → `MM.radius.radius_2x`
- `RoundedCornerShape(24.dp)` → `MM.radius.radius_3x`
- `RoundedCornerShape(50)` → `MM.radius.pill`
- `RoundedCornerShape(4.dp)` → `MM.radius.radius_0_5x`

## DO NOT Replace
- Component-specific sizes (e.g., `Modifier.size(18.dp)` for icon size — this is a component size, not a grid constant)
- Chart dimensions like `height(120.dp)`, `height(140.dp)`, `height(32.dp)` — keep as raw dp
- Animation-specific values
- `1.dp` (divider thickness) — keep as raw
- `1.5.dp` (border width) — keep as raw
- `20.dp` — no exact constant, keep as raw
- `6.dp`, `14.dp`, `18.dp`, `22.dp` — no exact match, keep as raw
- Sizes used in `Modifier.size()` for non-padding purposes
- The `RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)` — non-standard value, keep

## Strategy

For each file:
1. Read the file.
2. Find all `.dp` literals used in padding/spacing context.
3. Replace matching values with `MM.space.*` or `MM.radius.*`.
4. Add `val space = MM.space` and `val radius = MM.radius` at the top of each composable function (alongside existing `val colors = MM.colors` etc.) to avoid repeated `MM.space.xxx` calls. OR, if the file already assigns these, use the local variable.

Important: `MM.space` and `MM.radius` are only available inside `@Composable` functions. For non-composable uses (e.g., in object definitions), keep raw `.dp`.

## Example

Before:
```kotlin
val colors = MM.colors
val type = MM.type

Column(
    modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
)
```

After:
```kotlin
val colors = MM.colors
val type = MM.type
val space = MM.space

Column(
    modifier = Modifier
        .padding(horizontal = space.padding_2x, vertical = space.padding_1x)
)
```

## Acceptance Criteria
1. All matching `.dp` literals in padding/spacing context are replaced with grid constants.
2. Non-matching values remain as raw `.dp`.
3. Each changed composable function has `val space = MM.space` and `val radius = MM.radius` declared.
4. `./gradlew :composeApp:assembleDebug` passes.
5. Visual output is unchanged.
