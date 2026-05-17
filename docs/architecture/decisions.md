# Architectural decisions

A short, dated log of decisions made and decisions deliberately deferred. Append new entries; don't rewrite history. If a decision is reversed, add a new entry that references the old one.

Format: short title → context → choice → consequences.

---

## ADR-001 — Target only Android and iOS for v1

**Date**: 2026-05-10
**Status**: Decided
**Context**: Base project targeted Android, iOS, JS, Wasm. Local-first storage (SQLDelight), biometrics, and OS-level secure storage map poorly to browsers.
**Decision**: Drop `js` and `wasmJs` targets. Skip Desktop (JVM). Android + iOS only.
**Consequences**: Smaller surface area, faster builds, simpler dependency picks. Re-adding web later is non-trivial because SQLDelight web drivers are limited and `core:security` needs a browser story.

## ADR-002 — Feature-first module layout

**Date**: 2026-05-10
**Status**: Decided
**Context**: Could organize as layer-first (`:ui`, `:domain`, `:data`) or feature-first. User preference is feature-first.
**Decision**: Feature-first. Each feature is a module with internal layer discipline (UI / presentation / domain packages). Data and core concerns are modular along separate axes.
**Consequences**: Faster incremental builds, clearer ownership. Cost: more `build.gradle.kts` files to maintain. Convention plugins can offset this if it becomes a problem (revisit after Phase 3).

## ADR-003 — No standalone `:domain` module in v1

**Date**: 2026-05-10
**Status**: Decided
**Context**: Whether to extract use cases to a shared `:domain` module or keep them feature-local.
**Decision**: Keep use cases feature-local under `feature/<name>/.../domain/`. Promote a use case to `core:*` only when a second feature actually needs it.
**Consequences**: Less upfront ceremony. Risk: when promotion is needed, refactor pain. Acceptable — promotion is cheap with type-safe project accessors.

## ADR-004 — Jetpack Navigation Compose (multiplatform)

**Date**: 2026-05-11
**Status**: Decided
**Context**: Three credible options for KMP navigation: Jetpack Nav (multiplatform), Decompose, Voyager.
**Decision**: Jetpack Navigation Compose with type-safe routes via `kotlinx-serialization`.
**Consequences**: Familiar to Android devs, official, lowest friction. Loses some of Decompose's state-restoration rigor — acceptable for an app this size.

## ADR-005 — Koin for DI

