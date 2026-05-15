plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // compilerOptions DSL inside androidTarget is unsupported with sqldelight 2.0.x;
    // JVM target is controlled via android.compileOptions below.
    androidTarget()
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "DataAccounts"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(projects.core.model)
            implementation(projects.core.common)
            implementation(projects.core.database)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.core.testing)
        }
    }
}

sqldelight {
    databases {
        create("AccountsDatabase") {
            packageName.set("com.dv.moneym.data.accounts.db")
        }
    }
}

android {
    namespace = "com.dv.moneym.data.accounts"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
