# Phase 6 — Koin wiring + navigation

**Status:** ✅ Done — assembleDebug PASS, composeApp iOS compile PASS, full testDebugUnitTest PASS. Added `coreAiModule` (LocalLlmAiEngine + AiEngineRegistry(getAll())), `dataLlmModelsModule`, `featureAiModelsModule`; LocalLlmRunner in both platform modules; AnalyzeViewModel factory now `registry=get()`; MainNav registers `aiModelsEntry` + passes `onManageModels`. Also added composeApp gradle deps on `data:llmmodels` + `feature:aimodels` (were missing). HF token read from SecureStore `"hf_token"`.
**Depends on:** Phases 1–5

## Goal
Wire everything into the app: registry, local engine, repository/downloader, new feature module,
navigation, prefs.

## Tasks
1. `core/datastore` `PrefKeys`: add `AI_ENGINE_ID = "pref.ai_engine_id"` (and confirm
   `AI_ACTIVE_MODEL_ID` from Phase 3 exists).
2. `composeApp/.../di/DataModules.kt`:
   - `dataLlmModelsModule`: `single { llmHttpClient() }`, `single { LlmModelDownloader(get(), get()) }`,
     `single<LlmModelRepository> { DefaultLlmModelRepository(get(), get(), get(), get()) }` (catalog,
     downloader, dbPlatform, appSettings/secureStore as designed).
   - In `coreCommonModule` (or `coreAiModule`): register
     `single<AiEngine> { LocalLlmAiEngine(get(), activeModelPath = { get<LlmModelRepository>().activeModelPath() }) }`
     and `single { AiEngineRegistry(getAll()) }`. Existing platform engines already bound as
     `AiEngine` → `getAll()` collects all three.
   - Add `dataLlmModelsModule` to the app's module list (wherever modules are aggregated).
3. Platform modules:
   - `AndroidPlatformModule.kt`: `single<LocalLlmRunner> { AndroidLocalLlmRunner(get()) }` (needs
     Context for asset/cache if required).
   - `IosPlatformModule.kt`: `single<LocalLlmRunner> { IosLocalLlmRunner() }`.
4. `composeApp/.../di/FeatureModules.kt`:
   - Change `AnalyzeViewModel` factory: inject `registry = get()` instead of `engine = get()`.
   - Add `featureAiModelsModule { viewModel { AiModelsViewModel(get(), ...) } }`; add to module list.
5. `composeApp/.../MainNav.kt`:
   - import `AiModelsKey`, `aiModelsEntry`.
   - register `aiModelsEntry(onBack = { tabBackStack.removeLast() }, metadata = modalTransitionMeta)`.
   - pass `onManageModels = { tabBackStack.push(AiModelsKey) }` into `analyzeEntry(...)`.

## Tests
No new unit tests (wiring). Covered by build + earlier phase tests.

## Verify
- `./gradlew :composeApp:assembleDebug`
- `./gradlew testDebugUnitTest` (full)

## Commit
`feat(app): wire AiEngineRegistry, LocalLlm engine, llmmodels repo + aimodels nav`
