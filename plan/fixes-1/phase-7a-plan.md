# Phase 7a — Previews for shared core:ui components

Add a co-located private `@Composable fun <Name>Preview()` to each of the 10 `core:ui` composables currently missing one. Follow the existing convention (see `core/ui/.../MmButton.kt`, `MmCard.kt`): import `androidx.compose.ui.tooling.preview.Preview`, annotate `@Preview`, wrap body in `MoneyMTheme { … }` (`com.dv.moneym.core.designsystem.MoneyMTheme`), use `MM.dimen.*` for spacing. Pass literal/fake args (callbacks = `{}`). Preview must COMPILE.

## Files (all in core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/)
1. `HsvColorPickerDialog.kt`
2. `Icon.kt`
3. `MmDeleteSheet.kt`
4. `MmDialog.kt`
5. `MmErrorDialog.kt`
6. `MmLoadingOverlay.kt`
7. `MmLoadingSpinner.kt`
8. `MmMonthPickerDialog.kt`
9. `WalletSelector.kt`
10. `WalletSwitcherDialog.kt`

## Notes
- For dialog/sheet composables that internally use a Dialog/ModalBottomSheet host, just invoke the composable with `onDismiss = {}` / sample args inside `MoneyMTheme` — a `@Preview` of a dialog renders its content; if a composable hard-requires a visible-state flag, pass `true`.
- For `WalletSelector`/`WalletSwitcherDialog`: build sample `Account`/wallet domain objects from `core:model` (`Account`, `AccountId`, `CurrencyCode`, `Money`). Use preview-only literal values.
- For `Icon.kt`: preview a couple of `MmIcons` entries.
- Read each composable's signature first to pass correct params. No comments unless non-obvious. Import classes, no FQN.

## Verify
```
./gradlew :core:ui:compileDebugKotlinAndroid
```
(Compiling the module compiles the previews.) Must succeed. Production behavior unchanged — only added preview functions.
