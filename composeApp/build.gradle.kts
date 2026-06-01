import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

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
            export(projects.core.oauth)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
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
            implementation(libs.androidx.lifecycle.navigation3)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.org.jetbrains.navigation3.ui)
            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.core.platform)
            // Logging
            implementation(libs.kermit)
            // Settings (needed for DI wiring of DefaultAppSettings)
            implementation(libs.multiplatform.settings.no.arg)
            // Room (needed to call .close() on databases in DI wiring)
            implementation(libs.room.runtime)
            // Core modules
            implementation(projects.core.model)
            implementation(projects.core.designsystem)
            implementation(projects.core.ui)
            implementation(projects.core.common)
            implementation(projects.core.datastore)
            implementation(projects.core.navigation)
            implementation(projects.core.security)
            implementation(projects.core.ai)
            // Data modules
            implementation(projects.data.categories)
            implementation(projects.data.accounts)
            implementation(projects.data.transactions)
            implementation(projects.data.backup)
            implementation(projects.data.budgets)
            implementation(projects.data.remotebackup)
            implementation(projects.data.sync)
            api(projects.core.oauth)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Feature modules
            implementation(projects.feature.transactions)
            implementation(projects.feature.transactionEdit)
            implementation(projects.feature.security)
            implementation(projects.feature.settings)
            implementation(projects.feature.categories)
            implementation(projects.feature.budgets)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.overview)
            implementation(projects.feature.infopage)
            implementation(projects.feature.about)
            implementation(projects.feature.sync)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.core.testing)
        }
    }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.dv.moneym"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        val signingStoreFile = localProperties.getProperty("signing.storeFile")
        if (signingStoreFile != null) {
            getByName("debug") {
                storeFile = rootProject.file(signingStoreFile)
                storePassword = localProperties.getProperty("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.getProperty("signing.keyPassword")
            }
            create("release") {
                storeFile = rootProject.file(signingStoreFile)
                storePassword = localProperties.getProperty("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.getProperty("signing.keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.dv.moneym"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val googleOAuthServerClientId: String? = localProperties.getProperty("googleOAuthServerClientId")
            ?: System.getenv("GOOGLE_OAUTH_SERVER_CLIENT_ID")
        buildConfigField(
            "String",
            "GOOGLE_OAUTH_SERVER_CLIENT_ID",
            googleOAuthServerClientId?.let { "\"$it\"" } ?: "null",
        )
    }
    buildFeatures {
        buildConfig = true
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
            signingConfig = signingConfigs.findByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("debug")
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

