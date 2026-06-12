# Phase 1 — core/ui foundations: MmCheckbox, MmWalletPickerSheet, MmCategoryPickerSheet

## Goal
Add three reusable components to `core/ui` and migrate existing callers, so later banksync phases can consume them.

## Tasks

### 1. MmCheckbox (new: `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmCheckbox.kt`)
- `@Composable fun MmCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true)`
- Wraps Material3 `Checkbox` with `CheckboxDefaults.colors(checkedColor = MM.colors.accent, uncheckedColor = MM.colors.borderStrong, checkmarkColor = white-ish per theme)` — copy the color treatment used today inline in `feature/settings/.../PendingDeletionsScreen.kt` and `ImportDataScreen.kt`, but themed for both light/dark via MM.colors.
- Migrate the inline `Checkbox` usages in `feature/settings` (PendingDeletionsScreen, ImportDataScreen) to `MmCheckbox`. Do NOT touch feature/banksync yet (phase 4).

### 2. MmWalletPickerSheet (new: `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmWalletPickerSheet.kt`)
- `@Composable fun MmWalletPickerSheet(accounts: List<Account>, selectedAccountId: AccountId?, onSelect: (AccountId) -> Unit, onDismiss: () -> Unit, title: String? = null)`
- ModalBottomSheet, `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, `dragHandle = null` + custom grab-handle bar — copy the sheet chrome from `core/ui/.../MmDeleteSheet.kt` (containerColor MM.colors.bg etc.).
- One wallet per row: reuse `MmRow` like current `WalletSwitcherDialog` body (name + checkmark on selected, accent color). Dismiss sheet on select (call onSelect then onDismiss).
- Rewrite `core/ui/.../WalletSelector.kt` to open `MmWalletPickerSheet` instead of `WalletSwitcherDialog` (keep its public API unchanged so budgets/transactionEdit/overview callers compile untouched).
- Replace the direct `WalletSwitcherDialog(...)` call in `feature/transactions/.../list/TransactionListScreen.kt:220` with `MmWalletPickerSheet` (state flags in UiState stay as-is; same callbacks).
- Delete `core/ui/.../WalletSwitcherDialog.kt` once callerless (check for remaining references first).
- Title string: add new key `wallet_picker_title` ("Select wallet") to core/ui composeResources `values/strings.xml` AND all 27 locale dirs (ar cs da de es et fi fr hi hr hu is it ja lt lv mk nb nl pl pt ru sk sl sv tr vi zh). Use it as default title inside the sheet when title param null. If core/ui already has a suitable key (check values/strings.xml first), reuse instead.

### 3. MmCategoryPickerSheet (new: `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmCategoryPickerSheet.kt`)
- Move/generalize `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/components/CategoryFilterBottomSheet.kt` (`internal fun CategoryFilterSheet`) into core/ui as public `MmCategoryPickerSheet`.
- Two modes in one component (or two overloads — pick cleaner):
  - Multi-select: `categories, selectedIds: Set<CategoryId>, onToggle: (CategoryId) -> Unit, onClearAll: (() -> Unit)?, onDismiss` — current behavior.
  - Single-select: `categories, selectedId: CategoryId?, onPick: (CategoryId) -> Unit, onDismiss` — dismiss on pick.
- Keep FlowRow + MmChip + category icon layout exactly as today. Any string keys used by the old sheet (title, clear-all) must move/duplicate into core/ui resources across ALL locales (copy existing translations from feature/transactions locale files — do not machine-retranslate).
- Update `TransactionListScreen.kt:232` to use the new core/ui component; delete the old file.

## Conventions (CLAUDE.md)
- Import classes, no FQNs. No comments unless non-obvious why. Compose resource keys need per-key imports (`import moneym.core.ui.generated.resources.*` pattern — check existing core/ui usage).
- Every new string key into base + ALL 27 locales in the same change.
- core/ui module's Res class: check how existing core/ui composables access string resources.

## Verify
```
./gradlew :core:ui:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid :feature:overview:compileDebugKotlinAndroid :feature:budgets:compileDebugKotlinAndroid :feature:transactionEdit:compileDebugKotlinAndroid :feature:settings:compileDebugKotlinAndroid
```
All must pass. Grep for `WalletSwitcherDialog` and `CategoryFilterSheet` afterwards — zero references outside deleted files.
