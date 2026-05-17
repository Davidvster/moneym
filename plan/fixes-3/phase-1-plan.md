# Phase 1 — Settings: Category Name Toggle Bug + Density Radio Buttons

## Items covered
- Item 1: Settings transaction view — category name/description toggle bug
- Item 2: Settings transaction view — density option from toggle to radio buttons

## Problem analysis

### Item 1: Category name toggle bug
The `MmToggle` composable in `TxListDisplayScreen.kt` (line 281) receives `onCheckedChange` which calls `onPrefsChanged(currentPrefs.copy(...))`. However, the row `onClick` (line 274) also toggles via `onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName))`. Both handlers fire when the row is tapped (because MmToggle internally uses `detectTapGestures` which conflicts with the parent row's onClick — the toggle consumes the event and re-fires from the row, causing double-toggle or stale state closure). The fix: the `MmToggle`'s `onCheckedChange` should NOT call `onPrefsChanged` again — the row-level onClick is sufficient. Pass `onCheckedChange = {}` to MmToggle (no-op), let only the row's onClick control state. Same issue exists for the Note row.

### Item 2: Density toggle → 3 radio buttons
Currently uses `MmSegmented` with only 2 options (Compact / Comfortable). Need to:
1. Add `Normal` to the `Density` enum in `core/model/src/commonMain/kotlin/com/dv/moneym/core/model/TxDisplayPrefs.kt`
2. Update `DefaultAppSettingsRepository.kt` to handle the new enum value
3. Update `TxListDisplayScreen.kt` to use radio-button style rows (same style as IndicatorStyle section) instead of MmSegmented
4. Add string resource `settings_txdisplay_normal` to all locale string files
5. Update `TxRow.kt` to handle the Normal density (vertical padding between Compact and Comfortable, e.g. 12.dp)

## Files to modify

1. `/Users/davidvalic/Developer/MoneyM2/core/model/src/commonMain/kotlin/com/dv/moneym/core/model/TxDisplayPrefs.kt`
   - Add `Normal` to `Density` enum between Compact and Comfortable

2. `/Users/davidvalic/Developer/MoneyM2/core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/TxRow.kt`
   - Add `Density.Normal -> 12.dp` case to `verticalPadding` when block

3. `/Users/davidvalic/Developer/MoneyM2/core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/DefaultAppSettingsRepository.kt`
   - Update density parsing to handle "Normal" gracefully (getOrDefault already handles unknown values)

4. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/TxListDisplayScreen.kt`
   - Item 1 fix: In Category name MmRow (around line 274), change the nested MmToggle to pass `onCheckedChange = {}` (the row onClick is the single source of truth). Same for Note row.
   - Item 2 fix: Replace the density MmSegmented row with radio-button rows (same pattern as IndicatorStyle rows). Show 3 options: Compact / Normal / Comfortable. Each is a clickable MmRow with a custom radio circle on the right, a Text on the left.

5. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values/strings.xml`
   - Add `<string name="settings_txdisplay_normal">Normal</string>`

6. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values-de/strings.xml`
   - Add `<string name="settings_txdisplay_normal">Normal</string>`

7. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values-es/strings.xml`
   - Add `<string name="settings_txdisplay_normal">Normal</string>`

8. `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values-it/strings.xml`
   - Add `<string name="settings_txdisplay_normal">Normale</string>`

## Detailed changes

### TxDisplayPrefs.kt
```kotlin
enum class Density { Compact, Normal, Comfortable }
```

### TxRow.kt
```kotlin
val verticalPadding = when (prefs.density) {
    Density.Comfortable -> 14.dp
    Density.Normal -> 12.dp
    Density.Compact -> 10.dp
}
```

### TxListDisplayScreen.kt — Toggle fix
The `MmRow` for Category name should only use the row onClick for state changes. The `MmToggle` should have a no-op `onCheckedChange` (since row click handles it). This prevents double-firing:

```kotlin
MmRow(onClick = { onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName)) }) {
    Text(...)
    MmToggle(
        checked = currentPrefs.showCategoryName,
        onCheckedChange = { onPrefsChanged(currentPrefs.copy(showCategoryName = it)) },
    )
}
```
Wait — actually the real issue is that MmToggle uses `detectTapGestures` internally which consumes the gesture. The parent `MmRow` uses `onClick`. When user taps the toggle area, BOTH fire. Fix: pass a no-op to MmToggle.onCheckedChange and rely solely on the MmRow onClick. 

Actually looking at MmToggle — it uses `pointerInput { detectTapGestures(onTap = ...) }`. And MmRow likely uses `clickable`. Both will fire independently. The simplest fix: make MmToggle.onCheckedChange a no-op, so only MmRow.onClick does the state change:

```kotlin
MmRow(onClick = { onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName)) }) {
    Text(...)
    MmToggle(
        checked = currentPrefs.showCategoryName,
        onCheckedChange = { /* handled by row click */ },
    )
}
```

### TxListDisplayScreen.kt — Density section replacement
Replace the entire DENSITY section with radio-button rows (using same pattern as IndicatorStyle section):

```kotlin
// DENSITY section
SectionLabel(...)
MmCard(...) {
    val densityOptions = listOf(Density.Compact, Density.Normal, Density.Comfortable)
    val densityLabels = mapOf(
        Density.Compact to stringResource(Res.string.settings_txdisplay_compact),
        Density.Normal to stringResource(Res.string.settings_txdisplay_normal),
        Density.Comfortable to stringResource(Res.string.settings_txdisplay_comfortable),
    )
    densityOptions.forEachIndexed { i, opt ->
        val isLast = i == densityOptions.size - 1
        val isSelected = currentPrefs.density == opt
        MmRow(
            onClick = { onPrefsChanged(currentPrefs.copy(density = opt)) },
            divider = !isLast,
        ) {
            Text(
                text = densityLabels[opt] ?: opt.name,
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            // Radio circle (same as IndicatorStyle)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isSelected) colors.accent else colors.borderStrong, CircleShape)
                    .background(if (isSelected) colors.accent else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(MmIcons.check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
```

## Acceptance criteria
- Tapping "Category name" toggle row toggles immediately with no double-fire or 10-second delay
- Tapping "Note / description" toggle row also toggles immediately
- Density section shows 3 radio buttons: Compact, Normal, Comfortable
- Selecting Normal applies a medium row height (12dp vertical padding) visible in the preview
- Android build compiles without errors
