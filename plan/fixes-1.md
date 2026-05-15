# fixes-1 — Bug Fixes & UX Improvements

7 phases. Ordered by severity. Each phase is independently releasable.

---

## Phase 1 — PIN Flow ✅
**1a** `App.kt`: Remove `lockController.init()` from onboarding `onDone`; add `LaunchedEffect(onboardingDone)` to init after onboarding completes. Prevents lock screen mid-onboarding.

**1b** `PinSetupViewModel`: Add `reset()` — clears state + drains effect channel. Called in `PinSetupScreen` via `LaunchedEffect(Unit)`. Prevents stale `isSaving=true` and buffered Done effect firing on Change PIN re-open.

---

## Phase 2 — ViewModel Session Scoping ✅
`AppScreen.TransactionEdit` + `AppScreen.CategoryEdit` gain `sessionKey: String = Random.nextLong().toString()`. Passed to `koinViewModel(key=)`. Each navigation creates a fresh VM instance. Fixes: stale form data on re-open, disabled save button after prior save.

---

## Phase 3 — Currency Chooser Flash ✅
`App.kt`: `collectAsState(initial = appSettings.getBoolean(PrefKeys.ONBOARDING_COMPLETED))` — sync initial read eliminates one-frame onboarding flash on app start.

---

## Phase 4 — Transaction List Monthly Sum
`TransactionListUiState`: add `monthlySummary: String`.
`TransactionListViewModel`: compute per active filter — `None`→net, `EXPENSE`→sum negative, `INCOME`→sum positive.
`TransactionListScreen`: render compact summary row between filter chips and list.

---

## Phase 5 — Overview Improvements
**5a** Year view: `OverviewPeriod` sealed (`Month`/`Year`). Toggle button in top bar. `_period` replaces `_currentMonth` in VM. Nav chevrons step month or year.

**5b** Period chart: replace 6-month trend with `DailyBarChart` (per-day in month view) / monthly bar chart (in year view). `chartBars: List<BarEntry>` in UiState.

**5c** Category filter: `FilterChip` row in Overview. `selectedCategoryId` in state/intent. Filters breakdown + chart.

**5d** Donut interaction: `pointerInput` on Canvas detects tap angle → finds slice → `onSliceTapped(idx)`. Center label shows tapped category + amount.

---

## Phase 6 — Settings Default Currency
`SettingsUiState`: add `defaultCurrency`, `showCurrencyPicker`.
`SettingsIntent`: `CurrencyChangeRequested`, `CurrencySelected(code)`.
`SettingsScreen`: "Default currency" row → `CurrencyPickerDialog` (reuses `commonCurrencies` from OnboardingUiState).
`SettingsViewModel`: reads/writes `PrefKeys.DEFAULT_CURRENCY`.

---

## Phase 7 — Screen Transition Animations
`App.kt`: wrap `when(val s = screen)` in `AnimatedContent`. Modal screens (`TransactionEdit`, `CategoryEdit`, `PinSetup`, `Categories`) slide up from bottom. Bottom-nav content switches crossfade 220ms.
