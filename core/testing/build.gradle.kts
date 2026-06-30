import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "CoreTesting"; isStatic = true }
    }
    sourceSets {
        // All testing utilities live in commonMain so other modules can access
        // them from their commonTest source set via implementation(projects.core.testing)
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.datastore)
            api(projects.core.security)
            api(projects.data.categories)
            api(projects.data.accounts)
            api(projects.data.transactions)
            api(projects.data.banksync)
            api(projects.data.budgets)
            api(projects.data.overview)
            api(projects.data.llmmodels)
            api(projects.data.aichat)
            api(projects.core.ai)
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
            api(libs.kotlinx.datetime)
            api(libs.turbine)
            api(libs.kotest.assertions.core)
        }
    }
}

android {
    namespace = "com.dv.moneym.core.testing"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
