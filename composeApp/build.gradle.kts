import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
            }
        }
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.org.jetbrains.navigation3.ui)
            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            // Logging
            implementation(libs.kermit)
            // Settings (needed for DI wiring of DefaultAppSettings)
            implementation(libs.multiplatform.settings.no.arg)
            // Core modules
            implementation(projects.core.model)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
            implementation(projects.core.common)
            implementation(projects.core.datastore)
            implementation(projects.core.database)
            implementation(projects.core.navigation)
            implementation(projects.core.security)
            // Data modules
            implementation(projects.data.categories)
            implementation(projects.data.accounts)
            implementation(projects.data.transactions)
            implementation(projects.data.backup)
            // Feature modules
            implementation(projects.feature.transactions)
            implementation(projects.feature.transactionEdit)
            implementation(projects.feature.security)
            implementation(projects.feature.settings)
            implementation(projects.feature.categories)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.overview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.core.testing)
        }
    }
}

android {
    namespace = "com.dv.moneym"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dv.moneym"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

