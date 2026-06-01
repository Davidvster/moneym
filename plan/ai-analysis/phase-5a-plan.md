# Phase 5a — Analyze chat screen + nav entry + Koin + i18n

**Status:** IN PROGRESS

## Goal
Build the Analyze chat UI for `feature/aianalysis`, register it in Koin + as a modal nav entry, and add localized strings. (The Overview button that opens it comes in Phase 5b.)

## Reference files (mirror exactly)
- Modal screen + entry: `feature/categories/.../list/CategoryListScreen.kt` — `@Serializable data object CategoriesKey : ModalKey`, `fun EntryProviderScope<NavKey>.categoriesEntry(onBack, metadata) = entry<CategoriesKey>(metadata) { ... koinViewModel() }`. For a Key WITH params + `parametersOf`, mirror `feature/transactionEdit` (`transactionEditEntry` / `TransactionEditKey(id)`) and its FeatureModules `viewModel { params -> ... params.get() }` registration.
- core/ui components: `ScreenHeader` (modal title + back), `MmField`, `MmIconButton`, `MmCard`, `MmSegmented`, `Icon` + `imageVector`. Read them under `core/ui/src/commonMain/...`.
- Phase 4 VM: `feature/aianalysis/.../AnalyzeViewModel.kt`, `AnalyzeUiState.kt` (AnalyzeIntent). Constructor: `(year: Int, month: Int, engine, buildSnapshot, buildToolset, appSettings, dispatchers, clock)`.
- i18n layout: `feature/overview/src/commonMain/composeResources/values{,-de,-es,-it}/strings.xml`.

## Tasks

1. **`AnalyzeScreen.kt`** (`com.dv.moneym.feature.aianalysis`):
   - `@Serializable data class AnalyzeKey(val year: Int, val month: Int) : ModalKey` (import `ModalKey` from `core.navigation`).
   - `fun EntryProviderScope<NavKey>.analyzeEntry(onBack: () -> Unit, metadata: Map<String, Any> = emptyMap()) = entry<AnalyzeKey>(metadata = metadata) { key -> AnalyzeScreen(year = key.year, month = key.month, onBack = onBack) }`.
   - `AnalyzeScreen(year, month, onBack, viewModel: AnalyzeViewModel = koinViewModel { parametersOf(year, month) })` — collect state, render `AnalyzeContent(state, onIntent, onBack)`.
   - `AnalyzeContent`: `Column` with `ScreenHeader(title = analyze_title, onBack)`, the grounding-mode `MmSegmented` toggle (Snapshot / Tools), an optional fallback-notice row (when `showToolsFallbackNotice`), the messages `LazyColumn` (weight 1f) of message bubbles, suggested-prompt chips shown when `messages.isEmpty()`, and a bottom input bar (`MmField` bound to `state.input` → `InputChanged`, send `MmIconButton` → `SendMessage(state.input)`), plus a generating indicator when `isGenerating`. Keep UI dumb — all state from `state`, all actions via `onIntent`. Transient UI (LazyListState, auto-scroll-to-bottom) stays in the composable.

2. **`components/` composables** (split for readability):
   - `MessageBubble` (user vs assistant styling via `ChatRole`).
   - `SuggestedPrompts` — reads `stringArrayResource(Res.array.analyze_suggested_prompts)`, renders chips; onClick → `onIntent(AnalyzeIntent.SendMessage(text))`.
   - `AnalyzeInputBar`.
   Follow `ui-patterns` skill (theming via `MM`, previews with `MoneyMTheme`, no hardcoded user-visible literals).

3. **i18n** `feature/aianalysis/src/commonMain/composeResources/values/strings.xml` (+ `-de`, `-es`, `-it`):
   - `analyze_title`, `analyze_intro` (empty-state subtitle), `analyze_input_hint`, `analyze_send_cd`, `analyze_grounding_label`, `analyze_grounding_snapshot`, `analyze_grounding_tools`, `analyze_tools_fallback_notice`, `analyze_error`, `analyze_generating`.
   - `string-array analyze_suggested_prompts` with 4 items: "How much did I spend this month?", "What are my top spending categories?", "How does this month compare to last month?", "Any unusual or large expenses?" — translate all to de/es/it.

4. **Koin** `composeApp/.../di/FeatureModules.kt`:
   - Add `val featureAianalysisModule = module { single { BuildFinanceSnapshotUseCase(get(), get(), get(), get(), get()) }; single { BuildFinanceToolsetUseCase(get(), get(), get(), get(), get()) }; viewModel { params -> AnalyzeViewModel(year = params.get(), month = params.get(), engine = get(), buildSnapshot = get(), buildToolset = get(), appSettings = get(), dispatchers = get(), clock = get()) } }` (match the actual Phase-4 constructor arities — read the use case + VM constructors and pass the right `get()` count).
   - Add imports.

5. **`composeApp/.../di/AppModules.kt`**: add `featureAianalysisModule` to the `appModules` list (after `featureOverviewModule`).

6. **`composeApp/build.gradle.kts`**: add `implementation(projects.feature.aianalysis)` (commonMain, next to `projects.feature.overview`).

7. **`MainNav.kt`**: import `AnalyzeKey` + `analyzeEntry`; register `analyzeEntry(onBack = { tabBackStack.removeLast() }, metadata = modalTransitionMeta)` in the `entryProvider {}` block (the Overview→Analyze push is added in 5b).

## Constraints
- VM + use cases already public. `koinViewModel { parametersOf(...) }` needs `import org.koin.core.parameter.parametersOf`.
- No user-visible literal in Kotlin — everything via `stringResource`/`stringArrayResource`.
- Kotlin conventions; no TODO placeholders.

## Verify
```bash
cd /Users/davidvalic/Developer/MoneyM/.claude/worktrees/ai-analysis
./gradlew :feature:aianalysis:compileDebugKotlinAndroid
./gradlew :feature:aianalysis:testDebugUnitTest
./gradlew :composeApp:assembleDebug
```
All must pass (Koin graph resolves AnalyzeViewModel + use cases; app assembles with the new module + nav entry). Report files + results.
