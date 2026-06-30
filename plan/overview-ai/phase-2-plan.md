# Phase 2 Plan: Modular Overview Blocks

## Goal

Refactor overview rendering so the page is assembled from adaptable block descriptors controlled by persisted layout preferences, while preserving the existing default visual order and behavior.

## Implementation

- Add `data:overview` as a dependency of `feature:overview`.
- Add a pure use case under `feature/overview/.../usecase/` to resolve visible overview blocks:
  - Inputs: period, `SpendingFilter`, `OverviewLayoutPrefs`, `OverviewPageUiState`, saved enabled AI widgets.
  - Output: ordered `OverviewResolvedBlock` list.
  - Built-in defaults must match current screen order:
    - all periods: totals, budget progress when non-empty, averages, category breakdown
    - month: cumulative spend, category trends
    - year: monthly spend when not income-only, monthly income when not expense-only, monthly net when filter is all, category trends
    - date range: category trends only when range trends exist
  - Hidden persisted blocks are omitted.
  - Unknown persisted block ids are ignored.
  - AI widget blocks are included after built-ins according to saved order when enabled.
- Extend `OverviewPageUiState` with `blocks: List<OverviewResolvedBlock>`.
- Update `OverviewPageViewModel` to combine `OverviewRepository.observeLayoutPrefs()` and `OverviewRepository.observeAiWidgets()` with the existing page state.
- Refactor `OverviewPeriodBody`:
  - Iterate `state.blocks`.
  - Move existing card calls into a built-in block renderer.
  - Keep existing cards visually unchanged.
  - Keep existing UI-only transients, such as category percentage toggle, inside the composable.
- Add an edit/customize icon to `OverviewHeader`:
  - Use an existing `Icon`/`MoneyMIcons` equivalent from the design system.
  - Localized content description in all overview locales.
  - `OverviewScreen` exposes `onCustomizeOverview` callback; phase 3 will wire it to settings navigation.

## Tests

- Unit tests for the block resolver:
  - default month/year/date-range order
  - hidden blocks omitted
  - budget block omitted when no budget progress
  - year chart blocks honor spending filter
  - unknown ids ignored
  - enabled AI widgets included in order
- Update `OverviewPageViewModelTest` to inject `FakeOverviewRepository` and verify hidden blocks flow through.
- Run:
  - `./gradlew :feature:overview:testDebugUnitTest`

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md`:
- Phase 2 status
- commands run and results
- commit hash after committing

Commit only Phase 2 changes.
