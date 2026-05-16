# Phase 3: Top Navigation Titles + Bottom Bar Items (Locale)

## Problem
The top navigation screen titles ("Transactions", "Overview", "Settings") and bottom tab bar labels ("Transactions", "Overview", "Settings") are hardcoded English strings. They must respect the selected language via the Compose Multiplatform resource system.

Key locations:
- `TransactionListScreen.kt` line 128: `text = "Transactions"` — hardcoded
- `TransactionListScreen.kt` line 297: `text = "New transaction"` — hardcoded
- `MmTabBar.kt` lines 48-50: labels `"Transactions"`, `"Overview"`, `"Settings"` — hardcoded
- `OverviewScreen.kt`: uses `stringResource(Res.string.overview_title)` — already correct
- `SettingsScreen.kt`: uses `stringResource(Res.string.settings_title)` — already correct

The tab bar is in `core/ui` which has no string resources yet. The tab labels and screen titles need to be sourced from string resources.

## Files to modify
- `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmTabBar.kt` — replace hardcoded tab labels with string resources
- `core/ui/src/commonMain/composeResources/values/strings.xml` — create or update with tab label strings (create if not exists)
- `core/ui/src/commonMain/composeResources/values-de/strings.xml` — German
- `core/ui/src/commonMain/composeResources/values-es/strings.xml` — Spanish
- `core/ui/src/commonMain/composeResources/values-it/strings.xml` — Italian
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt` — replace hardcoded `"Transactions"` title and `"New transaction"` button with string resources
- `feature/transactions/src/commonMain/composeResources/values/strings.xml` — add missing string keys
- `feature/transactions/src/commonMain/composeResources/values-de/strings.xml` — German
- `feature/transactions/src/commonMain/composeResources/values-es/strings.xml` — Spanish
- `feature/transactions/src/commonMain/composeResources/values-it/strings.xml` — Italian

## Implementation steps

1. **core/ui resources**: Check if `core/ui/src/commonMain/composeResources/values/` exists. Create `strings.xml` with:
   ```xml
   <string name="tab_transactions">Transactions</string>
   <string name="tab_overview">Overview</string>
   <string name="tab_settings">Settings</string>
   ```
   Add DE/ES/IT translations.

2. **MmTabBar.kt**: Import the generated Res from `core/ui`. Replace the hardcoded `Triple(...)` list with:
   ```kotlin
   val tabs = listOf(
       Triple(TabRoute.Transactions, MmIcons.list, stringResource(Res.string.tab_transactions)),
       Triple(TabRoute.Overview, MmIcons.chart, stringResource(Res.string.tab_overview)),
       Triple(TabRoute.Settings, MmIcons.settings, stringResource(Res.string.tab_settings)),
   )
   ```

3. **TransactionListScreen.kt**: The title `"Transactions"` already has `transactions_title` in strings.xml. Use `stringResource(Res.string.transactions_title)`.
   The `"New transaction"` button: add `transactions_new_transaction` string and use it.

4. **Add `transactions_new_transaction`** to transactions strings.xml in all locales.

5. Verify `overview_title` is already used (it is — from `overview/generated`).

6. Verify `settings_title` is already used (it is).

## Acceptance criteria
- [ ] Bottom tab bar shows localized labels when app language is changed (e.g., to German)
- [ ] Transactions screen title is localized
- [ ] "New transaction" button text is localized
- [ ] No hardcoded English strings in tab bar or Transactions screen header
