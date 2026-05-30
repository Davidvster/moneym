# Phase 1 — Shared wallet selector (tasks 1 + 2)

## Goal
One reusable wallet-selection UI (chip button that opens a switcher dialog), used in:
transaction-edit top-right, overview top-right, and the budget create/edit account picker.
Selecting a wallet sets the **global** selected account; currency/budgets recompute from it.

## Background (verified facts)
- `WalletChip` + `WalletColorDot` already live in
  `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/WalletChip.kt`.
- `WalletSwitcherDialog` currently lives in
  `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/components/WalletSwitcherDialog.kt`
  and uses two strings from the transactions module (`transactions_wallet_select`,
  `transactions_cancel`). It's used by `TransactionListScreen.kt`.
- Wallet selection is **global**: `AppSettingsRepository.setSelectedAccountId(id: Long)` /
  `observeSelectedAccountId(): Flow<Long>`. All relevant VMs already observe it.
- Account model: `com.dv.moneym.core.model.Account` (has `id: AccountId`, `name`,
  `colorHex: String?`, `currency`, `isDefault`). `AccountId` has `.value: Long`.

## Steps

### 1. Move switcher + selector into core:ui
- Create `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/WalletSwitcherDialog.kt`
  with the same composable (make it `public` `fun WalletSwitcherDialog(...)`, package
  `com.dv.moneym.core.ui`). Use core:ui's own string resources (see step 2).
- Add `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/WalletSelector.kt`:
  ```kotlin
  @Composable
  fun WalletSelector(
      accounts: List<Account>,
      selectedAccountId: AccountId?,
      onSelect: (AccountId) -> Unit,
      modifier: Modifier = Modifier,
  )
  ```
  It renders a `WalletChip` (name+color of the currently selected account, fallback to
  default/first) and owns the dialog visibility via `rememberSaveable` (pure UI transient —
  allowed). Tapping the chip opens `WalletSwitcherDialog`; selecting calls `onSelect(id)`
  and closes. If `accounts` is empty, render nothing.
- Delete the old `feature/transactions/.../components/WalletSwitcherDialog.kt` and update
  `TransactionListScreen.kt` to import/use either the moved dialog or `WalletSelector`
  from `core:ui` (keep its existing behaviour: chip in the list top bar → switcher).
- Confirm `core:ui` already depends on `core:model` and `core:designsystem` (it does —
  WalletChip imports them).

### 2. Strings → core:ui
- Add `wallet_select` (title) and `cancel` to
  `core/ui/src/commonMain/composeResources/values/strings.xml` and the `-de`, `-es`,
  `-it` variants. Reuse existing translations of `transactions_wallet_select` /
  `transactions_cancel` from the transactions module strings.xml for the localized values.
- Remove the now-unused transactions strings only if nothing else references them
  (grep first; if still used elsewhere, leave them).

### 3. Transaction edit — add selector top-right
- `feature/transactionEdit/.../components/TransactionEditHeader.kt`
  (`TransactionEditModalHeader`): add a trailing `WalletSelector` on the right side of the
  header (next to / replacing the delete-button slot layout as appropriate — keep delete
  reachable in edit mode). It needs `accounts`, `selectedAccountId`, and an
  `onAccountSelected` callback passed down.
- `TransactionEditUiState`: add `accounts: List<Account> = emptyList()` and ensure
  `selectedAccountId` is exposed (VM already tracks it).
- `TransactionEditViewModel`: include `accountRepository.observeAll()` in the state combine
  so `accounts` is populated. On `AccountSelected` intent (already exists), ALSO call
  `appSettingsRepository.setSelectedAccountId(id.value)` so the choice is global. Keep the
  existing currency/budget recompute behaviour.
- `TransactionEditScreen.kt`: thread accounts + onIntent(AccountSelected) into the header.

### 4. Overview — add selector top-right
- `feature/overview/.../OverviewUiState.kt`: add
  `accounts: List<Account> = emptyList()` and `selectedAccountId: AccountId? = null`.
- `OverviewViewModel.kt`: the combine already has `accountRepository.observeAll()` and
  `_selectedAccountId` (lines ~95-152). Populate the two new state fields. Add intents
  `ShowWalletPicker(visible: Boolean)` and `AccountSelected(id: AccountId)` to the
  `OverviewIntent` sealed interface (package `com.dv.moneym.feature.overview.page`). On
  `AccountSelected` → `viewModelScope.launch { appSettingsRepository.setSelectedAccountId(id.value) }`
  (currency recomputes from selected account at OverviewViewModel.kt:134).
- `OverviewHeader.kt` + `OverviewScreen.kt` (header invocation at ~line 152): add a
  trailing `WalletSelector` wired to `state.accounts`, `state.selectedAccountId`, and
  `onIntent(OverviewIntent.AccountSelected(it))`.

### 5. Budget create/edit — replace chip FlowRow with selector
- `feature/budgets/.../create/BudgetCreateScreen.kt` lines ~207-231: replace the
  `FlowRow { state.availableAccounts.forEach { MmChip {...} } }` block with a labelled
  `WalletSelector(accounts = state.availableAccounts, selectedAccountId = state.selectedAccountId, onSelect = { onIntent(BudgetCreateIntent.AccountSelected(it)) })`.
  Keep the `budgets_account_label` label above it.
- `BudgetCreateViewModel`: no signature change needed (intent `AccountSelected` exists).
  Verify `selectedAccountId`/`availableAccounts` types match `AccountId`/`List<Account>`.

## Conventions (CLAUDE.md)
- One `onIntent` entry per VM; new behaviour = new Intent variants, not new public methods.
- Public VMs/usecases (Koin can't see `internal` across modules).
- No hardcoded currency/date in UiState defaults.
- Every user-visible string in all 4 langs.
- Import classes; no fully-qualified names.

## Build / verify
- `./gradlew :core:ui:compileDebugKotlinAndroid`
- `./gradlew :feature:transactions:compileDebugKotlinAndroid :feature:transactionEdit:compileDebugKotlinAndroid :feature:overview:compileDebugKotlinAndroid :feature:budgets:compileDebugKotlinAndroid`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- Run any existing affected module unit tests (`:feature:overview:testDebugUnitTest` etc.).
- Report a summary of files changed.
