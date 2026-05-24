# Phase 2 — Intent-only ViewModel public API

Run from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`.

## Goal

Every ViewModel exposes a single public entry point: `fun onIntent(intent: <VmName>Intent)`. Everything else is `private` (or `internal val state` flow). Fold existing extra public functions into Intent variants, and add an Intent sealed interface where none exists.

Each VM has a corresponding `*Intent.kt` file (or sealed interface in the VM file). Use whichever convention the module already uses — look at `TransactionListIntent.kt` for reference.

For every VM changed, also update the consuming Composable Screen to use `viewModel::onIntent` + intent construction instead of `viewModel.foo()` direct calls.

---

## VMs to refactor — fold extra public fns into Intent

For each of the following, list the existing extra public funcs (you'll see them in the file), add a matching Intent variant, route through `onIntent`, and delete the original public fn. Keep behavior identical.

### 1. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListViewModel.kt`
Existing public fns to fold: `setTab(...)`, `reorder(...)`, `createCategory(...)`, `updateCategory(...)`, `deleteCategory(...)`.

Update or create `CategoryListIntent.kt` in the same package with variants like:
```kotlin
sealed interface CategoryListIntent {
    data class SetTab(val tab: <ExistingTabType>) : CategoryListIntent
    data class Reorder(val from: Int, val to: Int) : CategoryListIntent  // match real signature
    data class CreateCategory(...) : CategoryListIntent
    data class UpdateCategory(...) : CategoryListIntent
    data class DeleteCategory(val id: CategoryId) : CategoryListIntent
}
```
Read the existing fn signatures first, then mirror them as `data class`/`data object` variants. `onIntent` body uses `when (intent) { is Tab -> setTab(intent.tab); ... }` and the helpers become `private`.

### 2. `feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/security/OnboardingSecurityViewModel.kt`
Fold `onReturnFromPinSetup()` → `OnboardingSecurityIntent.ReturnFromPinSetup` (data object).

### 3. `feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/currency/OnboardingCurrencyViewModel.kt`
Fold `onRestoreFileSelected(...)` → `OnboardingCurrencyIntent.RestoreFileSelected(...)` (data class — copy the parameter list).

### 4. `feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/setup/PinSetupViewModel.kt`
Fold `reset()` → `PinSetupIntent.Reset` (data object).

### 5. `feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/unlock/PinUnlockViewModel.kt`
Fold:
- `setBiometricPrompt(...)` → `PinUnlockIntent.SetBiometricPrompt(...)`
- `onResume()` → `PinUnlockIntent.Resume`

### 6. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SecuritySettingsViewModel.kt`
Fold `refreshPinState()` → `SecuritySettingsIntent.RefreshPinState` (data object).

### 7. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/paymentmodes/PaymentModeListViewModel.kt`
Fold:
- `showAddDialog()`, `showRenameDialog(...)`, `showDeleteConfirm(...)`, `dismissDialog()`, `createMode(...)`, `renameMode(...)`, `deleteMode(...)`
→ `PaymentModeListIntent.{ShowAdd, ShowRename, ShowDelete, Dismiss, Create, Rename, Delete}` with matching params.

---

## VMs missing Intent entirely — add sealed interface + onIntent

For each: create an `Intent` sealed interface (file alongside the VM), add `fun onIntent(intent: <Name>Intent)` to the VM, fold existing public setters into Intent variants, mark setters `private`.

### 8. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/SettingsOverviewViewModel.kt`
Existing public fns: `setThemeMode(...)`, `setDefaultTransactionType(...)`, `setPaymentModeEnabled(...)`.
New: `SettingsOverviewIntent.{SetThemeMode(mode), SetDefaultTransactionType(type), SetPaymentModeEnabled(enabled)}`.

### 9. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/currencypicker/CurrencyPickerViewModel.kt`
Existing public fn: `setDefaultCurrency(...)`.
New: `CurrencyPickerIntent.SetDefaultCurrency(currency)`.

### 10. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/locale/LanguagePickerViewModel.kt`
Existing public fn: `setLanguage(...)`.
New: `LanguagePickerIntent.SetLanguage(language)`.

### 11. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/transactiondisplay/TxListDisplayViewModel.kt`
Existing public fns: `setTxDisplayPrefs(...)`, `setDefaultTransactionType(...)`.
New: `TxListDisplayIntent.{SetTxDisplayPrefs(prefs), SetDefaultTransactionType(type)}`.

---

## Screen-side updates

After each VM change, update the consuming screen. Replace patterns like:
```kotlin
viewModel::setThemeMode
viewModel.refreshPinState()
onClick = { viewModel.createCategory(name) }
```
with:
```kotlin
{ viewModel.onIntent(SettingsOverviewIntent.SetThemeMode(it)) }
viewModel.onIntent(SecuritySettingsIntent.RefreshPinState)
onClick = { viewModel.onIntent(CategoryListIntent.CreateCategory(name)) }
```

Use `grep -rn "viewModel\.<funcName>\|viewModel::<funcName>" feature/ composeApp/` to find every call site for each removed function.

For each test file under `feature/*/src/commonTest/` that calls the removed public fn, update it to use `onIntent(Intent.X)` instead. Tests must still compile and pass.

---

## Verification

```
./gradlew compileDebugKotlinAndroid 2>&1 | tail -40
./gradlew :composeApp:assembleDebug
./gradlew allTests 2>&1 | tail -30
```

All must pass. Then audit:

```
for vm in CategoryListViewModel OnboardingSecurityViewModel OnboardingCurrencyViewModel \
          PinSetupViewModel PinUnlockViewModel SecuritySettingsViewModel \
          PaymentModeListViewModel SettingsOverviewViewModel CurrencyPickerViewModel \
          LanguagePickerViewModel TxListDisplayViewModel; do
  echo "=== $vm ==="
  file=$(grep -rln "^class $vm\b" feature/*/src/commonMain --include="*.kt" | head -1)
  grep -E "^\s+(fun|suspend fun) [a-zA-Z]" "$file" | grep -v "onIntent\|private\|internal"
done
```

Should report nothing except (optionally) `onIntent` lines. Any other public `fun` on a VM means the refactor is incomplete.

Stop and report when verification passes. List every file changed.
