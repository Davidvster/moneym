# Phase 2: Currency Symbol Improvements

## Context
Two related issues:
1. Settings subtitle "Show € instead of EUR" is hardcoded — should adapt to the current wallet's currency (e.g., "Show £ instead of GBP")
2. Some places display raw currency code (EUR, USD) without respecting the `useCurrencySymbol` preference — specifically `BudgetRemainingChip.kt` and `OverviewPeriodBody.kt`

## Critical Files
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/components/PreferencesSection.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsOverviewViewModel.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsUiState.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsScreen.kt`
- `feature/settings/src/commonMain/composeResources/values/strings.xml` (+ de, es, it)
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/components/BudgetRemainingChip.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/components/OverviewPeriodBody.kt`
- `core/model/src/commonMain/kotlin/com/dv/moneym/core/model/CurrencyDisplay.kt`

## Changes

### Change 1: Dynamic subtitle in settings
**`SettingsUiState.kt`**: Add `walletCurrency: String = "EUR"` field.

**`SettingsOverviewViewModel.kt`**: 
- Add `AccountRepository` constructor param
- Observe `appSettingsRepository.observeSelectedAccountId()` + `accountRepository.observeAll()` to derive the selected account's currency code
- Expose it in state as `walletCurrency`

**`PreferencesSection.kt`**:
- Add `walletCurrency: String` param
- Compute subtitle dynamically: show symbol of walletCurrency vs the code
  ```kotlin
  // Use existing CurrencyDisplay / CommonCurrencies to get symbol
  val symbol = CommonCurrencies.firstOrNull { it.code == walletCurrency }?.symbol ?: walletCurrency
  subtitle = "Show $symbol instead of $walletCurrency"
  ```
- OR use a parameterized string resource: `settings_use_currency_symbol_subtitle` → `"Show %1$s instead of %2$s"`

**`strings.xml`** (all locales): Change subtitle to parameterized form `"Show %1$s instead of %2$s"` (de/es/it translators maintain their wording with placeholders).

**`SettingsScreen.kt`**: Pass `state.walletCurrency` down to `PreferencesSection`.

### Change 2: BudgetRemainingChip uses raw currency string
`BudgetRemainingChip.kt` line ~29–33:
```kotlin
// Current — raw string, ignores useCurrencySymbol:
private fun formatAmount(v: Double, currency: String): String {
    ...
    return if (currency.isNotEmpty()) "$currency $major.$frac" else "$major.$frac"
}
```
Fix: Make composable read `LocalUseCurrencySymbol.current` and call `currencyDisplay(currency, useSymbol)` before formatting.

### Change 3: OverviewPeriodBody raw currencyCode text
`OverviewPeriodBody.kt` line ~341:
```kotlin
text = "$currencyCode ${formatAmount(cat.amount)}",
```
Fix: Before building the text, apply `currencyDisplay(currencyCode, LocalUseCurrencySymbol.current)`.

**Also scan the full file** for any other `"$currencyCode ..."` raw string interpolations and fix each one.

### FeatureModules wiring for SettingsOverviewViewModel
`FeatureModules.kt` — `featureSettingsModule`: add `AccountRepository` (already bound in `dataAccountsModule`) to the `SettingsOverviewViewModel` factory.

## Verification
1. Set currency to GBP → settings subtitle shows "Show £ instead of GBP"
2. Enable currency symbol → budget chip shows "£" not "GBP"  
3. Enable currency symbol → overview category breakdown shows symbol, not code
