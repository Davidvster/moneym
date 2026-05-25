# Budgets Feature

Monthly spending caps — either total ("€2000 this month") or per-category ("€400 groceries"). Budgets are visible at the top of the Overview tab as a progress breakdown, and inline when picking a category during transaction entry. Budgets are created from Settings → Budgets.

Data model is forward-compatible:
- `BudgetPeriodType` enum starts with `MONTHLY` (room for `YEARLY`).
- Recurring field uses `-1 = unlimited / null = single / N = N months`, the same shape the future Recurring Payments feature will reuse.

## Phases

1. [Phase 1 — Data layer](phase-1-plan.md) — `:data:budgets` Room module, `Budget` domain model, fake, DI.
2. [Phase 2 — Settings entry + `:feature:budgets`](phase-2-plan.md) — list/create/edit/delete screens, nav, Settings row.
3. [Phase 3 — Overview budget breakdown card](phase-3-plan.md) — `BuildBudgetProgressUseCase` + `BudgetBreakdownCard`.
4. [Phase 4 — Tx-edit budget remaining chip](phase-4-plan.md) — `ComputeCategoryBudgetRemainingUseCase` + `BudgetRemainingChip`.

Status: see [status.md](status.md).

## Domain Model

```kotlin
// core/model/Budget.kt
@Serializable
data class Budget(
    val id: BudgetId,
    val name: String,
    val amount: Money,
    val categoryId: CategoryId?,           // null = all categories (whole-period cap)
    val periodType: BudgetPeriodType,      // MONTHLY for now (YEARLY future)
    val startYearMonth: YearMonth,
    val recurringMonths: Int?,             // null = single, -1 = unlimited, N = N occurrences
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class BudgetPeriodType { MONTHLY }    // YEARLY reserved for future

@Serializable data class BudgetId(val value: Long)   // add to core/model/Ids.kt
```

**Active-in-month rule** (`Budget.isActiveIn(ym: YearMonth): Boolean`): a budget is active in `ym` when
- `ym >= startYearMonth` AND
- (`recurringMonths == null` && `ym == startYearMonth`) OR
- (`recurringMonths == -1`) OR
- (`recurringMonths != null` && `recurringMonths > 0` && `ym < startYearMonth.plusMonths(recurringMonths)`)

## Cross-cutting

**Phase ordering:** each phase compiles, runs, and has its own tests. Phase 2 needs Phase 1; Phases 3 and 4 each only need Phase 1. Recommended ship order: 1 → 2 → 3 → 4.

**Patterns to reuse:**
- `feature/categories` source layout → mirror for `feature/budgets`.
- `data/categories` Room + DAO + DataSource + Mapper + Repository pyramid → mirror for `data/budgets`.
- `core/testing/FakeCategoryRepository` MutableStateFlow pattern → `FakeBudgetRepository`.
- `feature/overview/.../usecase/BuildCategoryBreakdownUseCase` aggregation pattern (load into memory + Kotlin `groupBy`) → `BuildBudgetProgressUseCase`.
- Intent-only VM surface + dumb-UI rule from `CLAUDE.md`.
- `MmCard`, `MmRow`, `MmField`, `MmChip`, `MM.dimen.*`, `MM.type.*` — no new design-system components needed.

**Future hooks (do NOT build now, but leave doors open):**
- `BudgetPeriodType.YEARLY` — enum already exists; add a `period.yearMonth → period.year` branch in `BuildBudgetProgressUseCase` when added.
- Recurring Payments feature — `recurringMonths: Int?` shape with `-1 = unlimited / null = single` matches. When that lands, extract `Recurrence` into `core/model` and share.
- Notifications when approaching/exceeding a budget — not in scope.
