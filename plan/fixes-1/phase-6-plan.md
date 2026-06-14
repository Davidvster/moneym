# Phase 6 — Theme picker → bottom sheet

Repo: /Users/davidvalic/Developer/MoneyM. Replace the cramped inline
Light/Dark/Auto `MmSegmented` in settings with a `MmSettingsRow` that shows the
current theme on the right + a chevron, opening a single-select bottom sheet.

**No new strings** — reuse `settings_theme`, `settings_theme_light/dark/auto`.

Follow the existing `showLockPicker` wiring exactly (it's the template for a
VM-driven sheet flag here). Files:
- `feature/settings/.../overview/SettingsUiState.kt` (UiState + Intent)
- `feature/settings/.../overview/SettingsOverviewViewModel.kt`
- `feature/settings/.../overview/SettingsScreen.kt`
- `feature/settings/.../overview/components/SettingsLazyList.kt`
- `feature/settings/.../overview/components/AppearanceSection.kt`

---

## A. UiState + Intent (`SettingsUiState.kt`)
- Add field to `SettingsUiState`: `val showThemeSheet: Boolean = false`.
- Add intent: `data class ShowThemeSheet(val visible: Boolean) : SettingsOverviewIntent`.

## B. ViewModel (`SettingsOverviewViewModel.kt`)
Mirror `_showLockPicker`:
```kotlin
private val _showThemeSheet = MutableStateFlow(false)
val showThemeSheet: StateFlow<Boolean> = _showThemeSheet.asStateFlow()
```
In `onIntent`, add:
```kotlin
is SettingsOverviewIntent.ShowThemeSheet -> _showThemeSheet.update { intent.visible }
```

## C. SettingsScreen (`SettingsScreen.kt`)
- Collect: `val showThemeSheet by overviewViewModel.showThemeSheet.collectAsStateWithLifecycle()`.
- Add to the `SettingsUiState(...)` builder: `showThemeSheet = showThemeSheet`.
- In the `SettingsContent(...)` call, add:
  `onShowThemeSheet = { overviewViewModel.onIntent(SettingsOverviewIntent.ShowThemeSheet(it)) }`.
- `SettingsContent` signature: add param `onShowThemeSheet: (Boolean) -> Unit`.
- The `themeIndex`/`themeModes` locals (lines ~200-205) are only used by the old
  segmented control — **remove them** once AppearanceSection no longer needs them
  (keep `onThemeModeChanged`, still used by the sheet).
- Render the sheet next to the lock picker block (~line 246):
```kotlin
if (state.showThemeSheet) {
    ThemePickerSheet(
        current = state.themeMode,
        onSelect = { onThemeModeChanged(it); onShowThemeSheet(false) },
        onDismiss = { onShowThemeSheet(false) },
    )
}
```
  Import `ThemePickerSheet` from the components package (defined in D).
- Update the preview/other `SettingsContent` call sites (there's a preview ~line
  310) to pass `onShowThemeSheet = {}`.
- Pass to `SettingsLazyList`: replace the theme args with
  `onOpenThemeSheet = { onShowThemeSheet(true) }` (see D). Remove the now-unused
  `themeIndex`/`themeModes`/`onThemeModeChanged` args from the `SettingsLazyList`
  call.

## D. SettingsLazyList (`SettingsLazyList.kt`)
- Remove params `themeIndex`, `themeModes`, `onThemeModeChanged`; add
  `onOpenThemeSheet: () -> Unit`.
- Update the `AppearanceSection(...)` call (line ~72) to:
```kotlin
AppearanceSection(
    themeMode = state.themeMode,
    onOpenThemeSheet = onOpenThemeSheet,
    onNavigateToTxDisplay = onNavigateToTxDisplay,
)
```

## E. AppearanceSection (`AppearanceSection.kt`)
- New signature:
```kotlin
internal fun AppearanceSection(
    themeMode: ThemeMode,
    onOpenThemeSheet: () -> Unit,
    onNavigateToTxDisplay: () -> Unit,
)
```
- Replace the theme `MmRow { Icon + Text + MmSegmented }` with an `MmSettingsRow`
  whose trailing slot shows the current theme label + a chevron:
```kotlin
val isDark = themeMode == ThemeMode.Dark
MmCard(Modifier.padding(horizontal = space.padding_2x)) {
    MmSettingsRow(
        title = stringResource(Res.string.settings_theme),
        leadingIcon = if (isDark) Icon.Moon.imageVector else Icon.Sun.imageVector,
        onClick = onOpenThemeSheet,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(themeLabel(themeMode), style = MM.type.caption, color = MM.colors.text2)
                Spacer(Modifier.width(MM.dimen.padding_0_5x))
                Icon(
                    imageVector = Icon.ChevronRight.imageVector,
                    contentDescription = null,
                    tint = MM.colors.text3,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                )
            }
        },
    )
    MmSettingsRow(
        title = stringResource(Res.string.settings_tx_list),
        leadingIcon = Icon.Sliders.imageVector,
        onClick = onNavigateToTxDisplay,
        divider = false,
    )
}
```
- Add a `@Composable themeLabel(mode)` helper:
```kotlin
@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.Light -> stringResource(Res.string.settings_theme_light)
    ThemeMode.Dark -> stringResource(Res.string.settings_theme_dark)
    ThemeMode.Auto -> stringResource(Res.string.settings_theme_auto)
}
```
- Add the bottom sheet composable (model it on `MmWalletPickerSheet.kt` —
  drag-handle box + `MmSheetHeader` + single-select `MmRow`s + `Check` icon):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePickerSheet(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = space.padding_2_5x, topEnd = space.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = space.padding_2_5x, vertical = space.padding_3x),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 36.dp, height = space.padding_0_5x)
                        .clip(RoundedCornerShape(50)).background(colors.borderStrong),
                )
            }
            MmSheetHeader(title = stringResource(Res.string.settings_theme), onClose = onDismiss)
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_25x)) {
                listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.Auto).forEach { mode ->
                    MmRow(
                        onClick = { onSelect(mode) },
                        divider = false,
                        padding = PaddingValues(horizontal = space.padding_0_5x, vertical = space.padding_1x),
                    ) {
                        Text(themeLabel(mode), style = MM.type.body, color = colors.text, modifier = Modifier.weight(1f))
                        if (mode == current) {
                            Icon(
                                imageVector = Icon.Check.imageVector,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(space.icon_1x),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(space.padding_1x))
        }
    }
}
```
- Remove now-unused imports (`MmSegmented`, `MmSegmentedSize`) and add the new
  ones (ModalBottomSheet, rememberModalBottomSheetState, ExperimentalMaterial3Api,
  RoundedCornerShape, Box, Spacer, Row, Column, Arrangement, PaddingValues, width,
  height, size, clip, background, Alignment, dp, M3 `Icon`, M3 `Text`, MmRow,
  MmSheetHeader, MmSettingsRow, the `imageVector` ext, and the 4 theme string res
  imports). `Icon.Check`, `Icon.ChevronRight`, `Icon.Moon`, `Icon.Sun`,
  `Icon.Sliders` come from `com.dv.moneym.core.model.Icon`.

Note the name collision pattern: both `androidx.compose.material3.Icon`
(composable) and `com.dv.moneym.core.model.Icon` (enum) under simple name `Icon`
— resolved by position, same as elsewhere in the codebase.

---

## Verify
```bash
./gradlew :feature:settings:compileDebugKotlinAndroid
```
Also run the settings unit tests if they touch the VM:
```bash
./gradlew :feature:settings:testDebugUnitTest --no-configuration-cache
```
Must compile/pass. Report files changed, results, deviations. Do NOT commit.
