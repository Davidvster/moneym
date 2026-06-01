# Phase 2 — Android Gemini Nano engine + wiring

**Status:** IN PROGRESS

## Goal
Implement `AiEngine` on Android using the **ML Kit GenAI Prompt API** (Gemini Nano via AICore), and wire it into the Android Koin platform module.

## Verified API facts (ML Kit GenAI Prompt API)
- Dependency: `com.google.mlkit:genai-prompt:1.0.0-beta2`
- Client: `val model = Generation.getClient()` (package `com.google.mlkit.genai.prompt` / `...generativeai` — resolve exact import at compile time; if `getClient` requires a `Context` or `GenerationConfig` in this version, pass it from the engine constructor).
- Availability: `model.checkStatus()` → `FeatureStatus` with values `AVAILABLE`, `DOWNLOADABLE`, `DOWNLOADING`, `UNAVAILABLE` (it may return an `Int` you compare against `FeatureStatus.*` constants — adapt to actual signature; it is a suspend call).
- Download (if `DOWNLOADABLE`): `model.download().collect { DownloadStatus.* }` — NOT triggered automatically here (see availability policy below).
- Streaming inference: `model.generateContentStream(prompt).collect { chunk -> chunk.candidates[0].text }` returns a Flow; each chunk exposes incremental text.

## Tasks

1. **`gradle/libs.versions.toml`**
   - Add version: `mlkitGenaiPrompt = "1.0.0-beta2"` under `[versions]`.
   - Add library under `[libraries]`: `mlkit-genai-prompt = { group = "com.google.mlkit", name = "genai-prompt", version.ref = "mlkitGenaiPrompt" }`.

2. **`core/ai/build.gradle.kts`** — add an `androidMain.dependencies { implementation(libs.mlkit.genai.prompt) }` source-set block (keep commonMain pure). If the genai-prompt artifact requires a higher `minSdk` than the module currently sets, raise `core/ai` android `minSdk` only as needed (check the artifact's requirement; many GenAI APIs need minSdk 26). Confirm Android compile still works.

3. **`core/ai/src/androidMain/kotlin/com/dv/moneym/core/ai/GeminiNanoAiEngine.kt`**
   ```kotlin
   class GeminiNanoAiEngine(/* context if required by getClient */) : AiEngine {
       override val supportsTools = false   // Prompt API is text-only; no function calling
       override suspend fun availability(): AiAvailability { /* map checkStatus() -> AiAvailability */ }
       override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> {
           val prompt = PromptBuilder.build(messages, grounding, SYSTEM_INSTRUCTION)
           return model.generateContentStream(prompt).map { it.candidates[0].text }
       }
   }
   ```
   - `availability()` maps: AVAILABLE→AVAILABLE, DOWNLOADABLE→DOWNLOADABLE, DOWNLOADING→DOWNLOADING, UNAVAILABLE→UNAVAILABLE. Wrap in runCatching → UNAVAILABLE on any error (e.g. AICore absent).
   - `SYSTEM_INSTRUCTION`: a private const string — a finance-assistant instruction. (User-facing UI strings go to resources later; this is a model-side system prompt, not shown in UI — keep as a const here, acceptable.)
   - Because `supportsTools = false`, the engine ignores `Grounding.Tools` data (PromptBuilder already omits a data block for Tools); the VM in Phase 4 handles the snapshot fallback. The engine itself does not need to fall back.
   - Lazily create the client (first use) so construction never throws on unsupported devices.

4. **`composeApp/src/androidMain/kotlin/com/dv/moneym/di/AndroidPlatformModule.kt`**
   - Add `implementation(projects.core.ai)` to `composeApp` android deps if not already visible (composeApp likely already depends on all modules; verify it can see `GeminiNanoAiEngine` — add `projects.core.ai` to composeApp `build.gradle.kts` commonMain or androidMain as appropriate).
   - Register: `single<AiEngine> { GeminiNanoAiEngine(/* get<Context>() if needed */) }`.

5. **AICore manifest** — ML Kit GenAI usually needs no manifest entry, but if the artifact docs require an `<uses-library>` / AICore metadata or a specific `targetSdk`, add minimally to `composeApp/src/androidMain/AndroidManifest.xml`. Only if required to compile/assemble.

## Constraints
- commonMain stays pure — all ML Kit code in `androidMain` only.
- Use `android-cli` skill to confirm SDK/AGP can resolve the dependency; `feature-module` skill for wiring.
- Kotlin conventions; no TODO placeholders.
- Engine must construct without throwing on devices lacking AICore (lazy client).

## Verify
```bash
cd /Users/davidvalic/Developer/MoneyM/.claude/worktrees/ai-analysis
./gradlew :core:ai:compileDebugKotlinAndroid
./gradlew :composeApp:assembleDebug
```
Both must pass. Report dependency resolution result, exact `Generation.getClient`/`checkStatus`/`generateContentStream` signatures you bound against, and build output.
