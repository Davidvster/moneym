# MoneyM Redesign — Summary

## What was built

Full redesign of the MoneyM Compose Multiplatform app (iOS + Android) from a Material 3-heavy UI to
a custom, platform-agnostic design system using neutral tokens, Geist/GeistMono fonts, and bespoke
components.

---

## Phases completed

| Phase | Scope                                                                                                                                   |
|-------|-----------------------------------------------------------------------------------------------------------------------------------------|
| 0     | Design system foundation — `MoneyMColors`, `MoneyMType`, `MoneyMSpacing`, `MoneyMRadius`, `MM` object, `MoneyMTheme`, Geist font wiring |
| 1     | Core UI component library — 17 components + 37 icons + 3 charts                                                                         |
| 2     | Data model extension — `TxDisplayPrefs`, `ThemeMode`, `AppSettingsRepository`                                                           |
| 3     | PIN screens + Transaction list — full rewrites                                                                                          |
| 4     | Add/Edit transaction screen — full rewrite                                                                                              |
| 5     | Overview screen (Month + Year) — full rewrite + new chart composables                                                                   |
| 6     | Settings + 3 sub-screens (TxListDisplay, CurrencyPicker, LanguagePicker)                                                                |
| 7     | Categories — ManageCategoriesScreen + NewCategorySheet with color/icon pickers                                                          |
| 8     | Polish — M3 shim removal, animations, empty states, edge cases                                                                          |

---

## Key files changed

### core/designsystem

- `MoneyMColors.kt` — semantic token layer (light + dark)
- `MoneyMTypography.kt` — 9-style type system using Geist + GeistMono
- `MoneyMSpacing.kt` — 4dp base spacing (s1–s12)
- `MoneyMRadius.kt` — xs/sm/md/lg/xl/pill
- `MM.kt` — convenience accessors (`MM.colors`, `MM.type`, `MM.dimen`, `MM.radius`)
- `MoneyMTheme.kt` — pure `CompositionLocalProvider` (no M3 wrapper)
- `Fonts.kt` — `rememberGeist()` / `rememberGeistMono()`
- `composeResources/font/` — 7 Geist TTF files

### core/ui (new)

- `MmButton.kt` — 6 variants, 3 sizes
- `MmIconButton.kt` — 3 variants
- `MmSegmented.kt` — animated pill, Sm/Md sizes
- `MmChip.kt` — pill, inverted selected state
- `MmField.kt` — label above, prefix/suffix
- `MmToggle.kt` — animated thumb
- `MmRow.kt` — 56dp min height, 1dp divider
- `MmCard.kt` — surface bg, 16dp radius, shadow
- `MmTabBar.kt` — custom row-based tab bar (no M3 NavigationBar)
- `MmMoney.kt` — GeistMono tnum, U+2212 minus
- `CategoryIconTile.kt` — 5 IndicatorStyle variants
- `SectionLabel.kt`, `ScreenHeader.kt`
- `DonutChart.kt`, `CumulativeChart.kt`, `MiniBars.kt`
- `MmIcons.kt` — 37 stroke-based ImageVector icons
- `TxRow.kt` — shared transaction row

### core/model

- `ThemeMode.kt`, `TxDisplayPrefs.kt` — new enums/data class

### core/datastore

- `AppSettingsRepository.kt` — interface
- `DefaultAppSettingsRepository.kt` — implementation

### feature/security

- `PinUnlockScreen.kt`, `PinSetupScreen.kt` — full rewrites
- `PinKeypad.kt` — extracted shared composable

### feature/transactions

- `TransactionListScreen.kt` — full rewrite
- `TransactionListViewModel.kt`, `TransactionListUiState.kt` — extended

### feature/transactionEdit

- `TransactionEditScreen.kt` — full rewrite

### feature/overview

- `OverviewScreen.kt` — full rewrite (LazyColumn, SpendingByCategoryCard, CategoryTrendsCard)
- `OverviewViewModel.kt`, `OverviewUiState.kt` — extended with daily/cumulative/trend data

### feature/settings

- `SettingsScreen.kt` — full rewrite
- `TxListDisplayScreen.kt`, `CurrencyPickerScreen.kt`, `LanguagePickerScreen.kt` — new
- `SettingsViewModel.kt`, `SettingsUiState.kt` — extended

### feature/categories

- `CategoryListScreen.kt` — full rewrite (ManageCategoriesScreen + NewCategorySheet)
- `CategoryListViewModel.kt`, `CategoryListUiState.kt` — extended

### composeApp

- `App.kt` — observes `ThemeMode`, passes `isDark` to `MoneyMTheme`
- `MainNav.kt` — removed M3 Scaffold/NavigationBar; added new screen entries

---

## Design decisions made

- **No M3 NavigationBar** — each screen owns its `MmTabBar` inside its Column
- **No FAB** — pinned full-width primary button above tab bar
- **Expenses neutral, not red** — only income uses accent green (#16A34A)
- **GeistMono + tnum for all currency values** — tabular figures for alignment
- **U+2212 (−) for negative amounts**, not ASCII hyphen
- **`ModalBottomSheet`, `AlertDialog`, `DatePickerDialog`** — kept as M3 exceptions (no good KMP
  alternative)
- **Color ring selection effect** — triple-nested Box (outer color → white gap → inner color +
  check)
- **Theme reactive at root** — `App.kt` observes `ThemeMode` flow; instant switch

---

## Follow-up recommendations

1. **iOS font loading** — verify Geist renders on iOS simulator; if not, implement
   `iosMain/Fonts.ios.kt` with `UIFont` registration
2. **Drag-to-reorder persistence** — Phase 7 reorder is in-memory only (Category model lacks `order`
   field); add `order: Int` to Category + migration to persist reorder
3. **Category type (Expense/Income)** — Category model doesn't have a `type` field; add it to fully
   support the Expense/Income tab split in ManageCategoriesScreen
4. **Search in TransactionListScreen** — search icon present but not wired (placeholder)
5. **Hardcoded strings** — several UI strings in rewritten screens are not yet in `strings.xml`; a
   full i18n pass would complete the work from the prior string resources phase
6. **Old overview chart files** — `feature/overview/ui/charts/BarChart.kt` and `DonutChart.kt` are
   dead code; safe to delete
7. **CategoryEditScreen stub** — reduced to a no-op in Phase 7; either wire it to the sheet or
   remove the route from MainNav
8. **Unit tests** — OverviewViewModel existing tests pass; new ViewModel logic (TxListViewModel
   prefs, SettingsViewModel theme) could use Turbine-based flow tests
