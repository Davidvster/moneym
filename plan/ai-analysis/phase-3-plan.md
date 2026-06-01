# Phase 3 — iOS Foundation Models engine + Swift bridge

**Status:** IN PROGRESS

## Goal
Implement `AiEngine` on iOS backed by Apple **Foundation Models** (iOS 26 + Apple Intelligence), via a Swift bridge injected from `iosApp` — mirroring the existing `GoogleSignInBridge` pattern exactly.

## Reference pattern (mirror this — DO NOT invent a new mechanism)
- Kotlin interface + holder object in iosMain: `core/oauth/src/iosMain/kotlin/com/dv/moneym/core/oauth/GoogleSignInBridge.kt`:
  ```kotlin
  interface GoogleSignInBridge { fun signIn(scopes: List<String>, onResult: (String?, String?) -> Unit); ... }
  object GoogleSignInBridgeHolder { var instance: GoogleSignInBridge? = null }
  ```
- Swift impl `iosApp/iosApp/GoogleSignInBridge.swift`: `final class GoogleSignInBridgeImpl: NSObject, GoogleSignInBridge { ... }`.
- Registration in `iosApp/iosApp/iOSApp.swift` `init()`: `GoogleSignInBridgeHolder.shared.instance = GoogleSignInBridgeImpl()`.
- Consumer reads `GoogleSignInBridgeHolder.instance` and adapts callbacks to coroutines via `suspendCancellableCoroutine` (see `IosGoogleAuthManager.kt`).

## Tasks

1. **Move `IosAiBridge` to iosMain + make it callback-based** (Swift cannot implement suspend/Flow). Delete `core/ai/src/commonMain/kotlin/com/dv/moneym/core/ai/IosAiBridge.kt`; create `core/ai/src/iosMain/kotlin/com/dv/moneym/core/ai/IosAiBridge.kt`:
   ```kotlin
   interface IosAiBridge {
       fun isAvailable(): Boolean
       fun streamReply(
           prompt: String,
           onChunk: (String) -> Unit,     // cumulative text-so-far from Foundation Models
           onComplete: () -> Unit,
           onError: (String) -> Unit,
       )
   }
   object IosAiBridgeHolder { var instance: IosAiBridge? = null }
   ```
   (Nothing in commonMain references `IosAiBridge`, so the move is safe — confirm `AiEngine`/`PromptBuilder` still compile.)

2. **`core/ai/src/iosMain/kotlin/com/dv/moneym/core/ai/IosFoundationModelsAiEngine.kt`**:
   ```kotlin
   class IosFoundationModelsAiEngine : AiEngine {
       override val supportsTools = false
       override suspend fun availability(): AiAvailability =
           if (IosAiBridgeHolder.instance?.isAvailable() == true) AiAvailability.AVAILABLE else AiAvailability.UNAVAILABLE
       override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> = callbackFlow {
           val bridge = IosAiBridgeHolder.instance ?: run { close(); return@callbackFlow }
           val prompt = PromptBuilder.build(messages, grounding, SYSTEM_INSTRUCTION)
           var last = ""
           bridge.streamReply(
               prompt = prompt,
               onChunk = { cumulative -> val delta = cumulative.removePrefix(last); last = cumulative; trySend(delta) },
               onComplete = { close() },
               onError = { msg -> close(IllegalStateException(msg)) },
           )
           awaitClose { }
       }
   }
   ```
   - Engine converts Foundation Models' **cumulative** snapshots → **deltas** (matches Android's delta contract; VM appends).
   - `SYSTEM_INSTRUCTION`: private const finance-assistant instruction (model-side, not UI — const is fine).

3. **Swift bridge `iosApp/iosApp/FoundationModelsBridge.swift`**:
   - Guard the framework so the app still builds on Xcode/SDK lacking Foundation Models:
     ```swift
     import Foundation
     import ComposeApp
     #if canImport(FoundationModels)
     import FoundationModels
     #endif

     final class FoundationModelsBridgeImpl: NSObject, IosAiBridge {
         func isAvailable() -> Bool {
             #if canImport(FoundationModels)
             if #available(iOS 26.0, *) {
                 if case .available = SystemLanguageModel.default.availability { return true }
             }
             #endif
             return false
         }
         func streamReply(prompt: String, onChunk: @escaping (String) -> Void,
                          onComplete: @escaping () -> Void, onError: @escaping (String) -> Void) {
             #if canImport(FoundationModels)
             if #available(iOS 26.0, *) {
                 Task {
                     do {
                         let session = LanguageModelSession(instructions: "")
                         let stream = session.streamResponse(to: prompt)
                         for try await partial in stream {
                             onChunk(partial.content)   // cumulative String-so-far; adapt to actual partial text accessor
                         }
                         onComplete()
                     } catch { onError(error.localizedDescription) }
                 }
                 return
             }
             #endif
             onError("Foundation Models unavailable")
         }
     }
     ```
   - Resolve the exact accessor for the partial's text against the iOS 26 SDK (`partial.content`, or `String(partial)`, or `partial.asPartiallyGenerated()` — pick what compiles when FoundationModels is present). Forward the **cumulative** text.

4. **Register the bridge** in `iosApp/iosApp/iOSApp.swift` `init()` (next to the GoogleSignIn line):
   `IosAiBridgeHolder.shared.instance = FoundationModelsBridgeImpl()`.

5. **Koin** `composeApp/src/iosMain/kotlin/com/dv/moneym/di/IosPlatformModule.kt`: add `single<AiEngine> { IosFoundationModelsAiEngine() }` (import `AiEngine` from `com.dv.moneym.core.ai`). Ensure `composeApp` iosMain depends on `projects.core.ai` (it already does after Phase 2 added the dep to composeApp — verify the dep is visible to the iOS target; if Phase 2 only added it to androidMain, add `implementation(projects.core.ai)` to commonMain so both platforms see it).

## Constraints
- Mirror GoogleSignInBridge exactly (callback closures, Holder object, set in iOSApp.init).
- Kotlin: callbackFlow from `kotlinx.coroutines.channels`/`flow`. No suspend/Flow across the Swift boundary.
- Use `feature-module` skill for wiring conventions.
- Swift must compile even without the FoundationModels SDK (`#if canImport`).

## Verify
```bash
cd /Users/davidvalic/Developer/MoneyM/.claude/worktrees/ai-analysis
./gradlew :core:ai:compileKotlinIosSimulatorArm64
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "generic/platform=iOS Simulator" build CODE_SIGNING_ALLOWED=NO
```
All must pass. If the local Xcode lacks the iOS 26 SDK, the `#if canImport(FoundationModels)` guard must still let the build succeed (bridge reports unavailable) — confirm and report the Xcode/SDK version detected.
