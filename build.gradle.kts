buildscript {
    configurations.classpath {
        resolutionStrategy {
            // Paparazzi 2.0.0-alpha05 requires com.android.tools:sdk-common 31.13.2,
            // which Gradle's default conflict resolution picks over AGP 8.11.2's own
            // 31.11.2, leaving AGP's hardcoded aapt2 build number (12782657) paired
            // with the wrong tools version (8.13.2) -> aapt2:8.13.2-12782657 (404).
            force("com.android.tools:sdk-common:31.11.2")
            force("com.android.tools.build:aapt2-proto:8.11.2-12782657")
        }
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.paparazzi) apply false
    alias(libs.plugins.detekt) apply false
}

// detekt static analysis across every Kotlin module. Lint-only (no type
// resolution) so it stays stable across KMP source sets and configuration-cache.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        baseline = rootProject.file("config/detekt/baseline.xml")
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
            "src/commonTest/kotlin",
            "src/androidUnitTest/kotlin",
            "src/iosTest/kotlin",
        )
    }

    // Keep analysis lint-only: the type-resolution variants need a full compile
    // classpath, which is slow and flaky on KMP multi-target builds.
    tasks.matching { it.name == "detektMain" || it.name == "detektTest" }
        .configureEach { enabled = false }
}

// Paparazzi runs on the JVM without an Android Context, so Compose Multiplatform
// resources (strings, fonts) must be readable from the unit-test classpath. Each
// screenshot-tested module gets its own compose resources plus those of the shared
// UI modules its composables render at runtime.
subprojects {
    plugins.withId("app.cash.paparazzi") {
        val resourceProjects = listOf(path, ":core:ui", ":core:designsystem").distinct()
        val assetsDir = "generated/assets/copyDebugComposeResourcesToAndroidAssets"
        extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
            sourceSets.getByName("test").resources.apply {
                resourceProjects.forEach { srcDir(project(it).layout.buildDirectory.dir(assetsDir)) }
            }
        }
        tasks.matching { it.name == "processDebugUnitTestJavaRes" }.configureEach {
            resourceProjects.forEach { dependsOn("$it:copyDebugComposeResourcesToAndroidAssets") }
        }
    }
}