# Phase 0 — Approval gate

**Status**: ✅ Done

Goal: alignment on architecture, skills, libraries, module structure, roadmap. No production feature code.

## Steps

- [x] **0.1** Scan base project and surface clarifying questions
- [x] **0.2** Remove `js` and `wasmJs` targets from `composeApp/build.gradle.kts` (and the orphan `ExperimentalWasmDsl` import)
- [x] **0.3** Delete leftover web source folders (`jsMain/`, `wasmJsMain/`, `webMain/`)
- [x] **0.4** Write 5 skills under `.claude/skills/`:
  - `ui-patterns`
  - `viewmodel-state`
  - `testing`
  - `feature-module`
  - `data-layer`
- [x] **0.5** Write architecture docs under `docs/architecture/`:
  - `README.md`
  - `overview.md`
  - `data-model.md`
  - `security.md`
  - `theming.md`
  - `import-export.md`
  - `libraries.md`
  - `roadmap.md`
  - `decisions.md`
- [x] **0.6** Expand `gradle/libs.versions.toml` with all v1 library and plugin entries
- [x] **0.7** Resolve open ADRs:
  - ADR-010 → PBKDF2-HMAC-SHA256 600k iterations
  - ADR-013 → Kermit for logging
  - ADR-014 → no crash reporting in v1
  - ADR-015 → project name stays "MoneyM"; directory renamed `MoneyM2` → `MoneyM` outside session
- [x] **0.8** User approval to proceed

## Outcome

- Skills exist and cover the day-to-day patterns.
- Architecture is documented and reviewable.
- Library catalog is complete.
- Next phase is ready to start.
