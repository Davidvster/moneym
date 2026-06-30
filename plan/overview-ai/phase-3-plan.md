# Phase 3 Plan: Overview Settings Screen

## Goal

Add a dedicated overview customization settings screen reachable from both the Settings tab and the Overview header edit action. Users can toggle/reorder built-in overview blocks and enable/disable saved AI widgets.

## Implementation

- Add `data:overview` dependency to `feature:settings`.
- Add navigation:
  - `OverviewSettingsKey : NavKey` in the settings feature.
  - `overviewSettingsEntry(...)` extension.
  - Wire in `shared/MainNav.kt`.
  - Settings tab row navigates to `OverviewSettingsKey`.
  - Overview header edit callback also pushes `OverviewSettingsKey`.
- Add Settings home entry:
  - Add “Overview” row in Preferences, with an appropriate existing icon.
  - Add localized strings in `feature/settings` base + all 27 locale files.
- Add `OverviewSettingsViewModel`:
  - Public class, registered in `FeatureModules.kt`.
  - Single `onIntent`.
  - Observes `OverviewRepository.observeLayoutPrefs()` and `observeAiWidgets()`.
  - Emits default built-in block rows when no persisted layout exists.
  - Intents: toggle built-in block, move block up/down or reorder by ids, reset defaults, toggle AI widget, edit AI widget, create AI widget.
  - Effects for navigating to widget builder with optional widget id.
- Add screen UI:
  - Top app bar/back pattern matching existing settings sub-screens.
  - Built-in overview items section with toggles.
  - Reorder enabled built-in blocks using the existing `reorderable` dependency if already wired in `feature:settings`; otherwise use up/down icon buttons for this phase and leave drag reorder for later within this phase only if trivial.
  - AI widgets section listing saved widgets with enable toggle, edit action, and create button.
  - Reset to defaults action.
  - All text localized in all locales.

## Tests

- `OverviewSettingsViewModelTest`:
  - default rows emitted when repository layout is empty
  - toggle persists visibility
  - reorder persists order
  - reset restores default order/visibility
  - AI widget toggle persists
  - create/edit widget effects emitted
- Update settings screen tests/previews as needed.
- Run:
  - `./gradlew :feature:settings:testDebugUnitTest :feature:overview:testDebugUnitTest --no-configuration-cache`

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md`:
- Phase 3 status
- commands run and results
- commit hash after committing

Commit only Phase 3 changes.
