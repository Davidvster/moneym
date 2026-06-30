# Phase 1 Plan: Persistence, Models, Backup

## Goal

Add durable overview customization storage that can hold built-in block layout preferences and saved AI widgets, expose it through a repository/fake, wire it into Koin, and include it in JSON backup/export/import/restore.

## Implementation

- Add `:data:overview` to `settings.gradle.kts` using the existing Room/KMP module style from `:data:budgets`.
- Add public data/domain API in `data/overview`:
  - `OverviewBlockId` value class wrapping `String`.
  - `OverviewBuiltInBlockIds` with ids: `totals`, `budget_progress`, `averages`, `category_breakdown`, `cumulative_spend`, `monthly_spend`, `monthly_income`, `monthly_net`, `category_trends`.
  - `OverviewLayoutBlock(blockId, sortOrder, visible)`.
  - `OverviewLayoutPrefs(blocks)`.
  - `OverviewAiWidget(id, title, prompt, a2uiJson, enabled, sortOrder, createdAt, updatedAt, lastGeneratedAt, lastGenerationEngineId)`.
  - `OverviewRepository` with observable reads plus writes for replacing built-in layout and upserting/deleting/toggling widgets.
- Add Room persistence in `data/overview`:
  - `OverviewLayoutBlockEntity` keyed by block id.
  - `OverviewAiWidgetEntity` with generated long id.
  - DAO queries returning `Flow<List<...>>` and suspend write methods.
  - `OverviewRoomDatabase` version 1, Android/iOS factories, schema export enabled.
  - Repository implementation and mapper tests.
- Add `FakeOverviewRepository` in `core/testing`.
- Wire dependencies:
  - `data/overview/build.gradle.kts`.
  - `shared/src/commonMain/kotlin/com/dv/moneym/di/DataModules.kt`.
  - `shared/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt` database close handling for backups.
  - platform database factories wherever existing data DBs are created.
- Extend backup:
  - `BackupDto` gains `overviewLayout` and `overviewAiWidgets` with safe defaults.
  - DTO mappers for overview layout/widgets.
  - `BackupExporter` includes repository data.
  - `BackupImporter.applyFromJson` imports/merges overview layout/widgets.
  - `BackupRestorer.restore` replaces overview layout/widgets.

## Tests

- `data/overview` common tests for fake/data mapping/repository behavior where feasible.
- `core/testing` fake parity compile coverage.
- `data/backup` round-trip test includes overview layout and widgets.
- Run at minimum:
  - `./gradlew :data:overview:compileDebugKotlinAndroid`
  - `./gradlew :data:overview:testDebugUnitTest`
  - `./gradlew :data:backup:testDebugUnitTest`

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md`:
- Phase 1 status
- commands run and results
- commit hash after committing

Commit only Phase 1 changes.
