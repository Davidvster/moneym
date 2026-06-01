# Phase 1 — Compose UI fixes (issues #1 + #4)

Pure Compose, no platform code. Touch only the files listed.

## #1 — Sync screen initial loading spinner

Problem: `SyncSettingsContent` renders the device `LazyColumn` unconditionally. `SyncSettingsUiState.isLoading` exists but the composable never reads it and it defaults to `false`, so the first frame flashes empty cards before `refresh()` (run in VM `init`) completes. Requirement: until loaded, show ONLY the top bar (`ScreenHeader`) + a centered spinner.

Files:
- `feature/sync/src/commonMain/kotlin/com/dv/moneym/feature/sync/SyncSettingsUiState.kt`
- `feature/sync/src/commonMain/kotlin/com/dv/moneym/feature/sync/SyncSettingsScreen.kt`

Do:
1. In `SyncSettingsUiState`, change the default to `isLoading: Boolean = true` (the VM always runs `refresh()` in `init`, so the very first emission must be the loading state). Do not touch any other field.
2. In `SyncSettingsContent`, keep `ScreenHeader(...)` always rendered. Replace the body so that when `state.isLoading` is true, the `weight(1f)` area shows a centered `CircularProgressIndicator` instead of the `LazyColumn`. Render the existing `LazyColumn` only when `!state.isLoading`.
   - Use a `Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center)` containing the spinner.
   - Style the spinner to match the app: `color = MM.colors.accent`, a modest size (e.g. `Modifier.size(MM.dimen.icon_1x)` or similar), `strokeWidth = MM.dimen.padding_0_25x` — mirror the indicator already used in `BackupRestoreScreen.RemoteRestoreDialog`.
   - Imports needed: `androidx.compose.foundation.layout.Box`, `androidx.compose.foundation.layout.size`, `androidx.compose.material3.CircularProgressIndicator` (verify the exact `MM.dimen` token names exist before using; fall back to a literal `2.dp` stroke / `24.dp` size only if no token fits).
3. The `@Preview` (`SyncSettingsPreview`) builds state without `isLoading`, so with the new default it would now show the spinner. Set `isLoading = false` in that preview's `SyncSettingsUiState(...)` so the preview still shows content.

## #4 — Segmented control clips labels

Problem: `MmSegmented` non-`fillWidth` branch hardcodes `optionWidth = 52.dp` (Md) / `44.dp` (Sm). Labels like "Snapshot"/"Tools" in the Analyze screen exceed 52.dp and get clipped. The animated selection pill relies on equal-width slots (`pillOffset = optionWidth * animatedIndex`).

File: `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmSegmented.kt`

Do (only the `else` / non-`fillWidth` branch — leave the `fillWidth=true` branch untouched):
- Size every option slot to a uniform width = the widest option's text width + horizontal padding, so no label is clipped and all locales fit, while keeping equal-width slots for the pill math.
- Recommended implementation: capture each option's measured content width via `Modifier.onGloballyPositioned` into a `remember { mutableStateMapOf<Int, Int>() }` (or an IntArray state), compute `optionWidth` as the max (converted px→dp with `LocalDensity`), and lay out with that uniform width. Wrap each option `Text` in a `Box` with horizontal content padding (e.g. `MM.dimen.padding_2x`). Keep `trackWidth = optionWidth * options.size + innerPadding * 2` and `pillOffset = optionWidth * animatedIndex`.
  - Before the first measure, fall back to the current fixed width (52.dp/44.dp) as the initial `optionWidth` so layout is stable on frame 0.
- Alternative if cleaner: use `SubcomposeLayout`/`Layout` to measure the widest option once and use it as the uniform slot width.
- Do NOT change `MmSegmented`'s public signature or the call site in `AnalyzeScreen.kt`. The fix is entirely inside `MmSegmented`.
- Update the existing `@Preview` only if required to compile.

## Verify
```
./gradlew :feature:sync:compileDebugKotlinAndroid :core:ui:compileDebugKotlinAndroid
```
Both must compile. Report any new public API or signature changes (there should be none).
