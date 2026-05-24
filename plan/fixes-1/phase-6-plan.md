# Phase 6 — Final Android + iOS build verify

Run from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`.

## Goal

Prove the worktree's `main` head builds cleanly on both Android and iOS from a clean state.

## Commands (executed by orchestrator)

```
./gradlew clean
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosArm64 :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew testDebugUnitTest
./gradlew iosSimulatorArm64Test
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "generic/platform=iOS Simulator" build CODE_SIGNING_ALLOWED=NO
```

Note: the original orchestrator plan referenced `:composeApp:assembleDebugXCFramework`, but this project does not configure an XCFramework — its iOS distribution uses plain Kotlin/Native framework binaries consumed directly by the Xcode project. The two `linkDebugFramework*` tasks plus the `xcodebuild` step cover the full chain.

## Result

All six commands above completed with `BUILD SUCCESSFUL` / `** BUILD SUCCEEDED **`.

## Notes

- Build warnings about `kotlinx.datetime` deprecations (`monthNumber`, `dayOfMonth`) and one Kotlin/Native warning about an inferred bundle ID for `ComposeApp` are pre-existing and out of scope for fixes-1.
