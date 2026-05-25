# Phase 3 — Overview budget breakdown card

**Goal:** Show per-budget progress at the top of Overview, after `IncomeExpensesCard` and before `AvgStatsCard`.

**Depends on:** Phase 1 (Phase 2 not required to ship; budgets can be seeded for testing).

## Files to create

- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/usecase/BuildBudgetProgressUseCase.kt`
  ```kotlin
  class BuildBudgetProgressUseCase {
      operator fun invoke(
          budgets: List<Budget>,
          periodTxns: List<Transaction>,
          period: OverviewPeriod,
          catMap: Map<CategoryId, Category>,
      ): List<BudgetProgress>
  }

  data class BudgetProgress(
      val budget: Budget,
      val spent: Money,
      val remaining: Money,        // negative when overrun
      val fraction: Float,         // spent / amount, clamped [0, 1] for the bar
      val isOverrun: Boolean,
      val categoryName: String?,   // null when budget.categoryId == null
  )
  ```
  Logic:
  - For `OverviewPeriod.Month(ym)`: keep only budgets where `budget.isActiveIn(ym)`. For each: spent = sum of `periodTxns` where `tx.type == EXPENSE` AND (`budget.categoryId == null` OR `tx.categoryId == budget.categoryId`). Return sorted by `fraction` desc (worst-first).
  - For `OverviewPeriod.Year(year)`: skip in v1 (return empty) — leave a comment hook for future multi-month rollup.
  - For `OverviewPeriod.DateRange`: skip in v1 (return empty).
- `feature/overview/.../components/BudgetBreakdownCard.kt`
  - `MmCard` with header (`Res.string.overview_budgets_title`), then a column of `BudgetProgressRow`s.
  - `BudgetProgressRow`: name on left, `MmMoney(spent) / MmMoney(amount)` on right, `LinearProgressIndicator(progress = { it.fraction })` below; tint with `MaterialTheme.colorScheme.error` when `isOverrun`.

## Files to modify

- `feature/overview/build.gradle.kts` — add `implementation(projects.data.budgets)`.
- `feature/overview/.../page/OverviewPageUiState.kt` — add field:
  ```kotlin
  val budgetProgress: List<BudgetProgress> = emptyList(),
  ```
- `feature/overview/.../page/OverviewPageViewModel.kt`
  - Inject `budgetRepository: BudgetRepository` and `buildBudgetProgress: BuildBudgetProgressUseCase`.
  - Extend the `combine(...)` block to include `budgetRepository.observeAll()`. Populate `budgetProgress` via the use case.
  - Keep the body under ~250 lines per `CLAUDE.md`; if combine arity grows past Kotlin's overload limit, switch to `combine(listOf<Flow<*>>(...))` + cast, or split a helper.
- `feature/overview/.../components/OverviewPeriodBody.kt` — between `IncomeExpensesCard(...)` (~line 89) and `AvgStatsCard(...)` (~line 90):
  ```kotlin
  if (state.budgetProgress.isNotEmpty()) {
      BudgetBreakdownCard(progress = state.budgetProgress, currencyCode = currencyCode)
  }
  ```
- `composeApp/.../di/FeatureModules.kt`
  - `single { BuildBudgetProgressUseCase() }`.
  - Update the `OverviewPageViewModel` provider's constructor args to inject `BudgetRepository` + the new use case.
- `feature/overview/src/commonMain/composeResources/values{,-de,-es,-it}/strings.xml`
  - `overview_budgets_title`, `overview_budgets_all_categories`, `overview_budgets_overrun_suffix`, `overview_budgets_remaining_suffix`.

## Tests

- `feature/overview/.../usecase/BuildBudgetProgressUseCaseTest.kt`
  - Per-category budget — spent counts only matching txns.
  - All-categories budget (`categoryId == null`) — spent sums all expense txns.
  - Overrun — `isOverrun == true`, `remaining` negative, `fraction == 1f`.
  - Inactive budget (e.g. `ym < startYearMonth`) — excluded.
  - Recurring expansion — N-months budget shown across months `[start, start+N)`, hidden after.
  - Year + DateRange — empty list (v1).
- Extend `OverviewPageViewModelTest` — assert `budgetProgress` non-empty when a budget exists for the current month; empty when none.

## Verify

```bash
./gradlew :feature:overview:testDebugUnitTest
./gradlew :composeApp:assembleDebug
# Manual: with a "Groceries €400" budget for current month, Overview shows the progress card → spend > €400 → row flips to overrun styling.
```

## Done when

- All commands green.
- Manual smoke: budget card appears at the right slot, progress reflects real spend, overrun styling triggers above 100%.
- Year/DateRange periods do not crash and simply hide the card.
