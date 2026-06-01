# Phase 2 — iOS CSV file picker (issue #2)

Problem: "Import from CSV" does nothing on iOS. Two causes:
1. `topViewController()` resolves the host VC via the deprecated `UIApplication.sharedApplication.keyWindow`, which can be nil under scene-based lifecycle.
2. `FilePicker.ios.kt` calls `presentViewController` synchronously while the import `ModalBottomSheet` is still presented/animating — iOS silently refuses the present.

Export (`FileSaver.ios`) and `.bin` restore (`BinaryFilePicker.ios`) work because they're triggered from plain buttons, not from inside a sheet.

## Files & changes

### `core/platform/src/iosMain/kotlin/com/dv/moneym/platform/TopViewController.kt`
Replace the deprecated `keyWindow` lookup with a scene-aware one:
- Iterate `UIApplication.sharedApplication.connectedScenes`, pick the active `UIWindowScene` (prefer one in `UISceneActivationStateForegroundActive`), get its key window (`windows` → the one with `isKeyWindow == true`, else first).
- Fallback chain: scene key window → scene first window → `UIApplication.sharedApplication.keyWindow` (legacy) so nothing regresses.
- Then keep the existing `while (vc?.presentedViewController != null) vc = vc.presentedViewController` walk.
- Mind cinterop casts: `connectedScenes` is a `Set<*>`; filter `as? UIWindowScene`. `windows` elements cast `as? UIWindow`. Add `@OptIn(ExperimentalForeignApi::class)` if the compiler requires it. Keep the function `internal`.

This also improves `FileSaver.ios`/`FilePlatform.ios`/`BinaryFilePicker.ios`, which share it — do not change those files.

### `core/platform/src/iosMain/kotlin/com/dv/moneym/platform/FilePicker.ios.kt`
- In the returned lambda, present on the next main-runloop tick so any in-flight Compose modal transition completes first:
  ```kotlin
  dispatch_async(dispatch_get_main_queue()) {
      topViewController()?.presentViewController(picker, animated = true, completion = null)
  }
  ```
  Build the `picker` (and set `delegate`/`allowsMultipleSelection`) before the dispatch or inside it — keep the `remember`ed `delegate` exactly as-is so it isn't GC'd. Add imports `platform.darwin.dispatch_async`, `platform.darwin.dispatch_get_main_queue`.

### `composeApp/src/commonMain/kotlin/com/dv/moneym/MainNav.kt` + import sheet host
- Ensure the import `ModalBottomSheet` (`ImportSourceSheet`) is dismissed before/at picker launch. Trace where `ImportSourceSheet` is shown (it's hosted by the export screen reached via `exportDataEntry`; `onImportSourceSelected` is plumbed from there into `MainNav`). The format buttons (`onMoneyMSelected`/`onEasyHomeFinanceSelected`) currently call `onImportSourceSelected` without closing the sheet.
- Make the sheet close first, then `csvFilePicker()` runs. Concretely: in the screen that hosts `ImportSourceSheet`, drive sheet visibility from state and set it false when a source is selected (call `onDismiss`/hide before invoking the import callback). If the simplest correct fix is to call the sheet's `onDismiss` alongside `onImportSourceSelected`, do that. Keep behavior identical on Android (closing the sheet there is harmless/correct).
- Investigate the actual host: search for `ImportSourceSheet(` usage and how `onImportSourceSelected` reaches it, then close the sheet at selection time.

## Verify
```
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
Must link. Report the exact mechanism used to dismiss the sheet before launching the picker.
