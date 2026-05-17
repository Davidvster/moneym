# Phase 5 — TxListDisplayScreen: category name toggle not clickable + color indicator corner radius

## Goal
Two bugs in `TxListDisplayScreen.kt`:
1. The "category name" `MmRow` has no `onClick` — the entire row should be clickable and toggle the switch (same as how most toggle rows work in the app).
2. Color indicator card corner radius is too large — the `Dot` style preview circles use `CircleShape` which may be fine, but the `MmCard` wrapping the color indicator rows uses the default `radius.lg` — this may look too round; reduce to `radius.md`.

## File to Change
`feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/TxListDisplayScreen.kt`

## Current Code Analysis

### Bug 1: Category name row (lines ~272–283)
```kotlin
MmRow {  // <-- no onClick parameter
    Text(
        stringResource(Res.string.settings_txdisplay_category_name),
        ...
        modifier = Modifier.weight(1f),
    )
    MmToggle(
        checked = currentPrefs.showCategoryName,
        onCheckedChange = { onPrefsChanged(currentPrefs.copy(showCategoryName = it)) },
    )
}
```
The `MmRow` has no `onClick` so tapping the row does nothing; only the toggle itself is interactive.

### Bug 2: Corner radius (line ~171)
```kotlin
MmCard(Modifier.padding(horizontal = 16.dp)) {  // default radius.lg
```
The color indicator card (COLOR INDICATOR section) uses default `radius.lg` (16dp rounded). The note section card uses the same default. These look too round compared to the design system's `radius.md` (12dp).

## Implementation Steps

### Fix 1: Make category name row clickable
Change:
```kotlin
MmRow {
    Text(
        stringResource(Res.string.settings_txdisplay_category_name),
        style = type.body,
        color = colors.text,
        modifier = Modifier.weight(1f),
    )
    MmToggle(
        checked = currentPrefs.showCategoryName,
        onCheckedChange = { onPrefsChanged(currentPrefs.copy(showCategoryName = it)) },
    )
}
```
To:
```kotlin
MmRow(onClick = { onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName)) }) {
    Text(
        stringResource(Res.string.settings_txdisplay_category_name),
        style = type.body,
        color = colors.text,
        modifier = Modifier.weight(1f),
    )
    MmToggle(
        checked = currentPrefs.showCategoryName,
        onCheckedChange = { onPrefsChanged(currentPrefs.copy(showCategoryName = it)) },
    )
}
```

### Fix 2: Reduce corner radius on color indicator card
Change the COLOR INDICATOR section `MmCard`:
```kotlin
MmCard(Modifier.padding(horizontal = 16.dp)) {
```
To:
```kotlin
MmCard(Modifier.padding(horizontal = 16.dp), shape = MM.radius.md) {
```

And similarly for the SHOW section card and DENSITY section card — apply `shape = MM.radius.md` to all three `MmCard` calls that currently use the default to keep a consistent look. There are 3 `MmCard` calls in `TxListDisplayContent`:
1. The preview `MmCard { }` (the preview panel — can keep as default or set to md)
2. `// COLOR INDICATOR section` card
3. `// SHOW section` card
4. `// DENSITY section` card

Apply `shape = MM.radius.md` to cards 2, 3, and 4.

## Acceptance Criteria
1. Tapping the "Category name" row (anywhere in the row, not just the toggle) toggles the switch
2. The toggle still works independently
3. The color indicator section card, SHOW section card, and DENSITY section card have `radius.md` corners (12dp) instead of the default `radius.lg` (16dp)
4. No other functionality is changed
5. Build compiles: `./gradlew :composeApp:assembleDebug`
