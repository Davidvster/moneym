# iOS — linking the LiteRT-LM runtime (manual, ~4 clicks)

The Kotlin + Swift code for the on-device model engine is complete and uses **LiteRT-LM**
(non-deprecated). The native runtime is added via **Swift Package Manager** — no CocoaPods,
no `.xcworkspace` conversion. Until you add the package, `LiteRtLmBridge.swift`'s
`#if canImport(LiteRTLM)` is false, so the bridge reports the engine *unavailable* and the
app builds/runs fine (Apple Intelligence still works where supported; Android is fully
wired). Adding the package flips the branch on — **no further Kotlin/Swift edits**.

## Add the package in Xcode

1. Open `iosApp/iosApp.xcodeproj` (the normal project — no workspace needed).
2. **File ▸ Add Package Dependencies…**
3. In the search/URL field enter:
   ```
   https://github.com/google-ai-edge/LiteRT-LM
   ```
4. Dependency Rule: **Up to Next Major**, starting from **0.13.0** (matches the Android
   `litertLm = "0.13.0"` in `gradle/libs.versions.toml` — keep them in sync).
5. **Add Package**, then add the **`LiteRTLM`** library product to the **`iosApp`** target.
6. Build & run on a **physical device** (Metal/GPU backend; the simulator may not support
   the GPU backend — if a simulator build fails inside LiteRTLM, run on device, or set
   `backend: .cpu` in `LiteRtLmBridge.swift`).

That's it. `LiteRtLmBridge.swift` already implements the real path:
`EngineConfig(modelPath:backend:cacheDir:)` → `Engine(engineConfig:)` → `initialize()` →
`createConversation()` → `for try await chunk in conversation.sendMessageStream(Message(...))`.

## CLI alternative (no GUI)

SPM deps are stored in the `.xcodeproj`; adding them from pure CLI means editing
`project.pbxproj` (fiddly). The GUI flow above is the supported path. After it's added once,
`xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp ... build` resolves the package
automatically.

## Model storage & catalog

Downloaded `.litertlm` files live under the app's Application Support dir
(`DbPlatform.appFilesDirectory` + `/models/`). The model-manager screen handles download,
checksum (when a sha256 is set in `LlmModelCatalog`), activation, and deletion; the active
model path is what `loadModel(path:)` receives.

`LlmModelCatalog` ships the 3 model URLs (Gemma 3 1B, Gemma 4 E2B, Gemma 3n E2B) with
**empty sha256** (verification skipped) and approximate sizes. Before shipping, fill in real
`sha256` + `sizeBytes` from the HuggingFace `litert-community` repos and confirm the exact
`.litertlm` filenames in the `resolve/main/...` URLs.

## Keep Android + iOS in sync

- Android runtime: `com.google.ai.edge.litertlm:litertlm-android` (version `litertLm` in the
  version catalog).
- iOS runtime: SPM `LiteRTLM` at the matching version.
Bump both together.
