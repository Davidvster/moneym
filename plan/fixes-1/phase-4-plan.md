# Phase 4 — Split long functions, extract use cases

Run from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`.

## Goal

Cap every function/composable at roughly 60 lines (soft 80 max). For ViewModels with >200 lines of body, pull pure (no Flow / no ViewModelScope) logic out into use cases under `feature/<module>/.../usecase/`. The ViewModel keeps flow wiring, state holding, and intent dispatch. Use cases are plain classes constructed by Koin.

Goal is not to add abstractions for their own sake — only split where a pure block exists that has a clear input/output. Do not extract from VMs <200 lines.

Behavior must be identical. Do not change observable state shape.

---

## Targets

### 1. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageViewModel.kt` (435 lines)

Identify pure private helpers inside this file. Typical candidates:
- Date-range math: `OverviewPeriod` → start/end LocalDate. Extract to `ResolvePeriodRangeUseCase` (or similar — pick a clear name).
- Category aggregation: list of transactions + categories → `List<CategorySpend>` / `List<CategoryTrend>`. Extract to `BuildCategoryBreakdownUseCase` and `BuildCategoryTrendsUseCase`.
- Cumulative totals + series builders for the chart cards. Extract to `BuildCumulativeSeriesUseCase`.

Place new use cases under `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/usecase/`.

The use cases take primitive inputs (List<Transaction>, List<Category>, OverviewPeriod, etc.) and return data classes already defined in `OverviewPageUiState.kt` / `OverviewUiState.kt`. No DI for the use cases beyond constructor — pass dependencies they need.

Wire each use case in Koin (`composeApp/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`) with `single { ResolvePeriodRangeUseCase() }` etc, and inject them into `OverviewPageViewModel` constructor.

Target: bring `OverviewPageViewModel` body under ~250 lines. No single VM function over ~60 lines.

### 2. `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/TransactionEditViewModel.kt` (320 lines)

Extract:
- Validation (amount non-zero, category selected, account selected, etc.) + assembly of the `Transaction` domain object → `ValidateAndBuildTransactionUseCase`. Input: current edit state fields. Output: `Result<Transaction>` or sealed `ValidationOutcome.{Ok(tx), Invalid(reason)}`.

Place under `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/usecase/`.

VM calls the use case from `onIntent(Save)` handler. Wire in `FeatureModules.kt`.

### 3. `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/importdata/ImportDataViewModel.kt` (241 lines)

Extract:
- File parsing + duplicate diff → `PrepareImportPreviewUseCase`. Input: raw file bytes / URI / string. Output: `ImportPreview` data class (already exists in UiState — reuse it; if not, define under the same usecase package).

Place under `feature/settings/.../overview/importdata/usecase/`. Wire in `FeatureModules.kt`.

### 4. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewViewModel.kt` (239 lines)

Split private helpers into smaller functions only if helpful — VM is just over 200 lines. Don't extract a use case unless there's a clearly pure block. Acceptable to leave at 239 if all functions are already <60 lines.

Quick check: count functions over 60 lines with:
```
awk '/^    (private |internal |public )?fun/{name=$0; start=NR} /^    }$/{if (start) print NR-start, name; start=0}' \
  feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewViewModel.kt
```
Split any function reporting >60 lines.

### 5. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/components/OverviewPeriodBody.kt` (577 lines)

Split the giant private composables:
- `SpendingByCategoryCard` (~100 lines) → split into header + body sub-composables.
- `SpendingByCategoryLegend` (~120 lines) → split into `LegendHeaderRow`, `LegendDataRow`, `LegendTotalToggle` private @Composables in the same file.
- `CategoryTrendsCard` (~150 lines) → split into `TrendsHeader`, `TrendsRow`, `TrendsChart` private @Composables.

Each new sub-composable stays in the same file (no new file needed). No behavior change.

### 6. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt` (276 lines)

`NewCategorySheet` is the largest composable. Split into smaller private @Composables in the same file: `NameFieldBlock`, `IconRow`, `ColorRow`, `DeleteConfirmDialog`, `ColorPickerDialog`. Keep them in the same file.

---

## Constraints

- Use cases live in `commonMain` and have ZERO Compose / Koin / Flow / Coroutine dependencies in their bodies (constructor injection only; the `operator fun invoke(...)` can be `suspend` if the original VM code was suspending).
- Each new use case must be wired in `composeApp/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`.
- Do not break public state-shape — UiState fields stay identical.
- Do not change Intent surface — Phase 2 + 3 already locked it.
- No new tests in this phase (Phase 5 does that).

---

## Verification

```
./gradlew :composeApp:assembleDebug
```

Line-count audit — none of these files should exceed roughly 350 lines after the split:
```
wc -l feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageViewModel.kt \
      feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/TransactionEditViewModel.kt \
      feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/importdata/ImportDataViewModel.kt \
      feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt
```

Function-length audit (rough — should report no values >70):
```
for f in feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageViewModel.kt \
         feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/TransactionEditViewModel.kt \
         feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/importdata/ImportDataViewModel.kt \
         feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewViewModel.kt; do
  echo "=== $f ==="
  awk '/^[[:space:]]*(private |internal |public )?(suspend )?fun [a-zA-Z]/{name=$0; start=NR} /^[[:space:]]*}$/{if (start) print NR-start, name; start=0}' "$f" | sort -rn | head -5
done
```

Stop and report when build passes. List every file changed and every new use case created.
