# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MoneyM is a Kotlin Multiplatform (KMP) project targeting Android, iOS. All shared UI is built with Compose Multiplatform. The project is currently at the starter/template stage — there is no data layer, navigation, or DI framework yet.

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS — build Kotlin framework first, then open in Xcode
./gradlew :composeApp:assembleDebugXCFramework   # build shared XCFramework
# Then open iosApp/iosApp.xcodeproj in Xcode and run on simulator/device
# OR build from CLI (simulator):
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  build

# Tests
./gradlew allTests                          # all platforms
./gradlew testDebugUnitTest                 # Android only
./gradlew iosSimulatorArm64Test             # iOS simulator
./gradlew wasmJsBrowserTest                 # Wasm
```

## Module Structure

Single KMP module: `composeApp`

```
composeApp/src/
├── commonMain/kotlin/com/dv/moneym/   # Shared code & Compose UI
├── androidMain/                        # Android entry point (MainActivity)
├── iosMain/                              # Wasm platform impl
└── commonTest/                         # Shared tests
```

iOS Xcode project lives in `iosApp/`.

## Key Tech Stack

| Area | Library |
|------|---------|
| Language | Kotlin |
| UI | Compose Multiplatform |
| Android Activity | androidx-activity-compose |
| ViewModel | androidx-lifecycle-viewmodel-compose |

## Platform Abstraction Pattern

Platform-specific behavior uses Kotlin's `expect/actual` mechanism:

- `commonMain/Platform.kt` — `expect` interface/class
- `androidMain/Platform.android.kt`, `iosMain/Platform.ios.kt`, etc. — `actual` implementations

Follow this pattern for any new platform-specific code.

## Dependency Catalog

All dependency versions are managed in `gradle/libs.versions.toml`. Always add new dependencies there rather than hardcoding versions in build scripts.
