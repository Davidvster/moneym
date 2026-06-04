# Phase 7 — iOS native bridge (STUB) + build gate

**Status:** Not started
**Depends on:** Phases 1–6

## Goal
Provide the iOS Swift bridge so the Kotlin `IosLocalLlmRunner` has something to talk to, **without**
forcing the MediaPipe/LiteRT-LM framework into the build yet. iOS must compile and report the local
engine as *unavailable* until the framework is linked manually.

## Tasks
1. `iosApp/iosApp/MediaPipeBridge.swift` (new): `final class MediaPipeBridgeImpl: NSObject, IosLocalLlmBridge`.
   - Guard the LLM framework import behind `#if canImport(MediaPipeTasksGenAI)` (or the LiteRT-LM
     module). When NOT importable: `isLoaded()=false`, `loadModel(...) -> onResult(false)`,
     `streamReply(...) -> onError("On-device model runtime not linked")`.
   - Inside the `#if`: real impl — build the inference engine from the model file path in
     `loadModel`, run streaming generation in `streamReply` forwarding chunks (mirror
     `FoundationModelsBridge.swift`).
2. `iosApp/iosApp/iOSApp.swift`: add
   `IosLocalLlmBridgeHolder.shared.instance = MediaPipeBridgeImpl()` next to the existing
   `IosAiBridgeHolder` line.
3. `plan/ai-analysis-on-device/IOS_MANUAL_STEPS.md` (new): document how the user links the runtime —
   CocoaPods (`Podfile` + `MediaPipeTasksGenAI`/LiteRT-LM pod, switch to `.xcworkspace`) **or** SPM /
   prebuilt `.xcframework`. Note the `#if canImport` flips on automatically once linked.

## Verify (final gate — BOTH must pass)
- Android: `./gradlew :composeApp:assembleDebug`
- iOS link: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- iOS app build:
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination "generic/platform=iOS Simulator" build CODE_SIGNING_ALLOWED=NO`
- Full tests: `./gradlew testDebugUnitTest`

## Commit
`feat(ios): on-device LLM Swift bridge stub + manual framework-linking docs`
