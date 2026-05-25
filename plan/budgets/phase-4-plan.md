# Phase 4 — Transaction edit budget-remaining chip

**Goal:** When the user picks a category in the transaction add/edit screen (and `type == EXPENSE`), show "€280 / €400 · €120 left" inline under the category picker.

**Depends on:** Phase 1 (Phase 2 not required to ship; budgets can be seeded for testing).

## Files to create

- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/usecase/ComputeCategoryBudgetRemainingUseCase.kt`
  ```kotlin
  class ComputeCategoryBudgetRemainingUseCase(
      private val budgetRepository: BudgetRepository,
      private val transactionRepository: TransactionRepository,
  ) {
      suspend operator fun invoke(
          categoryId: CategoryId,
          date: LocalDate,
          excludingTransactionId: TransactionId? = null,
      ): CategoryBudgetRemaining?
  }

  data class CategoryBudgetRemaining(
      val budgetName: String,
      val amount: Money,
      val spent: Money,
      val remaining: Money,
      val fraction: Float,
      val isOverrun: Boolean,
  )
  ```
  Logic:
  - `ym = date.toYearMonth()`.
  - Load all budgets via `observeAll().first()`. Pick the narrowest active match: a budget where `categoryId == this.categoryId` AND `isActiveIn(ym)`. Fallback: a budget where `categoryId == null` AND `isActiveIn(ym)`. If neither exists, return `null`.
  - Spent = sum of `EXPENSE` txns in `ym` (matching `categoryId` if the chosen budget is per-category; all expenses if it's all-categories), excluding `excludingTransactionId` when set.
- `feature/transactionEdit/.../components/BudgetRemainingChip.kt`
  - Small `MmCard` placed under the category picker. Shows `LinearProgressIndicator` + `MmMoney(spent) / MmMoney(amount) · MmMoney(remaining) left`. Tint error color when `isOverrun`.

## Files to modify

- `feature/transactionEdit/build.gradle.kts` — add `implementation(projects.data.budgets)`.
- `feature/transactionEdit/.../TransactionEditUiState.kt` — add `val budgetRemaining: CategoryBudgetRemaining? = null`.
- `feature/transactionEdit/.../TransactionEditViewModel.kt`
  - Inject `budgetRepository: BudgetRepository` and `computeBudgetRemaining: ComputeCategoryBudgetRemainingUseCase`.
  - When `selectedCategoryId` AND `date` are non-null AND `type == EXPENSE`, recompute `budgetRemaining`. Otherwise set to `null`.
  - In edit mode, pass `excludingTransactionId = state.existingId` so editing the same tx doesn't double-count.
  - Implement reactively: e.g. `combine(_selectedCategoryId, _date, _type) { ... }` → `flatMapLatest` → call the use case → update a private `MutableStateFlow<CategoryBudgetRemaining?>` which feeds into the main `state` combine.
- `feature/transactionEdit/.../components/TransactionEditBody.kt` — after `CategoryPicker(...)` (~line 134):
  ```kotlin
  state.budgetRemaining?.let { BudgetRemainingChip(remaining = it, currencyCode = currencyCode) }
  ```
- `composeApp/.../di/FeatureModules.kt`
  - `single { ComputeCategoryBudgetRemainingUseCase(get(), get()) }`.
  - Update `TransactionEditViewModel` provider to inject the new dependency.
- `feature/transactionEdit/src/commonMain/composeResources/values{,-de,-es,-it}/strings.xml`
  - `tx_edit_budget_remaining_format` (e.g. `"%1$s / %2$s · %3$s left"`), `tx_edit_budget_remaining_overrun_format` (e.g. `"%1$s / %2$s · %3$s over"`).

## Tests

- `feature/transactionEdit/.../usecase/ComputeCategoryBudgetRemainingUseCaseTest.kt`
  - No matching budget → `null`.
  - Per-category budget present → returns it.
  - Only all-categories budget present → returns it.
  - Both per-category and all-categories budgets → per-category wins (narrower).
  - Edit mode with `excludingTransactionId` → excluded txn does not count toward `spent`.
  - Overrun → `isOverrun == true`, `remaining` negative.
- Extend `TransactionEditViewModelTest` — assert `budgetRemaining` updates when intents `CategorySelected` and `DateChanged` fire.

## Verify

```bash
./gradlew :feature:transactionEdit:testDebugUnitTest
./gradlew :composeApp:assembleDebug
# Manual: with a Groceries €400 budget existing, open Add Transaction → pick Groceries → chip appears with remaining → change to a category without a budget → chip disappears → toggle to Income → chip disappears.
```

## Done when

- All commands green.
- Manual smoke covers: no budget hides chip; matching per-cat budget shows correct numbers; all-categories budget shows when no per-cat exists; income type hides chip; editing an existing tx doesn't double-count its own amount.
