import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.paparazzi)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "FeatureAimodels"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.navigation3.runtime)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
            implementation(projects.core.model)
            implementation(projects.core.common)
            implementation(projects.core.navigation)
            implementation(projects.data.llmmodels)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.core.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.composable.preview.scanner)
            }
        }
    }
}

android {
    namespace = "com.dv.moneym.feature.aimodels"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
