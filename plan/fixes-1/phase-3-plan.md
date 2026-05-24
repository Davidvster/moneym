# Phase 3 — Dumb UI (move remember-state into VMs)

Run from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`.

## Goal

Move every `remember { mutableStateOf(...) }` that holds domain or business state out of Composables and into the owning ViewModel's `UiState`. The Composable drives changes via `onIntent(...)`.

Keep purely-UI transient state in the Composable (LazyListState, pager scroll positions, animation flags like `initialScrollDone`, focus, calculator/date-picker visibility flags that don't impact business logic).

For every UiState field you add, add a corresponding Intent variant. Logic that depended on the local var (e.g. `if (showMonthPicker)`) now reads from `state.showMonthPicker`. The dialog dismissal callback calls `onIntent(<Vm>Intent.ShowMonthPicker(false))`.

---

## Targets

### 1. `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListScreen.kt`

Lines 134–137 — lift these into `TransactionListUiState` (file `TransactionListUiState.kt`) and add matching `TransactionListIntent` variants:
- `var isSearchActive` → `TransactionListUiState.isSearchActive: Boolean = false` + `TransactionListIntent.ToggleSearch(active: Boolean)`
- `var showMonthPicker` → `TransactionListUiState.showMonthPicker: Boolean = false` + `TransactionListIntent.ShowMonthPicker(visible: Boolean)`
- `var showWalletSwitcher` → `TransactionListUiState.showWalletSwitcher: Boolean = false` + `TransactionListIntent.ShowWalletSwitcher(visible: Boolean)`
- `var showCategoryFilter` → `TransactionListUiState.showCategoryFilter: Boolean = false` + `TransactionListIntent.ShowCategoryFilter(visible: Boolean)`

Leave `var initialScrollDone by remember { mutableStateOf(false) }` — pure UI sync flag.

VM `onIntent` handles each new intent with `_uiBooleans.update { ... }` or similar (use whatever pattern the VM already uses for its existing `_filter`/`_currentMonth` StateFlows).

When `TransactionListIntent.SearchQueryChanged("")` is dispatched (search cancelled), also flip `isSearchActive` to false implicitly — but only if that matches current behavior. Otherwise issue a separate `ToggleSearch(false)` intent from the screen's "close" button.

### 2. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewScreen.kt`

Lines 77–78 — lift to `OverviewUiState` + `OverviewIntent`:
- `var showPeriodPicker` → `OverviewUiState.showPeriodPicker: Boolean = false` + `OverviewIntent.ShowPeriodPicker(visible: Boolean)`
- `var showDateRangePicker` → `OverviewUiState.showDateRangePicker: Boolean = false` + `OverviewIntent.ShowDateRangePicker(visible: Boolean)`

Leave `initialMonthScrollDone` and `initialYearScrollDone` — UI sync flags.

### 3. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt`

Lines 97–98 (sheet visibility + editing entity):
- `var showNewCategorySheet` → `CategoryListUiState.showCategoryEditSheet: Boolean = false`
- `var categoryToEdit: Category?` → `CategoryListUiState.editingCategory: Category? = null`

Lines 222–224 (validation/dialog flags inside `NewCategorySheet`):
- `var nameError: String?` → `CategoryListUiState.nameError: String? = null`
- `var showDeleteConfirm` → `CategoryListUiState.showDeleteConfirm: Boolean = false`
- `var showColorPicker` → `CategoryListUiState.showColorPicker: Boolean = false`

Form-name editing state (lines 209-211 currently `var name by remember(categoryToEdit?.id)`) → `CategoryListUiState.editingName: String = ""` + `CategoryListIntent.EditingNameChanged(name: String)`. The VM's existing `CreateCategory`/`UpdateCategory` handler should validate the duplicate/blank rules (the validation that the Phase 2 builder pushed into the Screen — move it back into the VM now).

New intents:
- `CategoryListIntent.ShowCategoryEditSheet(visible: Boolean)`
- `CategoryListIntent.StartEditCategory(category: Category)` (sets editingCategory + editingName + opens sheet)
- `CategoryListIntent.EditingNameChanged(name: String)`
- `CategoryListIntent.ShowDeleteConfirm(visible: Boolean)`
- `CategoryListIntent.ShowColorPicker(visible: Boolean)`

Leave `customColors by remember(categoryToEdit?.id)` — that's a transient color-picker history specific to the picker dialog, not business state.

### 4. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsScreen.kt`

Line 188 — lift `var showLockPicker` → `SettingsUiState.showLockPicker: Boolean = false` + `SettingsOverviewIntent.ShowLockPicker(visible: Boolean)`.

### 5. `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionEdit/TransactionEditScreen.kt`

Line 94 — lift `var showDeleteDialog by rememberSaveable` → `TransactionEditUiState.showDeleteDialog: Boolean = false` + `TransactionEditIntent.ShowDeleteDialog(visible: Boolean)`.

Leave lines 95–96 (`showDatePicker`, `showCalculator`) — pure UI pickers, no business coupling. Keep as `rememberSaveable`.

### 6. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/wallet/AddWalletScreen.kt` (function `AddWalletCurrencyPickerScreen`)

Line 184 — lift `var searchQuery by remember { mutableStateOf("") }` into the wallet picker's ViewModel. Check which VM backs this screen (`AddWalletViewModel` or a dedicated currency-picker subview model) — likely needs `searchQuery: String = ""` on its UiState + `AddWalletIntent.SearchQueryChanged(query: String)`.

Lines 186–201 — the filtering logic (`derivedStateOf` computing popular currencies) currently happens in the Composable. Move that computation into the VM: VM exposes pre-filtered lists in state; the Composable just renders them. Add the filter logic into the VM's existing state-flow chain (combine `searchQuery` with the static currency list, emit filtered result via `state.filteredCurrencies` and `state.popularCurrencies`).

---

## ViewModel-side wiring pattern

For each new boolean intent like `ShowMonthPicker(visible: Boolean)`, add a private `MutableStateFlow<Boolean>` (or a single struct of all UI booleans), combine it into the main state flow, and the `onIntent` handler does `_showMonthPicker.value = intent.visible`.

If the VM already has a single `MutableStateFlow<<Vm>UiState>` (rather than many separate sub-flows), just `_state.update { it.copy(showMonthPicker = intent.visible) }`.

Stick with the existing module convention — look at how each VM currently mutates its state, mirror that.

---

## Verification

```
./gradlew :composeApp:assembleDebug
./gradlew :feature:transactions:compileDebugKotlinAndroid \
          :feature:overview:compileDebugKotlinAndroid \
          :feature:categories:compileDebugKotlinAndroid \
          :feature:settings:compileDebugKotlinAndroid \
          :feature:transactionEdit:compileDebugKotlinAndroid
```

Both must pass.

Audit:
```
grep -rn "remember { mutableStateOf" feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListScreen.kt \
                                     feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewScreen.kt \
                                     feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt \
                                     feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsScreen.kt \
                                     feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionEdit/TransactionEditScreen.kt \
                                     feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/wallet/AddWalletScreen.kt
```

Should print only the lines for the explicitly-kept UI-only flags listed in this spec (`initialScrollDone`, `initialMonthScrollDone`, `initialYearScrollDone`, `customColors`, `showDatePicker`, `showCalculator`).

Stop and report when verification passes. List every file changed.
