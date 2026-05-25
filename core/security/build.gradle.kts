import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "CoreSecurity"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.cryptography.core)
            implementation(projects.core.common)
            implementation(projects.core.datastore)
        }
        androidMain.dependencies {
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.activity.compose)
            implementation(libs.cryptography.provider.jdk)
        }
        iosMain.dependencies {
            implementation(libs.cryptography.provider.apple)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.cryptography.provider.jdk)
            }
        }
    }
}

android {
    namespace = "com.dv.moneym.core.security"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
