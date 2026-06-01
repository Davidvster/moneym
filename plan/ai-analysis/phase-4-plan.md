# Phase 4 — `feature/aianalysis`: grounding use cases + ViewModel + settings toggle + tests

**Status:** DONE (commit ca43545). 9/9 tests pass; android+ios compile green. Deterministic formatMinor used (core/common formatNumber was locale-dependent + abs-stripped sign). AnalyzeViewModel constructor: (year, month, engine, buildSnapshot, buildToolset, appSettings, dispatchers, clock).

## Goal
Create the `feature/aianalysis` module with the read-only grounding use cases, the grounding-mode persistence, and the `AnalyzeViewModel` (chat orchestration) + unit tests. **No UI yet** (Compose screen comes in Phase 5) — but the module's `build.gradle.kts` includes Compose deps so Phase 5 only adds UI files.

## Decisions locked for this phase
- **Period scope = primitives**: feature cannot depend on `feature/overview`, so do NOT use `OverviewPeriod`. The VM takes `year: Int, month: Int` (Koin params). Snapshot = selected month + previous 3 months aggregates.
- **Grounding-mode persistence via low-level `AppSettings`** (NOT `AppSettingsRepository`): avoids coupling `core/datastore`→`core/ai`. Add `const val AI_GROUNDING_MODE = "pref.ai_grounding_mode"` to `core/datastore/.../PrefKeys.kt`. VM reads/writes via `AppSettings.getString/putString` and maps to `AiGroundingMode` (default `SNAPSHOT`). No `FakeAppSettingsRepository` change needed; tests use existing `FakeAppSettings`.
- Both engines currently report `supportsTools = false`, so TOOLS mode **always falls back to SNAPSHOT + notice** today; the toolset is still built (real, repo-backed) and unit-tested so it activates when an engine supports tools later.

## Tasks

1. **`settings.gradle.kts`**: add `include(":feature:aianalysis")` in the feature group.

2. **`feature/aianalysis/build.gradle.kts`** — mirror `feature/overview/build.gradle.kts`:
   - plugins: kotlinMultiplatform, androidLibrary, composeMultiplatform, composeCompiler, kotlinSerialization.
   - framework baseName `FeatureAianalysis`, namespace `com.dv.moneym.feature.aianalysis`.
   - commonMain deps: compose runtime/foundation/material3/ui/components.resources/uiToolingPreview, lifecycle viewmodelCompose + runtimeCompose, koin compose + koin.compose.viewmodel, kotlinx.datetime, navigation3.runtime, `projects.core.ai`, `projects.core.model`, `projects.core.common`, `projects.core.datastore`, `projects.core.designsystem`, `projects.core.ui`, `projects.core.navigation`, `projects.data.transactions`, `projects.data.accounts`, `projects.data.categories`, `projects.data.budgets`.
   - commonTest deps: `libs.kotlin.test`, `projects.core.testing`, coroutines-test + Turbine if used by the repo's testing skill (check `gradle/libs.versions.toml` aliases; mirror an existing `*ViewModelTest`'s deps).

3. **`core/datastore/.../PrefKeys.kt`**: add `const val AI_GROUNDING_MODE = "pref.ai_grounding_mode"`.

4. **`usecase/BuildFinanceSnapshotUseCase.kt`** (`com.dv.moneym.feature.aianalysis.usecase`):
   - Constructor: `TransactionRepository`, `AccountRepository`, `CategoryRepository`, `BudgetRepository`, `AppClock`.
   - `suspend operator fun invoke(year: Int, month: Int): String`.
   - Read-only only: `transactionRepository.observeByMonth(y,m).first()` for the selected month and each of the previous 3 months; `accountRepository.observeAll().first()`; `categoryRepository.observeActive().first()`; budgets via `budgetRepository` read method.
   - Build a compact, deterministic text summary: currency, selected-month income/expense totals, top per-category expense totals (name + amount), budget status (spent/limit) if any, and a one-line-per-month recent-history trend (prev 3 months expense totals). Reuse any existing money-formatting util in `core/model`/`core/common` if present (search for a formatter); else format `minorUnits/100.0` with the account currency code.
   - Keep aggregation minimal and local (cannot import overview use cases).

