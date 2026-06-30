# Phase 1 Plan: Low-Risk UI, Copy, and i18n

## Scope
- Polish transaction sync status icon states.
- Fix the notification bell vector.
- Group category filter categories by income/expense.
- Replace lock-after dialog with a bottom sheet.
- Fill missing settings translations for transaction-view and payment notification strings.
- Clarify bank-sync PSD2/open-banking copy.

## Implementation
- Add `Icon.CloudSync` in `core/model` and map it in `core/ui` to a cloud-with-sync-arrows vector.
- Use `Icon.CloudSync` for the idle transaction-list sync action. Keep progress indicator while syncing.
- Remove the infinite pulse from the sync attention warning state; show a static warning triangle and count badge.
- Replace `Icon.Bell` vector paths with a standard bell outline.
- Extend `MmCategoryPickerSheet` with an optional grouped mode, grouped by `TransactionType`, and enable it from transaction category filter.
- Rename/replace `LockTimeoutPickerDialog` internals with a `ModalBottomSheet` using `MmSheetHeader`, `MmRow`, `MmRadio`, and `MmButton`.
- Update `feature/settings` string values in base and all 27 locale folders for:
  - `settings_txdisplay_sync_suggestions`
  - `settings_txdisplay_save_as_new`
  - payment notification titles/body/settings text found in the module
- Update bank-sync setup hint and info-page copy to state PSD2/open-banking coverage cautiously for supported European banks/countries via Enable Banking.

## Verification
- Run compile checks:
  - `./gradlew :feature:settings:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid :feature:banksync:compileDebugKotlinAndroid`
- Run focused screenshot/preview tests if tasks are discoverable and quick.
- Audit touched string files for key parity.

## Commit
- `fixes-4 phase 1: polish icons copy and sheets`
