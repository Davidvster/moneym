# Phase 4: App restart after local backup restore

## What changes

### Android: DbPlatform.android.kt
File: `core/platform/src/androidMain/kotlin/com/dv/moneym/platform/DbPlatform.android.kt`

`DbPlatform` has `context: Context` (already available). Replace `terminateApp()`:

Current:
```kotlin
actual fun terminateApp() { exitProcess(0) }
```

Replace with:
```kotlin
actual fun terminateApp() {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) }
    if (intent != null) {
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .set(AlarmManager.RTC, System.currentTimeMillis() + 500L, pending)
    }
    exitProcess(0)
}
```

Add imports:
- `import android.app.AlarmManager`
- `import android.app.PendingIntent`
- `import android.content.Intent`

### iOS: DbPlatform.ios.kt
No change — programmatic restart is not possible on iOS without App Store violations.

## Build verification
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
