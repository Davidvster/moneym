# Phase 4 — dev/prod app separation

Decision (user): debug buildType IS the dev app.

## Android (done in this phase)

- `androidApp/build.gradle.kts` debug buildType: `applicationIdSuffix = ".dev"`, `versionNameSuffix = "-dev"`.
- `androidApp/src/debug/res/values/strings.xml`: `app_name` = "MoneyM Dev" (debug source set overrides main; avoids resValue duplicate-resource conflict).

## iOS (deferred until after Phase 5)

Changing the Debug bundle id now would replace the signed-in app on the iPhone 16e simulator needed for the Drive-restore investigation. After phase 5:
- `iosApp/Configuration/Config.xcconfig`: `PRODUCT_BUNDLE_IDENTIFIER[config=Debug] = com.dv.moneym.MoneyM$(TEAM_ID).dev` + Debug display name "MoneyM Dev".

## Manual steps required (USER)

Google OAuth is bound to package/bundle id:
- **Android:** in Google Cloud console, add an OAuth client of type Android for package `com.dv.moneym.dev` with the debug keystore SHA-1. The web `serverClientId` in `local.properties` stays the same.
- **iOS:** the `.dev` bundle id needs its own iOS OAuth client if Google sign-in should work in dev builds; `Secrets.xcconfig` `GID_CLIENT_ID` would then need a Debug-conditional value.
