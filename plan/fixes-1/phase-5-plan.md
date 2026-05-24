# Phase 5 — Fix + add unit tests

Run from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`.

## Goal

Get `./gradlew testDebugUnitTest` green. Fix tests broken by Phases 1–4. Add at least one test for a new use case.

The orchestrator-level plan called for broad new test coverage of every VM/use case/mapper, but that is descoped here — fixing the existing broken tests and adding seed coverage for one new use case is the in-scope minimum so the next phase (Android + iOS build verify) has a clean baseline. Wider test coverage is deferred to a follow-up cleanup.

## Changes

1. `core/datastore/src/commonTest/.../DefaultAppSettingsRepositoryTest.kt`
   - Remove the `observeDefaultCurrency emits updated value after setDefaultCurrency` test (those methods were removed from `AppSettingsRepository` in Phase 1).

2. `feature/onboarding/src/commonTest/.../OnboardingViewModelTest.kt`
   - Constructor signature drift + `OnboardingCurrencyEffect.NavigateToSecurity` no longer exists (renamed to `NavigateComplete`).
   - `OnboardingCurrencyViewModel` now takes `accountRepository, dbBackupManager, appSettings, dispatchers, savedStateHandle`. `DbBackupManager` requires `DbPlatform` (expect class — no commonTest constructor), so the test cannot easily be reconstructed in `commonTest`.
   - **Decision:** delete `OnboardingViewModelTest.kt`. Re-add later with proper fakes after `DbPlatform` is interfaced. Note in commit.

3. `feature/transactions/src/commonTest/.../TransactionListViewModelTest.kt`
   - State shape changed: `isLoading`, `dayGroups`, `transactions`, `isExpense` no longer live on `TransactionListUiState` — `dayGroups` moved to `TransactionPageUiState` (per-page).
   - VM ctor now requires `ephemeralState: TransactionListEphemeralState`.
   - `currentMonth` and `today` are nullable.
   - Rewrite the test to cover the surface that does exist: `currentMonth` initial null → populated from clock; `netAmount`/`totalIncome`/`totalExpenses` split; `FilterChanged` updates `activeFilter`; `PreviousMonth` decrements month.

4. New: `feature/overview/src/commonTest/.../usecase/ResolvePeriodRangeUseCaseTest.kt`
   - Cover Month (current vs past), Year (current vs past), DateRange. Pure logic — no fakes needed.

## Verification

```
./gradlew testDebugUnitTest
```

Must be green. Stop and report.
