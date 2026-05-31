# Phase 3: Budgets wallet chip

## What changes

File: `feature/budgets/src/commonMain/kotlin/com/dv/moneym/feature/budgets/list/BudgetListScreen.kt`

### Remove horizontal account filter chip row
Remove lines 104-124 (the `if (state.accounts.size > 1) { Row { ... MmChip ... } }` block).

### Add WalletSelector to ScreenHeader trailing content
Replace current ScreenHeader call (line 102):
```kotlin
ScreenHeader(title = stringResource(Res.string.budgets_title), onBack = onBack)
```

With:
```kotlin
ScreenHeader(
    title = stringResource(Res.string.budgets_title),
    onBack = onBack,
    trailingContent = if (state.accounts.size > 1) {
        {
            WalletSelector(
                accounts = state.accounts,
                selectedAccountId = state.selectedAccountId,
                onSelect = { onIntent(BudgetListIntent.AccountSelected(it)) },
            )
        }
    } else null,
)
```

### Update imports
- ADD: `import com.dv.moneym.core.ui.WalletSelector`
- REMOVE: `import androidx.compose.foundation.horizontalScroll`
- REMOVE: `import androidx.compose.foundation.rememberScrollState`
- REMOVE: `import com.dv.moneym.core.ui.MmChip`
- REMOVE: `import moneym.feature.budgets.generated.resources.budgets_account_filter_label` (if unused)

`WalletSelector` is in `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/WalletSelector.kt`. It takes `accounts: List<Account>`, `selectedAccountId: AccountId?`, `onSelect: (AccountId) -> Unit`, `modifier: Modifier = Modifier`.

`BudgetListUiState` already has `accounts: List<Account>` and `selectedAccountId: AccountId?`.
`BudgetListIntent.AccountSelected(id: AccountId)` already exists.

## Build verification
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
