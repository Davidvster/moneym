# Phase 1 — `core/ai` foundation + pure helpers + tests

**Status:** IN PROGRESS

## Goal
Create a new **pure** KMP module `core/ai` holding the on-device-AI abstractions (interfaces + models + a unit-testable prompt helper). No Compose, no data-layer deps. Platform engine impls (Android Gemini Nano, iOS Foundation Models) come in later phases — only the `androidMain`/`iosMain` source sets are scaffolded here.

## Why
The whole AI feature pivots on one engine-agnostic seam (`AiEngine`) so the chat feature never touches platform AI SDKs, and prompt assembly stays testable in `commonTest`.

## Tasks

1. **settings.gradle.kts** — add `include(":core:ai")` in the core group (alphabetical-ish, near other core modules).

2. **`core/ai/build.gradle.kts`** — copy the pattern from `core/common/build.gradle.kts` and `core/model/build.gradle.kts`:
   - plugins: `kotlinMultiplatform`, `androidLibrary`, `kotlinSerialization`.
   - `androidTarget` JVM_11; `iosArm64()` + `iosSimulatorArm64()` framework `baseName = "CoreAi"`, `isStatic = true`.
   - commonMain deps: `libs.kotlinx.coroutines.core`, `libs.kotlinx.datetime`, `libs.kotlinx.serialization.json`, `projects.core.model`, `projects.core.common`.
   - commonTest deps: `libs.kotlin.test`, `projects.core.testing` (only if needed; PromptBuilder test needs no fakes — `libs.kotlin.test` alone is fine).
   - android namespace `com.dv.moneym.core.ai`, compileSdk/minSdk from catalog, JVM_11 compileOptions.

3. **commonMain models/interfaces** under `core/ai/src/commonMain/kotlin/com/dv/moneym/core/ai/`:
   - `ChatMessage.kt`: `enum class ChatRole { USER, ASSISTANT }`; `data class ChatMessage(val role: ChatRole, val content: String)`.
   - `AiTool.kt`: `data class AiTool(val name: String, val description: String, val paramsSchema: String, val invoke: suspend (Map<String, String>) -> String)` (read-only query tool). Keep `invoke` a lambda so feature layer supplies repo-backed bodies.
   - `Grounding.kt`: `sealed interface Grounding { data class Snapshot(val text: String) : Grounding ; data class Tools(val tools: List<AiTool>) : Grounding }`.
   - `AiAvailability.kt`: `enum class AiAvailability { AVAILABLE, UNAVAILABLE, DOWNLOADABLE, DOWNLOADING }`.
   - `AiGroundingMode.kt`: `enum class AiGroundingMode { SNAPSHOT, TOOLS }`.
   - `AiEngine.kt`:
     ```kotlin
     interface AiEngine {
         val supportsTools: Boolean
         suspend fun availability(): AiAvailability
         fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String>
     }
     ```
   - `IosAiBridge.kt`: interface mirroring engine semantics for the Swift impl (P3 uses it). Keep ObjC-friendly (no sealed types across the boundary): 
     ```kotlin
     interface IosAiBridge {
         suspend fun isAvailable(): Boolean
         fun streamReply(prompt: String): Flow<String>
     }
     ```
     (Engine assembles `messages+grounding` → single `prompt` via PromptBuilder before calling the bridge.)
   - `PromptBuilder.kt`: **pure** object/class. Function `build(messages: List<ChatMessage>, grounding: Grounding, systemInstruction: String): String` that assembles: system instruction, then (if `Grounding.Snapshot`) a `Financial data:\n<text>` block, then the chat history in order (`User:` / `Assistant:` prefixes), ending ready for the model to continue. For `Grounding.Tools`, omit the data block (tools supply data at call time) but still include a note that tools are available. Deterministic, no platform deps.

4. **commonTest** `core/ai/src/commonTest/kotlin/com/dv/moneym/core/ai/PromptBuilderTest.kt`:
   - snapshot grounding injects the data block.
   - history ordering preserved, role prefixes correct.
   - empty history → only system (+ data) preamble.
   - tools grounding omits data block.
   Use `kotlin.test` assertions (matches repo style).

5. **Scaffold** empty `core/ai/src/androidMain/kotlin/com/dv/moneym/core/ai/` and `core/ai/src/iosMain/kotlin/com/dv/moneym/core/ai/` dirs (a `.gitkeep` is fine) so P2/P3 have the source sets. No engine code yet.

## Constraints
- Follow `feature-module` + `testing` skills.
- Kotlin conventions: import classes, no needless comments, no TODO placeholders.
- Do NOT wire into Koin or any app module yet — module stays unused this phase.

## Verify
```bash
./gradlew :core:ai:compileDebugKotlinAndroid
./gradlew :core:ai:compileKotlinIosSimulatorArm64
./gradlew :core:ai:testDebugUnitTest
```
All must pass. Report file list + test results.
