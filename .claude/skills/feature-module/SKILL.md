---
name: feature-module
description: Step-by-step recipe for adding a new feature module to MoneyM — Gradle wiring, Koin DI module, navigation graph hook, source set layout, and verification. Use whenever creating feature/* or data/* or core/* modules.
---

# Adding a new module

Each feature/data/core module is its own Gradle project, configured via a small KMP build script. Follow this checklist in order — every step matters, and skipping the verification at the end is how we end up with modules nobody can compile against.

## 1. Decide what kind of module

| Kind | Path | Allowed deps |
|---|---|---|
| `feature:*` | `feature/<name>/` | `core:*`, `data:*` (no other features) |
| `data:*` | `data/<name>/` | `core:model`, `core:common`, `core:database`, `core:datastore`, `core:security` |
| `core:*` (leaf) | `core/<name>/` | `core:model` only (some special cases — see `docs/architecture/overview.md`) |

If unsure, re-read `docs/architecture/overview.md`. Cross-feature imports are a smell — extract shared code to `core:*` instead.

## 2. Create the directory tree

```
<kind>/<name>/
  build.gradle.kts
  src/
    commonMain/kotlin/com/dv/moneym/<kind>/<name>/
    commonTest/kotlin/com/dv/moneym/<kind>/<name>/
```

Add platform source sets (`androidMain`, `iosMain`) **only** if the module has expect/actual.

## 3. Write the build script

Use the convention-plugin-style block (see existing modules for the canonical example). Typical feature module:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "<name>" ; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
            implementation(projects.core.model)
            implementation(projects.core.common)
            implementation(projects.core.navigation)
            implementation(projects.data.<dataModuleYouNeed>)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            // ...
        }
        commonTest.dependencies { implementation(projects.core.testing) }
    }
}

android {
    namespace = "com.dv.moneym.<kind>.<name>"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
}
```

Data modules drop the Compose plugins and depend on SQLDelight / settings instead.

## 4. Register the module

In `settings.gradle.kts`:

```kotlin
include(":<kind>:<name>")
```

We use `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`, so the include name is what becomes `projects.<kind>.<name>` in other modules.

## 5. Create a Koin module

Every module that produces injectables exposes a single Koin module:

```kotlin
// commonMain/.../di/<Name>Module.kt
val <name>Module = module {
    viewModelOf(::<Name>ViewModel)
    factoryOf(::<Name>UseCase)
    // repositories: singleOf(::<Name>RepositoryImpl) bind <Name>Repository::class
}
```

Add the module to `composeApp/.../di/AppModules.kt`. There is one source of truth for the list of modules.

## 6. Wire navigation (feature modules only)

Each feature module owns its routes and exposes a `NavGraphBuilder` extension:

```kotlin
// commonMain/.../navigation/<Name>Graph.kt
sealed interface <Name>Route {
    @Serializable object Root : <Name>Route
    @Serializable data class Detail(val id: String) : <Name>Route
}

fun NavGraphBuilder.<name>Graph(
    onNavigateToX: () -> Unit,
) {
    composable<<Name>Route.Root> { <Name>Screen(onNavigateToX = onNavigateToX) }
    composable<<Name>Route.Detail> { backStackEntry ->
        val args = backStackEntry.toRoute<<Name>Route.Detail>()
        <Name>DetailScreen(id = args.id)
    }
}
```

Add the graph call in `composeApp/.../navigation/RootGraph.kt`.

## 7. Tests

Add a `commonTest` source set with at least one ViewModel test (see the `testing` skill).

## 8. Verify

```bash
./gradlew :<kind>:<name>:compileKotlinMetadata
./gradlew :<kind>:<name>:allTests
./gradlew :composeApp:assembleDebug
```

If `assembleDebug` fails after the module is added, the wiring is wrong — usually missing Koin module registration or a missing `include` in `settings.gradle.kts`. Fix immediately; don't move on.

## Anti-patterns

- Adding the new module's deps to `composeApp` "to make it work" — `composeApp` only depends on `feature:*` and bootstrap `core:*` modules.
- Sharing code by importing `feature:a` from `feature:b`. Extract to `core:*` instead.
- Putting Compose deps in a `data:*` or `core:model` module.
- Skipping the Koin module and `@Composable`-instantiating a ViewModel.
