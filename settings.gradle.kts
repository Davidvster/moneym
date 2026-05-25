rootProject.name = "MoneyM"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")

// Core
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:datastore")
include(":core:security")
include(":core:navigation")
include(":core:testing")
include(":core:platform")

// Data
include(":data:transactions")
include(":data:categories")
include(":data:accounts")
include(":data:settings")
include(":data:backup")
include(":data:budgets")

// Features
include(":feature:transactions")
include(":feature:transactionEdit")
include(":feature:overview")
include(":feature:categories")
include(":feature:settings")
include(":feature:security")
include(":feature:onboarding")