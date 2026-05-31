# Phase 2 — bottom action button padding (task 1)

App is `enableEdgeToEdge()` (MainActivity.kt). Pinned bottom action buttons never apply `navigationBarsPadding()`, so they sit too close to / under the system nav bar. Add `Modifier.navigationBarsPadding()` to the container that wraps each bottom action button so it clears the nav bar; the existing vertical padding then becomes the visible gap.

## Rule
- Append `.navigationBarsPadding()` to the existing `Box`/`Column`/`Row` modifier that wraps the bottom `MmButton`(s). Do NOT add it inside `MmButton`.
- Import `androidx.compose.foundation.layout.navigationBarsPadding`.
- Where the wrapper uses hardcoded `16.dp` for padding, leave the value but you may keep as-is; do not refactor unrelated code.
- Keep ordering sensible: padding then navigationBarsPadding (`.padding(...).navigationBarsPadding()`) — the nav-bar inset sits below the padded content, giving a clean gap.

## Files / locations

1. `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/components/SaveBar.kt` — the `Box` (~L32) wrapping the MmButton. Parent screens already have `imePadding()`; adding `navigationBarsPadding()` here is correct (when keyboard up, ime inset dominates via parent; when down, nav-bar gap shows).

2. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/wallet/AddWalletScreen.kt` — bottom `MmButton` wrapper (~L225-242). If the button is directly in a Column with `.fillMaxWidth().padding(...)`, add `.navigationBarsPadding()` to that modifier.

3. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/wallet/EditWalletScreen.kt` — bottom MmButton (~L186-196).

4. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/wallet/WalletManageScreen.kt` — add-wallet Box (~L208-221).

5. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/paymentmodes/PaymentModeListScreen.kt` — add button wrapper (~L120-128).

6. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/recurring/RecurringListScreen.kt` — new-recurring Box (~L96-109).

7. `feature/budgets/src/commonMain/kotlin/com/dv/moneym/feature/budgets/create/BudgetCreateScreen.kt` — bottom Box (~L285-299).

8. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt` — the "Add category" `Box` at ~L111 (the one in the main `Column`, NOT the in-sheet `NewCategorySaveButton`).

9. `feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/currency/OnboardingCurrencyStep.kt` — bottom button `Box`/Column (~L220-251).

10. `feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/security/OnboardingSecurityStep.kt` — Done/Skip button wrapper (~L187-199).

## Do NOT touch
- `NewCategorySaveButton.kt` (in ModalBottomSheet — sheet handles insets).
- `EditWalletCurrencyScreen.kt` conversion sheet buttons (ModalBottomSheet).
- `MmButton.kt` core component.

For each file: read it, find the wrapper of the bottom action button, append `.navigationBarsPadding()`, add the import. Verify each touched module compiles.

## Verify
```
cd /Users/davidvalic/Developer/MoneyM && ./gradlew \
  :feature:transactionEdit:compileDebugKotlinAndroid \
  :feature:settings:compileDebugKotlinAndroid \
  :feature:budgets:compileDebugKotlinAndroid \
  :feature:categories:compileDebugKotlinAndroid \
  :feature:onboarding:compileDebugKotlinAndroid
```
Report each file's exact edit + compile result.
