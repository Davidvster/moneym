# Phase 2 — Currency symbol setting (task 3)

## Goal
Add a settings toggle "Use currency symbol". When ON, money is shown with the currency
**symbol** (€, $) instead of the **code** (EUR, USD), app-wide.

## Background (verified facts)
- `CurrencyInfo(code, name, symbol, region)` and the `CommonCurrencies` list already exist
  in `core/model/.../Currencies.kt` — symbols are populated (EUR="€", USD="$", ...).
- The single money renderer is `MmMoney(currency: String, ...)` in
  `core/ui/.../MmMoney.kt`. It just prepends whatever `currency` string the caller passes.
  Nearly every money display routes through it, with the `currency`/`currencyCode` string
  coming from VM state (e.g. `state.currency`, `currencyCode`).
- Two displays use `Money.format()` (which renders the **code**) instead of MmMoney:
  - `feature/transactions/.../list/page/TransactionPageViewModel.kt` (lines ~184, 210) —
    builds `amountFormatted` for recurring-rule rows; also exposes a separate `currency`.
  - `feature/settings/.../recurring/RecurringListScreen.kt` (line ~130) — composable.
- Settings toggle pattern to copy: `paymentModeEnabled`
  (PrefKey → AppSettingsRepository → DefaultAppSettingsRepository → FakeAppSettingsRepository
  → SettingsOverviewViewModel StateFlow → SettingsUiState/Intent → PreferencesSection MmRow+MmToggle).
- App root: `composeApp/.../App.kt` wraps everything in `MoneyMTheme(darkTheme=isDark){…}`
  and already injects `appSettingsRepo` (it reads `observeThemeMode()`).

## Design — minimize call-site churn
Resolve symbol in ONE place (`MmMoney`) via a CompositionLocal, instead of editing ~30
screens. VMs keep passing the currency **code**; `MmMoney` converts to symbol when the
setting is on.

## Steps

### 1. Setting plumbing
- `core/datastore/.../AppSettings.kt`: add `const val USE_CURRENCY_SYMBOL = "pref.use_currency_symbol"`.
- `core/datastore/.../AppSettingsRepository.kt`: add
  `fun observeUseCurrencySymbol(): Flow<Boolean>` + `suspend fun setUseCurrencySymbol(enabled: Boolean)`.
- `core/datastore/.../DefaultAppSettingsRepository.kt`: implement via
  `appSettings.observeBoolean(PrefKeys.USE_CURRENCY_SYMBOL, false)` / `putBoolean`.
- `core/testing/.../FakeAppSettingsRepository.kt`: add matching overrides (REQUIRED — parity
  or unit-test compiles break). Mirror how it stores other booleans.

### 2. Helper in core:model
- New `core/model/.../CurrencyDisplay.kt`:
  ```kotlin
  fun currencyDisplay(code: String, useSymbol: Boolean): String =
      if (useSymbol) CommonCurrencies.firstOrNull { it.code == code }?.symbol ?: code else code
  ```

### 3. CompositionLocal + MmMoney
- In `core/ui` add `val LocalUseCurrencySymbol = staticCompositionLocalOf { false }`
  (e.g. in `MmMoney.kt` or a small `CurrencyDisplay.kt` in core:ui).
- In `MmMoney`: read `LocalUseCurrencySymbol.current`; before rendering, map the incoming
  `currency` to display via `currencyDisplay(currency, useSymbol)`. Guard: only convert
  when `currency` matches a known code (the helper already falls back to the input, so a
  symbol passed in stays unchanged since it won't match a code).
- `composeApp/.../App.kt`: collect `appSettingsRepo.observeUseCurrencySymbol()` (as state,
  default false) and wrap the content in
  `CompositionLocalProvider(LocalUseCurrencySymbol provides useSymbol) { … }` inside the
  existing `MoneyMTheme { … }`.

### 4. The two Money.format() display sites
- `Money.format()` → add optional param: `fun Money.format(useSymbol: Boolean = false)` in
  `core/model/.../MoneyFormat.kt`, using `currencyDisplay(currency.value, useSymbol)` in
  place of the raw `currency.value`. Default false keeps existing callers/tests stable.
- `RecurringListScreen.kt` (composable): pass
  `rule.amount.format(LocalUseCurrencySymbol.current)`.
- `TransactionPageViewModel.kt`: this is a VM, not a composable. First check whether
  `amountFormatted` is actually rendered as-is or whether the row uses `MmMoney(currency=…)`
  (it also exposes `currency`). If the row uses MmMoney, leave the VM alone (MmMoney handles
  it). If `amountFormatted` is shown directly, thread `appSettingsRepository.observeUseCurrencySymbol()`
  into the VM and format with it. Pick the smaller correct change; note which you did.

### 5. Settings UI toggle
- `SettingsUiState.kt`: add `useCurrencySymbol: Boolean = false` + intent
  `data class SetUseCurrencySymbol(val enabled: Boolean) : SettingsOverviewIntent`.
- `SettingsOverviewViewModel.kt`: expose StateFlow from `observeUseCurrencySymbol()`
  (mirror `paymentModeEnabled`); handle the intent → `setUseCurrencySymbol`.
- `SettingsScreen.kt`: collect + pass to content.
- `components/PreferencesSection.kt`: add an `MmRow` + `MmToggle` row mirroring the
  payment-mode row, with a new label + subtitle.
- New strings `settings_use_currency_symbol` + `settings_use_currency_symbol_subtitle` in
  `feature/settings` strings.xml ×4 langs (values, -de, -es, -it).

## Conventions
- Public VMs/usecases for Koin. One onIntent entry. Strings ×4 langs. Import classes.
- Don't break existing tests: `Money.format()` default param stays false.

## Build / verify
- `./gradlew :core:datastore:compileDebugKotlinAndroid :core:testing:compileDebugKotlinAndroid :core:model:compileDebugKotlinAndroid :core:ui:compileDebugKotlinAndroid`
- `./gradlew :feature:settings:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- Run affected module unit tests if quick.
- Report files changed + the TransactionPageViewModel decision + build result.
