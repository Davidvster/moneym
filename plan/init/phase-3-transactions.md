# Phase 3 — Transactions feature

**Status**: ✅ Done

Goal: the core experience — daily/monthly transaction list with filters, plus add/edit/delete.

**Exit criteria**: user can add, edit, delete, and filter transactions on Android and iOS; light/dark mode correct; all four locales (en/es/it/de) have strings; ViewModel tests pass.

## Step status

- [x] **3.0** Dependencies: material-icons-core added to core:designsystem; MoneyMIcons populated
- [ ] **3.1** `core:model` — `YearMonth` type + `Money.format()` extension
- [ ] **3.2** `composeApp` — simple `AppScreen` state navigation (no nav lib)
- [ ] **3.3** `feature:transactions` — presentation layer (UiState / Intent / ViewModel)
- [ ] **3.4** `feature:transactions` — UI (`TransactionListScreen` + components)
- [ ] **3.5** `feature:transactionEdit` — domain (UpsertTransaction / DeleteTransaction / GetTransaction use cases)
- [ ] **3.6** `feature:transactionEdit` — presentation layer (UiState / Intent / ViewModel)
- [ ] **3.7** `feature:transactionEdit` — UI (`TransactionEditScreen` + components)
- [ ] **3.8** String resources (en / es / it / de) for both feature modules
- [ ] **3.9** Koin modules for both feature modules wired into `appModules`
- [ ] **3.10** ViewModel tests in `feature:transactions/commonTest`
- [ ] **3.11** Final verification: add → edit → delete → filter round-trip on Android + iOS

## Notes

- Jetpack Navigation Compose library version unconfirmed; using simple `AppScreen` sealed class + Compose state for Phase 3. Wire Jetpack Nav when version is confirmed.
- Locale-aware date formatting deferred to Phase 8; English-only date display in Phase 3.
- Date picker UI deferred to Phase 8; text field + prev/next buttons in Phase 3.
- Material Icons Extended deferred; using core icon set only.
