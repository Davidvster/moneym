# iOS — linking the on-device LLM runtime (manual)

The Kotlin + Swift code for the on-device model engine is complete, but the **native
inference runtime is not linked yet**. Until you link it, `MediaPipeBridge.swift`'s
`#if canImport(MediaPipeTasksGenAI)` is false, so the bridge reports the engine as
*unavailable* and the app builds/runs fine — the on-device engine simply won't appear as
AVAILABLE on iOS (Apple Intelligence still works where supported; Android is fully wired).

Linking the framework flips the `#if canImport` branch on automatically — **no Kotlin or
Swift edits required afterward**.

## Option A — CocoaPods (most documented for MediaPipe)

The Xcode project currently links the Kotlin framework directly and has **no CocoaPods**.
Adding a pod means switching to a `.xcworkspace`:

1. Create `iosApp/Podfile`:
   ```ruby
   platform :ios, '15.0'
   target 'iosApp' do
     use_frameworks!
     pod 'MediaPipeTasksGenAI'      # or 'MediaPipeTasksGenAIC'
   end
   ```
2. `cd iosApp && pod install`
3. Open `iosApp/iosApp.xcworkspace` (not the `.xcodeproj`) from now on.
4. Ensure the existing "Run Script" phase that links the Kotlin `ComposeApp` framework
   (`embedAndSignAppleFrameworkForXcode` / the gradle link task) is preserved.

## Option B — LiteRT-LM / prebuilt `.xcframework` (SPM or manual)

The catalog models are `.litertlm`. If you prefer the LiteRT-LM runtime, add its iOS
`.xcframework` (Swift Package or drag-in) and change the import in
`MediaPipeBridge.swift` from `MediaPipeTasksGenAI` to the LiteRT-LM module name, plus the
`LlmInference` API calls to that SDK's equivalents.

## After linking — verify the real branch

`MediaPipeBridge.swift`'s active branch uses, for the MediaPipe pod:
`LlmInference.Options(modelPath:)` → `LlmInference(options:)` →
`generateResponseAsync(inputText:progress:completion:)`. **Confirm these signatures
against the pod version you installed** (the GenAI iOS API has shifted between releases,
e.g. session-based APIs). Adjust the bridge's `#if canImport` branch if the installed
version differs. The Kotlin contract (`IosLocalLlmBridge`) does not change.

## Model storage

Downloaded `.litertlm` files live under the app's Application Support directory
(`DbPlatform.appFilesDirectory` + `/models/`). The model-manager screen handles download,
checksum (when a sha256 is set in `LlmModelCatalog`), activation, and deletion. Active
model path is what `loadModel(path:)` receives.

## Catalog note

`LlmModelCatalog` ships the 3 model URLs with **empty sha256** (verification skipped) and
approximate sizes. Before shipping, fill in the real `sha256` + `sizeBytes` from the
HuggingFace `litert-community` repos and confirm the exact `.litertlm` filenames in the
`resolve/main/...` URLs.
