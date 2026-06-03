# Phase 7b-2 — Previews for feature screens: settings + transactionEdit + transactions + overview

Same rules as 7b: preview the stateless inner content composable with a literal `UiState`; do NOT refactor production to extract content; skip (with note) screens with no stateless composable or that already have a `*ContentPreview`. Co-located private `@Composable <Name>Preview()`, `@Preview` from `androidx.compose.ui.tooling.preview.Preview`, `MoneyMTheme { }`, callbacks `{}`/`onIntent = {}`. Read each screen first for the stateless composable + UiState shape + `@OptIn`.

## Screens (16)
Settings overview:
- `feature/settings/.../overview/SettingsScreen.kt` — composes multiple VMs; if no single stateless content fn, preview a representative stateless section composable (e.g. one of `components/*Section.kt`) or skip+note.
- `feature/settings/.../overview/backuprestore/BackupRestoreScreen.kt`
- `feature/settings/.../overview/currencypicker/CurrencyPickerScreen.kt`
- `feature/settings/.../overview/export/ExportScreen.kt`
- `feature/settings/.../overview/importdata/ImportDataScreen.kt`
- `feature/settings/.../overview/locale/LanguagePickerScreen.kt`
Settings other:
- `feature/settings/.../paymentmodes/PaymentModeListScreen.kt`
- `feature/settings/.../recurring/RecurringListScreen.kt`
Settings wallet:
- `feature/settings/.../wallet/AddWalletScreen.kt`
- `feature/settings/.../wallet/EditWalletCurrencyScreen.kt`
- `feature/settings/.../wallet/EditWalletScreen.kt`
- `feature/settings/.../wallet/WalletManageScreen.kt`
transactionEdit:
- `feature/transactionEdit/.../RecurringEditScreen.kt`
- `feature/transactionEdit/.../TransactionEditScreen.kt` (likely already has `TransactionEditContentPreview` — skip if so)
transactions:
- `feature/transactions/.../list/page/TransactionPageScreen.kt`
overview:
- `feature/overview/.../page/OverviewPageScreen.kt`

Build sample domain objects from `core:model`. Import classes, no FQN. No comments unless non-obvious. Only add preview functions.

## Verify
```
./gradlew :feature:settings:compileDebugKotlinAndroid \
  :feature:transactionEdit:compileDebugKotlinAndroid \
  :feature:transactions:compileDebugKotlinAndroid \
  :feature:overview:compileDebugKotlinAndroid
```
All compile. Report which screens got previews and which were skipped (+reason).
