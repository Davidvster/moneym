# Libraries

The full list of third-party libraries used in MoneyM, with the reasoning for each pick. Anything not on this list should not be added without an entry here (or an ADR in `decisions.md`).

Versions are pinned in `gradle/libs.versions.toml` — this doc names libraries, not versions.

## Language & runtime

| Library | Purpose | Why this one |
|---|---|---|
| **Kotlin Multiplatform** | Language + targets | Already in the base project; the whole premise. |
| **Compose Multiplatform** | Shared UI | Already in the base project; lets us share screens across Android and iOS. |

## State & concurrency

| Library | Purpose | Why this one |
|---|---|---|
| **kotlinx-coroutines-core** | Async + Flow | De facto standard; no real alternative on KMP. |
| **kotlinx-datetime** | Dates and times | The only credible multiplatform option. `java.time` doesn't exist on iOS. |
| **kotlinx-serialization-json** | JSON + DTO encoding | First-party, multiplatform, plays well with `kotlinx-datetime` and SQLDelight. Alternatives like Moshi/Gson are JVM-only. |

## Persistence

| Library | Purpose | Why this one |
|---|---|---|
| **SQLDelight 2.x** | Local database | Multiplatform, type-safe queries, schema migrations, Flow integration. User-requested. |
| **multiplatform-settings** (Russ Wolf, with `-coroutines` and `-no-arg`) | Key-value preferences | Standard multiplatform settings library. `-coroutines` gives us Flow-backed observation; `-no-arg` lets us create the default impl without platform plumbing. |

## DI

| Library | Purpose | Why this one |
|---|---|---|
| **Koin** (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) | Dependency injection | User-requested. Runtime DSL, no codegen, low ceremony, mature KMP support. The alternative was Kotlin-Inject (compile-time, safer, more boilerplate) — Koin wins on ergonomics for an app of this size. |

## Navigation

| Library | Purpose | Why this one |
|---|---|---|
| **Jetpack Navigation Compose (multiplatform)** (`org.jetbrains.androidx.navigation:navigation-compose`) | App navigation | User-requested. Type-safe routes via `kotlinx-serialization`. Official, lowest friction, well-documented. |

## UI

| Library | Purpose | Why this one |
|---|---|---|
| **Compose Material 3** (already present) | Theme baseline | Comes with Compose Multiplatform; gives us color schemes and typography tokens we lean on. |
| **AndroidX Lifecycle Compose** (`viewmodel-compose`, `runtime-compose`) (already present) | `ViewModel` on KMP, `collectAsStateWithLifecycle` | Official multiplatform `ViewModel` story. |
| **`io.github.ehsannarmani:compose-charts`** | Charts (line, bar, pie/donut) | User-requested. The other credible KMP option (Koalaplot) has fewer chart types. We isolate this behind `feature:overview`'s presentation layer so a swap is a one-module change. |

## Security

| Library | Purpose | Why this one |
|---|---|---|
| **AndroidX Biometric** (`androidx.biometric:biometric`) | Android biometric prompt | Standard. Handles fingerprint + face on Android with one API. |
| **AndroidX Security Crypto** (`androidx.security:security-crypto`) | `EncryptedSharedPreferences` + Keystore wrappers on Android | Avoids reinventing AES-GCM-keystore plumbing. |
| **PBKDF2-HMAC-SHA256** (built-in, 600k iterations) | PIN hashing | Decided in ADR-010. No native binary, no `expect/actual` on the algorithm itself — a small pure-Kotlin implementation works for all targets. Hash params are stored alongside the hash so we can migrate to Argon2 in the future without forcing a re-PIN. |
| Platform Keychain (iOS, via `expect/actual`) | Secure store on iOS | Native APIs (`SecItemAdd` etc.) accessed via cinterop. No third-party wrapper — the surface we need is small. |

## Logging

| Library | Purpose | Why this one |
|---|---|---|
| **Kermit** (`co.touchlab:kermit`) | Multiplatform structured logging | KMP-native, low overhead, has Android/iOS log writers built in, pluggable. Replaces both `Log.d` (Android) and `print` (iOS) with a single call site. Crash reporting is **not** wired in (ADR-014); Kermit's design lets us add a Sentry/Crashlytics writer later without touching call sites. |

## Testing

| Library | Purpose | Why this one |
|---|---|---|
| **kotlin.test** | Test runner / assertions baseline | Multiplatform; minimal. |
| **kotest-assertions-core** | Expressive assertions | `shouldBe`, `shouldContain`, `shouldBeRightOf` etc. without pulling the rest of Kotest. |
| **app.cash.turbine** | Flow assertions in tests | The standard. Makes Flow tests readable. |
| **kotlinx-coroutines-test** | `StandardTestDispatcher`, `runTest` | Required for deterministic coroutine tests. |
| **Mockative** (`io.mockative:mockative` + KSP) | Mocking on all KMP targets | Works on JVM, JS, Native — including iOS, which MockK does not support. We prefer fakes (in `core:testing`); Mockative is the escape hatch. |
| **AndroidX Compose UI Test** | Compose UI tests on Android | Android-only for v1; we don't yet test Compose UI on iOS. |

## Build & tooling (already wired)

- **Android Gradle Plugin 8.11.2**, AGP-driven build
- **Compose Compiler plugin** (Kotlin-bundled, present)
- **TypeSafe project accessors** (enabled in `settings.gradle.kts`)

## Deliberately NOT chosen (for v1)

| Not using | Why |
|---|---|
| **Ktor** | No remote calls in v1. User said "prefer Ktor for networking" — we will prefer it, when we need networking. Adding it now would be dead weight. |
| **Room** | Android-only. SQLDelight is the multiplatform equivalent. |
| **Realm** | Heavy, opinionated, sync model that doesn't match local-first + future cloud-of-our-choosing. |
| **DataStore** | Excellent on Android, no real iOS story. `multiplatform-settings` is the KMP replacement. |
| **MVI frameworks** (Orbit, MVIKotlin, etc.) | Our intent/state/effect pattern is small enough to hand-roll; a framework here is overkill. |
| **Hilt** | Not multiplatform. |
| **Decompose / Voyager** | Jetpack Nav was the user's pick. |
| **MockK** | Doesn't support iOS native. Replaced by Mockative + fakes. |
| **Detekt / ktlint** in v1 | Worth adding; not blocking on it for Phase 1. Add when the codebase has > ~5 modules and we feel drift. |

## When to revisit

- Add **Ktor** in Phase 6 (sync).
- Reconsider **DataStore-Multiplatform** if it stabilizes — currently `multiplatform-settings` is the safer choice.
- Reconsider **Compose Hot Reload** for dev ergonomics once it's out of preview.
- Add **kotlinx-immutable-collections** if we hit Compose stability issues with `List<T>` in UI state.
