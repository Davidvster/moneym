# Phase 1 — `core/ai`: multi-engine abstraction

**Status:** ✅ Done — `tasks-genai:0.10.24`; module compiles (android+ios) + tests pass. iOS runner is holder-stub (Swift in Phase 7).
**Depends on:** none

## Goal
Turn the single-`AiEngine` design into a registry of engines and add a downloaded-model engine
(`LocalLlmAiEngine`) backed by a platform `LocalLlmRunner`. iOS runner delegates to a Swift-bridge
holder (stub until Phase 7).

## Tasks
1. `core/ai/src/commonMain/.../AiEngineId.kt` (new):
   `enum class AiEngineId { GEMINI_NANO, APPLE_INTELLIGENCE, LOCAL_LLM }`.
2. `AiEngine.kt`: add `val id: AiEngineId` to the interface.
3. `GeminiNanoAiEngine.kt` (androidMain): `override val id = AiEngineId.GEMINI_NANO`.
4. `IosFoundationModelsAiEngine.kt` (iosMain): `override val id = AiEngineId.APPLE_INTELLIGENCE`.
5. `AiEngineRegistry.kt` (commonMain, plain class):
   - `class AiEngineRegistry(private val engines: List<AiEngine>)`
   - `fun all(): List<AiEngine> = engines`
   - `fun byId(id: AiEngineId): AiEngine? = engines.firstOrNull { it.id == id }`
   - `suspend fun availabilities(): Map<AiEngineId, AiAvailability>` — map each engine to its
     `availability()` result (caller wraps on IO).
6. `LocalLlmRunner.kt` (commonMain, interface):
   ```kotlin
   interface LocalLlmRunner {
       suspend fun isModelLoaded(): Boolean
       suspend fun loadModel(path: String): Boolean
       fun streamReply(prompt: String): Flow<String>
   }
   ```
7. `LocalLlmAiEngine.kt` (commonMain):
   - `class LocalLlmAiEngine(private val runner: LocalLlmRunner, private val activeModelPath: suspend () -> String?)`
   - `id = LOCAL_LLM`, `supportsTools = false`.
   - `availability()`: `val p = activeModelPath() ?: return DOWNLOADABLE; return if (runner.loadModel(p)) AVAILABLE else UNAVAILABLE`.
   - `streamReply(messages, grounding)`: build prompt via `PromptBuilder.build(messages, grounding, SYSTEM_INSTRUCTION)`,
     ensure model loaded (load active path), then `runner.streamReply(prompt)`. Reuse the same
     `SYSTEM_INSTRUCTION` text as the other engines (extract to a shared const if convenient).
8. Android runner `core/ai/src/androidMain/.../AndroidLocalLlmRunner.kt` (new):
   wrap LiteRT-LM / MediaPipe `LlmInference`. `loadModel(path)` builds the engine from the file
   (cache the loaded instance keyed by path); `streamReply` maps the streaming generate callback into
   a `callbackFlow<String>` emitting token deltas. Guard all native calls in `runCatching`.
9. iOS runner `core/ai/src/iosMain/.../IosLocalLlmRunner.kt` + `IosLocalLlmBridge.kt` (new):
   ```kotlin
   interface IosLocalLlmBridge {
       fun isLoaded(): Boolean
       fun loadModel(path: String, onResult: (Boolean) -> Unit)
       fun streamReply(prompt: String, onChunk: (String) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit)
   }
   object IosLocalLlmBridgeHolder { var instance: IosLocalLlmBridge? = null }
   ```
   `IosLocalLlmRunner` delegates to `IosLocalLlmBridgeHolder.instance` (mirror
   `IosFoundationModelsAiEngine` callbackFlow + cumulative-delta logic). When holder is null →
   `isModelLoaded=false`, `loadModel=false`, `streamReply` closes immediately.
10. `core/ai/build.gradle.kts`: add LiteRT-LM/MediaPipe artifact to `androidMain.dependencies`.
11. `gradle/libs.versions.toml`: add the version + library entry (e.g. `mediapipe-tasks-genai` or
    LiteRT-LM artifact). Pick the current LiteRT-LM Android artifact; if unavailable in catalog mirror,
    fall back to `com.google.mediapipe:tasks-genai` and note it.

## Tests (`core/ai/src/commonTest`)
- `AiEngineRegistryTest`: `byId` resolves, `all` returns inserted order, `availabilities` maps ids.
- `LocalLlmAiEngineTest`: with a fake `LocalLlmRunner` —
  - no active path → `DOWNLOADABLE`.
  - active path + runner loads → `AVAILABLE`; streamReply emits runner deltas.
  - active path + runner fails load → `UNAVAILABLE`.

## Skills
`viewmodel-state` (n/a here), primarily plain Kotlin. Follow `testing` skill for test layout.

## Verify
`./gradlew :core:ai:compileDebugKotlinAndroid :core:ai:compileKotlinIosSimulatorArm64 :core:ai:testDebugUnitTest`

## Commit
`feat(core-ai): multi-engine registry + LocalLlm engine/runner (android impl, ios bridge stub)`