**Date**: 2026-05-11
**Status**: Decided
**Context**: Koin (runtime, DSL, lightweight) vs Kotlin-Inject (compile-time, KSP).
**Decision**: Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`).
**Consequences**: Less boilerplate, slower startup than compile-time DI, runtime errors instead of compile errors for missing bindings. Mitigated by exhaustive DI tests in `composeApp:commonTest` that resolve every ViewModel.

## ADR-006 — Multi-currency without conversion in v1

**Date**: 2026-05-11
**Status**: Decided
**Context**: User wants multi-currency support but no FX conversion now.
**Decision**: Every transaction carries an explicit `currency` field (ISO-4217). The overview/analytics shows currencies separately when more than one is present in the range. No conversion engine, no FX rates.
**Consequences**: Schema is future-proof for FX. UI must handle mixed-currency aggregation (likely by grouping per currency, not summing across). Adding conversion later is additive.

## ADR-007 — Mockative instead of MockK

**Date**: 2026-05-11
**Status**: Decided
**Context**: MockK does not support iOS native targets. Tests need to run in `commonTest`.
**Decision**: Use Mockative (KSP-based, works on all KMP targets) as the mocking escape hatch. Prefer hand-written fakes in `core:testing` for anything used by multiple tests.
**Consequences**: Adds a KSP step. Mockative's API is slightly different from MockK — write the rules once, share via the `testing` skill.

## ADR-008 — Compose Charts for v1, isolated behind a wrapper

**Date**: 2026-05-11
**Status**: Decided
**Context**: KMP charting libraries are immature. Options: Koalaplot, Compose Charts (ehsannarmani), or hand-rolled Canvas.
**Decision**: Compose Charts in `feature:overview`, behind thin wrapper composables (`SpendingDonut`, `IncomeExpenseBar`, `TrendLine`). The library is **only** imported by `feature:overview`.
**Consequences**: If the library disappoints, only one module changes. Wrapper limits API drift.

## ADR-009 — No database encryption in v1

**Date**: 2026-05-11
**Status**: Decided
**Context**: PIN protects the app, but the on-disk SQLite DB is unencrypted. Both Android and iOS apply OS-level sandbox encryption when the device has a passcode.
**Decision**: Accept OS-level encryption for v1. Do not ship SQLCipher.
**Consequences**: A user without a device passcode is vulnerable to an attacker with physical access plus rooted/jailbroken extraction tools. Stated threat-model boundary in `security.md`. Future work: SQLCipher with key derived from PIN unlocks at app start.

## ADR-010 — PBKDF2-HMAC-SHA256 for PIN hashing (Argon2id deferred)

**Date**: 2026-05-11
**Status**: Decided
**Context**: Argon2id is the modern recommendation, but shipping it on iOS adds binary-size and build complexity (Swift interop or a native binary). The user has no strong opinion and asked for the simpler path.
**Decision**: Use **PBKDF2-HMAC-SHA256 with 600,000 iterations** (current OWASP recommendation) and a 16-byte random salt. Hash params (algorithm name, iteration count, salt) are stored alongside the digest so we can migrate to Argon2id later without forcing a re-PIN.
**Consequences**: One small pure-Kotlin implementation; no native binary, no `expect/actual` on the algorithm. Marginal security tradeoff against Argon2id is acceptable for a 4–6 digit PIN backed by a rate-limited unlock flow (attempt counter + exponential backoff per `security.md`). A future migration path is preserved: when reading a stored hash, dispatch on the algorithm field; on next successful PIN entry, re-hash with the new algorithm.

## ADR-011 — No Ktor in v1

**Date**: 2026-05-11
**Status**: Decided
**Context**: User said "prefer Ktor for networking". v1 has no remote calls.
**Decision**: Don't add Ktor in v1. Pin a version in `libs.versions.toml` so it's ready when Phase 6 (sync) starts.
**Consequences**: Smaller dependency surface. When sync arrives, we adopt Ktor in a new `data:sync` module without disturbing existing modules.

## ADR-012 — `core:model` over `core:domain` or `shared:model`

**Date**: 2026-05-11
**Status**: Decided
**Context**: User asked for a naming suggestion for the shared models module.
**Decision**: `core:model`. Singular, terse, matches the Now in Android convention and reads cleanly in `projects.core.model`.
**Consequences**: Leaves `core:domain` free for the day (if ever) we extract shared use cases.

## ADR-013 — Kermit for logging

**Date**: 2026-05-11
**Status**: Decided
**Context**: We need structured logging that works on Android and iOS, and we want crash reporting hookable later without rewriting call sites.
**Decision**: Use **Kermit** (`co.touchlab:kermit`) as the single logging facade across `commonMain`. Each module that logs gets a tagged logger via `Logger.withTag("FeatureX")`.
**Consequences**: One log API everywhere. Built-in Android (logcat) and iOS (NSLog) writers cover v1. Future crash reporting plugs in as a custom `LogWriter` (per ADR-014) without touching call sites.

## ADR-014 — No crash reporting in v1

**Date**: 2026-05-11
**Status**: Decided
**Context**: Crash reporting (Sentry, Firebase Crashlytics) has a privacy cost and pulls in a non-trivial third-party SDK. Local-first app, small user base initially.
**Decision**: Ship without crash reporting. Use Kermit for in-app logs only. Revisit if we have meaningful user feedback we can't act on.
**Consequences**: Bugs that crash in production go unreported until a user tells us. Mitigated by: comprehensive tests, ANR/strict-mode in dev, and the fact that Kermit's `LogWriter` extension point lets us add a reporter later without changing log call sites.

## ADR-015 — Project name is "MoneyM"; align directory to match

**Date**: 2026-05-11
**Status**: Decided
**Context**: Directory was `MoneyM2`, Gradle `rootProject.name` was `MoneyM`. User confirmed the project name (and user-facing display name) is **MoneyM**, and authorized renaming the directory to match.
**Decision**: Keep `rootProject.name = "MoneyM"`. The project directory `MoneyM2` is renamed to `MoneyM` outside the agent session. Android `applicationId` / namespace stays `com.dv.moneym`.
**Consequences**: Internal naming converges on "MoneyM". After the directory rename, all paths use `…/Developer/MoneyM/`. Until that happens, agent tool calls reference `…/Developer/MoneyM2/` — coordinate the rename across sessions to avoid path breakage.

## ADR-016 — Screens must not inject repositories directly

**Date**: 2026-05-17
**Status**: Decided
**Context**: `WalletManageScreen` originally called `koinInject<AccountRepository>()` and `koinInject<AppSettingsRepository>()` directly inside the composable, then used `rememberCoroutineScope` to call suspend functions. This couples the UI layer to the data layer, makes the screen untestable in isolation, and scatters business logic across composables.
**Decision**: All repository access and coroutine work must go through a `ViewModel`. Screens receive a `ViewModel` (via `koinViewModel()`) and communicate through a typed intent sealed interface. The `ViewModel` owns state as a `StateFlow<UiState>`. Screens only call `viewModel.onIntent(...)` — never `repository.someMethod(...)`.

**Rule**: A composable screen function must not:
- inject a repository with `koinInject()`
- call `rememberCoroutineScope()` to invoke suspend repository methods
- hold repository references of any kind

**Example**: `WalletManageScreen` was refactored in this commit. It previously injected `AccountRepository` and `AppSettingsRepository` directly; it now delegates entirely to `WalletManageViewModel`.
**Consequences**: Every screen is testable by supplying a fake `ViewModel` or a fake `StateFlow`. DI wiring is explicit in `FeatureModules.kt`. Slight increase in boilerplate (new `*UiState` + `*Intent` + `*ViewModel` files per screen), offset by strong architectural consistency.

---

## Open questions (no ADR yet)

- **iOS distribution**: SwiftPM, CocoaPods, or pre-built XCFramework. Currently the iOS project consumes the Kotlin framework directly — fine for development; decide before TestFlight.
- **Detekt / ktlint**: when, not if. Defer until we have 5+ modules and feel the drift.
- **`data:sync` module shape**: separate module or per-data-module mixin. Decide when sync starts (Phase 6+).
- **Theme switcher position in settings**: top-level vs nested under "Appearance". Designer call, not architectural.
