# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MoneyM is a Kotlin Multiplatform (KMP) project targeting Android, iOS, Web (JS), and WebAssembly. All shared UI is built with Compose Multiplatform. The project is currently at the starter/template stage — there is no data layer, navigation, or DI framework yet.

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug

# Web (Wasm) — development server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS) — development server
./gradlew :composeApp:jsBrowserDevelopmentRun

# iOS — open iosApp/ in Xcode and run from there

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
├── iosMain/                            # iOS entry point (MainViewController)
├── jsMain/                             # JS platform impl
├── wasmJsMain/                         # Wasm platform impl
└── commonTest/                         # Shared tests
```

iOS Xcode project lives in `iosApp/`.

## Key Tech Stack

| Area | Library | Version |
|------|---------|---------|
| Language | Kotlin | 2.3.21 |
| UI | Compose Multiplatform | 1.10.3 |
| Android Activity | androidx-activity-compose | 1.13.0 |
| ViewModel | androidx-lifecycle-viewmodel-compose | 2.10.0 |
| UI Theme | Material3 | 1.10.0-alpha05 |
| JVM target | — | 11 |

## Platform Abstraction Pattern

Platform-specific behavior uses Kotlin's `expect/actual` mechanism:

- `commonMain/Platform.kt` — `expect` interface/class
- `androidMain/Platform.android.kt`, `iosMain/Platform.ios.kt`, etc. — `actual` implementations

Follow this pattern for any new platform-specific code.

## Dependency Catalog

All dependency versions are managed in `gradle/libs.versions.toml`. Always add new dependencies there rather than hardcoding versions in build scripts.
