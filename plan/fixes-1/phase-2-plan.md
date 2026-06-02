# Phase 2 — Mapper tests (pure)

Write behavior-complete unit tests for all 6 mapper files. Pure functions → no fakes/coroutines needed; `kotlin.test` assertions suffice (each data module's `commonTest` already has `libs.kotlin.test` + `projects.core.testing`). Place each test in the SAME package as the mapper (mappers are `internal`), under the module's `src/commonTest/kotlin/...`.

Cover every mapping function + every branch (null fields, enum parsing, default/override params, round-trips where both directions exist).

## Files & functions

1. `data/accounts/.../internal/AccountMappers.kt` → `AccountMappersTest`
   - `AccountEntity.toDomain()`, `AccountEntity.toSyncRow()`. Cover null `colorHex`, `isDefault`/`archived` combos, enum/type string → domain.

2. `data/budgets/.../internal/BudgetMappers.kt` → `BudgetMappersTest`
   - `BudgetEntity.toDomain()`, `BudgetEntity.toSyncRow()`, `parsePeriodType(raw)`, `parseYearMonth(raw)`. Cover each `BudgetPeriodType` raw value + an invalid/fallback raw; valid + malformed `YearMonth` key.

3. `data/categories/.../internal/CategoryMappers.kt` → `CategoryMappersTest`
   - `CategoryEntity.toDomain()`, `CategoryEntity.toSyncRow()`. Cover null colorHex/icon fields, archived flag, type string.

4. `data/transactions/.../internal/TransactionMappers.kt` → `TransactionMappersTest`
   - `TransactionEntity.toDomain()`, `TransactionEntity.toSyncRow()`, `yearMonthKey(year, month)` (zero-padding e.g. month 1 → "01", month 12 → "12"). Cover null note, income vs expense type.

5. `data/transactions/.../internal/RecurringTransactionMappers.kt` → `RecurringTransactionMappersTest`
   - `RecurringTransactionEntity.toDomain()`, `RecurringTransaction.toEntity()`, `RecurringTransactionEntity.toSyncRow()`. Round-trip `toEntity().toDomain()` equality on the shared fields. Cover each frequency/interval enum and null end date.

6. `data/backup/DtoMappers.kt` → `DtoMappersTest` (these are `public fun`, package `com.dv.moneym.data.backup`)
   - `Category`/`Account`/`Transaction`/`RecurringTransaction`/`Budget` each: `toDto()` and `toDomain()`. Verify `toDto().toDomain()` round-trips. Cover the `idOverride` param (default vs explicit) on the `*Dto.toDomain` functions, null optional fields, and the `RecurringTransaction.toDto()`/`RecurringTransactionDto.toDomain()` branch logic (it has a function body, not expression — check its conditional fields).

## Construction notes
- Read each Entity/Dto/domain model to build valid fixtures. Use `core:model` types (`Money`, `CurrencyCode`, `CategoryId`, `AccountId`, `YearMonth`, `LocalDate`, `Instant`). Reuse fixture patterns from existing tests in the same module (e.g. `data/transactions/src/commonTest/.../TransactionRepositoryTest.kt`).
- No comments unless non-obvious. Import classes, no FQN.

## Verify
```
./gradlew :data:accounts:testDebugUnitTest :data:budgets:testDebugUnitTest \
  :data:categories:testDebugUnitTest :data:transactions:testDebugUnitTest \
  :data:backup:testDebugUnitTest
```
All green.
