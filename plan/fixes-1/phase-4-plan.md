# Phase 4 — Add-button consistency (task 5)

## Goal
Use only the bottom full-width add button for wallet + categories management. Remove the
top-right "+" icon buttons. (Budgets + payment modes are already bottom-only — verify.)

## Background (verified)
- **WalletManageScreen.kt** (`feature/settings/.../wallet/`): has a top-right
  `MmIconButton(Icon.Plus)` in the `ScreenHeader` trailingContent (lines ~96-101) wired to
  `onNavigateToAddWallet`. It has **no** bottom add button. Layout is
  `Box { Column { ScreenHeader; …; LazyColumn(weight=1f) } }`.
- **CategoryListHeader.kt** (`feature/categories/.../list/components/`): has a top-right
  `MmIconButton(Icon.Plus)` (lines ~37-42) wired to `onAddClick`. `CategoryListScreen.kt`
  already has a bottom full-width `MmButton` (Plus, Secondary, fullWidth) that fires
  `CategoryListIntent.ShowCategoryEditSheet(true)`.
- Existing bottom-button reference pattern (copy its style): `CategoryListScreen.kt`
  bottom `MmButton(text=…, variant=MmButtonVariant.Secondary, fullWidth=true, leadingIcon=Icon.Plus.imageVector)`.

## Steps

### Wallet manage
- Remove the `trailingContent = { MmIconButton(Plus) {…} }` from the `ScreenHeader` in
  `WalletManageScreen.kt` (keep title + onBack).
- Add a bottom full-width `MmButton` **after** the `LazyColumn` (still inside the themed
  `Column`), mirroring the categories bottom button (Secondary, fullWidth, leadingIcon =
  Plus), wrapped in a `Box`/padding like categories
  (`horizontal = padding_2_5x, vertical = padding_2x`). On click → `onNavigateToAddWallet`.
- The `onNavigateToAddWallet` param stays (now used by the bottom button). Remove the now
  unused `MmIconButton` / `Icon` imports only if nothing else uses them.
- New string for the button label, e.g. `settings_wallet_add` = "Add wallet" — check if a
  suitable string already exists in `feature/settings` strings.xml first (the add-wallet
  screen may have a title to reuse); if not, add `settings_wallet_add` to all 4 langs
  (values, -de, -es, -it).

### Categories
- Remove the `trailingContent = { MmIconButton(Plus) {…} }` from `ScreenHeader` in
  `CategoryListHeader.kt`.
- The header's `onAddClick` param becomes unused (the bottom button uses
  `ShowCategoryEditSheet`, not `onAddClick`). Remove the `onAddClick` param from
  `CategoryListHeader` and its call site in `CategoryListScreen.kt`, and drop now-unused
  imports (`MmIconButton`, `Icon`, `imageVector`) if nothing else in the file uses them.
  (Grep to confirm before deleting.)

### Verify (no change expected)
- Budgets list (`BudgetListScreen.kt`) — bottom-only already. Confirm no top "+".
- Payment modes (`PaymentModeListScreen.kt`) — bottom-only already. Confirm no top "+".

## Conventions
- Strings ×4 langs. Import classes. Delete genuinely-unused code/imports (CLAUDE.md).

## Build / verify
- `./gradlew :feature:settings:compileDebugKotlinAndroid :feature:categories:compileDebugKotlinAndroid`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- Report files changed + whether you reused an existing wallet-add string or added one + build result.