5. **`usecase/BuildFinanceToolsetUseCase.kt`**:
   - Constructor: same repos + `AppClock`.
   - `operator fun invoke(year: Int, month: Int): List<AiTool>` returning real read-only tools, e.g. `totals(period)`, `spendingByCategory(period)`, `topExpenses(n)`, `comparePreviousMonths()`. Each `AiTool.invoke` lambda calls only read methods and returns a short string. (Used when an engine supports tools; today it's built but fallback applies.)

6. **`AnalyzeUiState.kt`** (+ `AnalyzeIntent` in same file):
   ```kotlin
   data class AnalyzeUiState(
       val available: Boolean = true,
       val messages: List<ChatMessage> = emptyList(),
       val input: String = "",
       val isGenerating: Boolean = false,
       val groundingMode: AiGroundingMode = AiGroundingMode.SNAPSHOT,
       val showToolsFallbackNotice: Boolean = false,
       val errorKey: String? = null,
   )
   sealed interface AnalyzeIntent {
       data class InputChanged(val text: String) : AnalyzeIntent
       data class SendMessage(val text: String) : AnalyzeIntent   // also used by suggested-prompt chips
       data class GroundingModeChanged(val mode: AiGroundingMode) : AnalyzeIntent
       data object DismissFallbackNotice : AnalyzeIntent
       data object ClearError : AnalyzeIntent
   }
   ```
   (Suggested-prompt strings are i18n resources owned by the UI in Phase 5; chips dispatch `SendMessage(text)`. VM does not hardcode prompt text.)

7. **`AnalyzeViewModel.kt`** — single public `fun onIntent(intent: AnalyzeIntent)`; `internal val state: StateFlow<AnalyzeUiState>`. Constructor (Koin params + injected): `year: Int`, `month: Int`, `engine: AiEngine`, `buildSnapshot: BuildFinanceSnapshotUseCase`, `buildToolset: BuildFinanceToolsetUseCase`, `appSettings: AppSettings`, `dispatchers: DispatcherProvider`, `clock: AppClock` (if needed).
   - On init: load persisted grounding mode from `appSettings`; set `available` from `engine.availability() == AVAILABLE`.
   - `SendMessage(text)`: ignore if blank or `isGenerating`. Append `ChatMessage(USER, text)`, clear input, set `isGenerating`. Resolve grounding: if `groundingMode == TOOLS && engine.supportsTools` → `Grounding.Tools(buildToolset(year,month))`; else `Grounding.Snapshot(buildSnapshot(year,month))` and if mode was TOOLS set `showToolsFallbackNotice = true`. Append an empty `ChatMessage(ASSISTANT, "")`, then collect `engine.streamReply(messages, grounding)` appending each delta to that assistant message. On completion clear `isGenerating`; on error set `errorKey` + clear generating.
   - `GroundingModeChanged(mode)`: update state + persist via `appSettings.putString(PrefKeys.AI_GROUNDING_MODE, mode.name)`.
   - Use `viewModelScope` + `dispatchers`. Follow `viewmodel-state` skill.

8. **Koin** — defer registration to Phase 5 (module not yet on `appModules`). Do NOT add to FeatureModules/AppModules here (keeps this phase build-isolated like Phase 1). Confirm module compiles standalone.

9. **commonTest**:
   - `BuildFinanceSnapshotUseCaseTest` — using `FakeTransactionRepository`, `FakeAccountRepository`, `FakeCategoryRepository`, `FakeBudgetRepository`, `FixedClock`: seed transactions across the selected + prior months, assert the snapshot contains the right totals, top categories, and recent-history lines; assert ONLY read methods are exercised (no writes).
   - `AnalyzeViewModelTest` — with a **fake `AiEngine`** (test double emitting scripted deltas; `supportsTools` toggdleable) + `FakeAppSettings`: assert SendMessage appends user+assistant and streams deltas into the assistant message; grounding-mode toggle persists to `AppSettings`; TOOLS mode with `supportsTools=false` sets `showToolsFallbackNotice`; blank/duplicate-while-generating ignored. Mirror an existing `*ViewModelTest` (e.g. under `feature/overview/src/commonTest` or `feature/transactionEdit`) for Turbine/coroutines-test style. Follow `testing` skill.

## Constraints
- ViewModel + use cases must be **public** (Koin cross-module in Phase 5).
- Intent-only VM surface; immutable StateFlow state; no domain state in (future) composables.
- Read-only data access exclusively.
- Kotlin conventions; no TODO placeholders.

## Verify
```bash
cd /Users/davidvalic/Developer/MoneyM/.claude/worktrees/ai-analysis
./gradlew :feature:aianalysis:compileDebugKotlinAndroid
./gradlew :feature:aianalysis:testDebugUnitTest
./gradlew :feature:aianalysis:compileKotlinIosSimulatorArm64
```
All must pass. Report files + test results.
