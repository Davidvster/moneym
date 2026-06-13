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

include(":shared")
include(":androidApp")

// Core
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:ui-graphs")
include(":core:datastore")
include(":core:security")
include(":core:navigation")
include(":core:testing")
include(":core:platform")
include(":core:utils")
include(":core:ai")

// Data
include(":data:transactions")
include(":data:categories")
include(":data:accounts")
include(":data:settings")
include(":data:backup")
include(":data:budgets")
include(":data:remotebackup")
include(":data:sync")
include(":data:banksync")
include(":data:walletsync")
include(":feature:walletsync")
include(":data:llmmodels")
include(":data:aichat")
include(":core:oauth")

// Features
include(":feature:transactions")
include(":feature:transactionEdit")
include(":feature:overview")
include(":feature:categories")
include(":feature:budgets")
include(":feature:settings")
include(":feature:security")
include(":feature:onboarding")
include(":feature:infopage")
include(":feature:about")
include(":feature:sync")
include(":feature:banksync")
include(":feature:aianalysis")
include(":feature:aimodels")