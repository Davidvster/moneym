# fixes-1 status

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | VM-state refactor (Analyze + SyncSettings) | ✅ done (9 aianalysis + 10 sync tests green) |
| 2 | Mapper tests (6) | ✅ done (75 tests; +fixed 2 pre-existing budgets failures) |
| 3 | Repo-impl tests (5, fake DS) | ✅ done (62 tests, no prod edits) |
| 4 | Datasource tests (9, Room in-memory) — RISK | ⛔ blocked/skipped (see note) |
| 5 | Feature VM tests (22) | ☐ pending |
| 6 | UseCase tests (12) | ☐ pending |
| 7 | Previews (10 core:ui + ~27 screens) | ☐ pending |
| 8 | Build verify Android + iOS | ☐ pending |

## Phase 4 blocker note
Direct in-memory Room tests are not feasible from `commonTest`. On the Android
unit-test target (`testDebugUnitTest`) Room 2.7.2 resolves the Android-specific
`Room.inMemoryDatabaseBuilder(context, klass)` overload, which requires an Android
`Context`. The reified no-arg KMP builder `inMemoryDatabaseBuilder<T>()` is not
available on the Android target, so the DB cannot be constructed without
Robolectric (a real `Context`). Per plan, the feasibility gate was attempted once
on `data:accounts`, reverted, build kept green.

Result: the 5 Room datasource impls (`SqlDelightAccountDataSource`,
`RoomBudgetDataSource`, `SqlDelightCategoryDataSource`, `SqlDelightTransactionDataSource`,
`SqlDelightPaymentModeDataSource`) have no direct unit tests. They are exercised
indirectly: the repo layer above them is fully covered (Phase 3), and the
entity↔domain mappers they rely on are covered (Phase 2). Future option: a
Robolectric-based `androidUnitTest` source set, or native-only `iosSimulatorArm64Test`
where the reified builder resolves.

Plan: `~/.claude/plans/ultra-1-i-see-splendid-deer.md`
