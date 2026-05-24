# fixes-1 Status

Tracks the 6-phase quality refactor described in `/Users/davidvalic/.claude/plans/in-the-transaction-list-zesty-wall.md`.

| Phase | State | Date | Commit | Notes |
|------:|-------|------|--------|-------|
| 1 | done | 2026-05-24 | 8e91da5 | Seed/UiState/xLabels hardcoded values removed; null-safe UiState; fakes fixed; OverviewPageViewModel opened |
| 2 | done | 2026-05-24 | 08e8ef9 | All 11 VMs intent-only; new Intent sealed for Settings/Currency/Language/TxListDisplay |
| 3 | done | 2026-05-24 | 77c0603 | Lifted 6 screens' domain remember-state into VM + Intent; validation moved back to VM |
| 4 | done | 2026-05-24 | b0697fb | 6 use cases extracted; big composables split; OverviewPageVM 435->226 |
| 5 | done | 2026-05-24 | 3be8a33 | Fixed 3 broken tests + 1 new usecase test; testDebugUnitTest green; broad coverage deferred |
| 6 | pending |  |  | Android + iOS final build verify |
