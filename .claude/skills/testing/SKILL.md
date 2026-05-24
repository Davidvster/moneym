---
name: testing
description: Testing strategy and conventions for MoneyM — test pyramid, source-set layout, libraries (Turbine, kotest-assertions, mockative, coroutines-test), fake-first approach for KMP, naming, and what to test where. Use when writing or reviewing any *Test.kt file.
---

# Testing strategy

The pyramid:

```
                    UI tests (few)
              ─ feature ViewModel + UseCase tests (many)
       ─ Repository / DataSource integration tests (some)
  ─ Pure helpers and formatters in core:common, core:designsystem (many)
```

## Where tests live

- **`commonTest`** — pure-Kotlin unit tests. ViewModels, UseCases, Repositories (with fakes), helpers. Runs on every target.
- **`androidUnitTest` / `iosTest`** — only when the test exercises platform-specific code (e.g. a real SQLDelight Android driver, real Keychain). Prefer to keep these thin.
- **`androidInstrumentedTest`** — Compose UI tests for critical screens (currently transaction list and add/edit).

A test goes into `commonTest` by default. Only push it down to a platform source set if it depends on platform APIs.

## Libraries

| Concern | Library |
|---|---|
| Test runner / assertions | `kotlin.test` + `io.kotest:kotest-assertions-core` |
| Flow assertions | `app.cash.turbine:turbine` |
| Coroutine control | `kotlinx-coroutines-test` |
| Mocking (KMP-wide) | `io.mockative:mockative` (KSP-generated; works on all our targets) |
| Compose UI tests | `androidx.compose.ui:ui-test-junit4` (Android only for now) |

**MockK is not used** — it does not work on iOS native targets. If a test needs a mock, use Mockative; otherwise write a fake.

## Fake-repository parity rule

When a method is added to a `*Repository` interface under `data/`, the matching override **must** be added to the corresponding fake under `core/testing/Fake*Repository`. Otherwise every consumer's `:*:compileDebugUnitTestKotlinAndroid` task breaks with `Class 'FakeXRepository' is not abstract and does not implement abstract members`. Same applies to `AppSettingsRepository` ↔ `FakeAppSettingsRepository`. CI catches this but it's faster to fix it as part of the same PR that added the method.

## `expect class` blocks `commonTest` faking

If a class is declared `expect class` in `commonMain` (e.g. `core/platform/.../DbPlatform.kt`), it cannot be constructed from `commonTest` — there is no `actual` available in that source set. Anything that depends on such a class transitively (e.g. `DbBackupManager` takes `DbPlatform`) is therefore not unit-testable from common code.

To unblock: redeclare the type as a plain `interface` in `commonMain` and have the platform module provide the implementation. Then a fake works trivially. Use `expect class` only for true expect/actual platform glue that genuinely has no shared shape.

## Fakes first, mocks second

A fake is a hand-written test double that satisfies the interface and stores state in memory. Prefer fakes when:
- The collaborator is used by many tests (a `FakeTransactionRepository` pays for itself fast).
- The interface is small.
- You want to assert on observed state ("repository now contains 3 transactions") instead of method calls.

Use Mockative when:
- The interface is large and the test only cares about one call.
- You need to simulate failure cases (throw on call N).

Fakes live in `core:testing` under `commonMain` (so production code never depends on them, but tests in any module can). Each fake is named `Fake<Interface>` and exposes its internal state as `val state: StateFlow<...>` for assertions.

## Coroutines in tests

Use the standard test dispatcher pattern. We provide `MainDispatcherRule` (JUnit) and a `runTestWithDispatchers { ... }` helper in `core:testing` that swaps the `DispatcherProvider` for a test one backed by `StandardTestDispatcher`.

```kotlin
@Test
fun emitsLoadedStateWhenRepositoryProvidesData() = runTestWithDispatchers {
    val repository = FakeTransactionRepository().apply { addAll(sampleTransactions) }
    val vm = TransactionListViewModel(repository, GetTransactionsUseCase(repository))

    vm.state.test {
        awaitItem().isLoading shouldBe true
        awaitItem().days.size shouldBe 3
    }
}
```

## Naming and structure

- File: one test class per production class, named `<ClassName>Test`.
- Test names: descriptive sentences, not Given/When/Then templates. `emitsLoadedStateWhenRepositoryProvidesData` is fine; `test1` is not.
- One behaviour per test. If the test needs three assertions about three things, it's probably three tests.
- AAA structure inside the test (Arrange / Act / Assert) — no comments needed if blank lines separate the sections clearly.

## ViewModel tests — what to cover

1. Initial state is correct.
2. Each intent transitions state correctly.
3. Repository updates flow through to state.
4. Effects are emitted exactly once.

Don't test framework integration (StateFlow itself, Koin wiring) — that's not your code.

## Repository tests

Repositories are tested against a fake DataSource (in-memory Map-backed). Real SQLDelight driver tests live in `androidUnitTest` and exercise schema/migration correctness — not business logic.

## What we deliberately don't test

- Generated SQLDelight code (Cash App tests it).
- Compose Material 3 internals.
- Trivial getters or one-liner mappers — if the test would just restate the implementation, skip it.

## CI signal

`./gradlew allTests` is the canonical command. PRs that don't pass `allTests` don't merge. Tests should be deterministic — flaky tests are bugs and get fixed or quarantined immediately.
