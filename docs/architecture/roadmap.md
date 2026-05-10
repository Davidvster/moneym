# Implementation roadmap

Eight phases, each ending in a runnable app with meaningful new capability. Don't skip phases — each builds on what the previous landed.

Acceptance criteria for every phase: builds clean on Android and iOS, `./gradlew allTests` passes, manual run looks correct in both light and dark mode.

---

## Phase 0 — Approval gate (you are here)

**Goal**: alignment on architecture, skills, libraries, module structure, and roadmap. **No production code.**

Deliverables (this phase):
- Architecture docs under `docs/architecture/` ✅
- Skills under `.claude/skills/` ✅
- Web targets and source folders removed ✅
- This roadmap ✅

Exit: you approve. Then we move to Phase 1.

---

## Phase 1 — Skeleton modules and DI bootstrap

Build the multi-module skeleton with no features, just wiring.

**Tasks**:
1. Create empty modules per the structure in `overview.md`. Each gets a `build.gradle.kts`, source dirs, and a Koin module that produces nothing yet.
2. Update `libs.versions.toml` with all libraries from `libraries.md`. Don't wire libs that aren't used yet — but version-pin them.
3. Configure `composeApp` to start Koin and host a placeholder `App()` composable that renders "Hello, MoneyM" using `MoneyMTheme`.
4. Wire `core:designsystem` (theme tokens, typography, spacing) and `core:ui` (one or two starter components).
5. Wire `core:common` (DispatcherProvider, Clock).
6. Stand up `core:database` with the SQLDelight plugin and driver factories (Android `AndroidSqliteDriver`, iOS `NativeSqliteDriver`).
7. Stand up `core:datastore` with a `multiplatform-settings` wrapper.
8. Add `core:testing` with the `runTestWithDispatchers` helper and a `Fake` interface convention.

**Verification**: `./gradlew :composeApp:assembleDebug` and iOS framework build both succeed. App opens to "Hello, MoneyM" on both platforms.

---

## Phase 2 — Data layer: categories, accounts, transactions

Now there's persistence. Still no real features, but you can read/write through repositories from tests.

**Tasks**:
1. `data:categories` — schema, repository, seed UseCase that inserts defaults on first run.
2. `data:accounts` — schema, repository, seed UseCase for the default "Main" account.
3. `data:transactions` — schema with the columns from `data-model.md`, repository, queries for: observe-all, observe-by-month, observe-filtered, get-by-id, upsert, delete.
4. Each repository has a fake in `core:testing`.
5. Repository tests in `commonTest` using fake DataSources.
6. SQLDelight schema migration test scaffold in `androidUnitTest`.

**Verification**: All repository tests pass. `composeApp` boots, seed runs, you can confirm DB has 11 categories and 1 account via a debug log.

---

## Phase 3 — Transactions feature

The core experience.

**Tasks**:
1. `feature:transactions` — daily-grouped list, monthly toggle, filter by category and type. ViewModel + UiState + Intents per the `viewmodel-state` skill.
2. `feature:transactionEdit` — add screen, edit screen (reuses add), delete confirmation. Amount input handles minor-unit math.
3. `core:navigation` graph host. Both features expose their `NavGraphBuilder` extensions.
4. Wire bottom navigation in `composeApp` (Transactions • Overview • Settings — Overview and Settings still empty stubs).
5. UseCases as needed: `GetTransactionsByMonth`, `GetTransactionsFiltered`, `UpsertTransaction`, `DeleteTransaction`.
6. ViewModel tests + at least one Compose UI test for the list screen on Android.

**Verification**: You can add, edit, delete, and filter transactions on Android and iOS. Light and dark mode look correct. All four locales (en, es, it, de) have strings present (translation quality not required yet — see Phase 8).

---

## Phase 4 — Security: PIN and biometrics

**Tasks**:
1. `core:security` — `SecureStore` interface + Android/iOS actuals, `PinHasher` (PBKDF2-HMAC-SHA256, 600k iterations — ADR-010), `BiometricAuthenticator`.
2. `feature:security` — PIN setup screen, PIN unlock screen, biometric prompt.
3. App-lock controller in `composeApp` — observes lifecycle, routes to unlock screen when needed.
4. `feature:settings` — Security section: enable PIN, change PIN, enable biometrics, set background-lock duration.
5. Recent-apps blur (FLAG_SECURE on Android, snapshot overlay on iOS).
6. Tests: `PinHasher` round-trip, attempt-counter backoff logic, lock-on-background timing.

**Verification**: Enable PIN, restart app, enter PIN. Enable biometric, lock, unlock with biometric (fall back to PIN). Background the app for > N seconds, foreground, get locked.

---

## Phase 5 — Categories management and onboarding

**Tasks**:
1. `feature:categories` — list, create, edit (name, icon, color), archive. Icon picker, color picker (12 preset hues + custom hex).
2. `feature:onboarding` — first-launch flow: pick currency → optional PIN setup → optional biometrics → done. Sets `pref.onboarding_completed = true`.
3. Hook onboarding into `composeApp` root: if `onboarding_completed = false`, route to onboarding before main graph.

**Verification**: Fresh install routes to onboarding; second launch goes straight to transactions list. Categories CRUD round-trips and is reflected in transaction picker.

---

## Phase 6 — Overview / analytics

**Tasks**:
1. `feature:overview` — sections for daily/monthly/yearly summaries; spending by category (donut); income vs expense (bar); trend over time (line).
2. UseCases: `SummariseByDay`, `SummariseByMonth`, `SummariseByCategory`, `Trend`.
3. Charts via `compose-charts`, isolated behind a thin `Chart…` wrapper composable per type so we can swap library.
4. Empty-state and short-data handling (≤ 1 day of data → friendly message, no broken charts).

**Verification**: Charts render with realistic test data. Color usage stays restricted to category colors. Both themes look clean.

---

## Phase 7 — Import / export

**Tasks**:
1. `data:backup` — JSON exporter/importer per `import-export.md`. CSV exporter/importer.
2. Validation layer and `ImportPreview` UI in `feature:settings`.
3. Platform file pickers via `expect/actual` in `feature:settings` (SAF on Android, `UIDocumentPicker` on iOS).
4. Round-trip test: export → wipe DB → import → state matches.

**Verification**: Export JSON, edit one transaction outside the app, import → preview shows 1 update. Export CSV, open in spreadsheet, confirm shape.

---

## Phase 8 — Polish, localization quality, release prep

**Tasks**:
1. Final localization pass for es/it/de (translation quality, plurals, currency formatting per locale).
2. Empty states, error states, loading states swept for every screen.
3. Accessibility audit: TalkBack on Android, VoiceOver on iOS, dynamic font sizing, contrast.
4. Performance pass: profile transaction list with 5,000+ rows; profile overview charts with 12 months × 30 transactions/day.
5. App icons, launch screens, store listing assets.
6. Crash reporting decision (not yet committed — see `decisions.md`).

**Verification**: Release-candidate build, installed on real devices, dogfooded for a week.

---

## Future (post-v1, not roadmapped here)

- Recurring transactions (the `recurrence_rule` column waits for this).
- Cloud backup (Google Drive first; `data:sync` module slots in).
- Multi-currency with FX (price in EUR, expense in USD, show both).
- Budgets and limits per category.
- Receipt attachments.

These are deliberately not roadmapped — each will get its own design pass when prioritized.
