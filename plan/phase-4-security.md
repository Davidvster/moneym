# Phase 4 — Security (PIN + biometrics)

**Status**: ✅ Done

Goal: implement optional PIN locking with biometric unlock, including app-lock lifecycle (cold launch + background timeout). PIN is opt-in per ADR-004 (Q4); biometric only available if PIN is set.

**Exit criteria**: enable PIN → restart → enter PIN; enable biometric → unlock with biometric (PIN fallback works); background app > N seconds → foreground → locked; recent-apps view shows blurred/blanked screen.

## Steps (will expand when starting)

- [ ] **4.1** `core:security` — real `SecureStore.android.kt` backed by Android Keystore + EncryptedSharedPreferences (replace Phase 1 stub)
- [ ] **4.2** `core:security` — real `SecureStore.ios.kt` backed by Keychain (replace Phase 1 stub)
- [ ] **4.3** `core:security` — biometric-bound storage variants (`requireBiometric = true` path)
- [ ] **4.4** `core:security` — `BiometricAuthenticator` expect/actual with prompt spec (Android `BiometricPrompt`, iOS `LAContext.evaluatePolicy`)
- [ ] **4.5** `feature:security` — `PinSetupScreen` (enter twice, validation, save)
- [ ] **4.6** `feature:security` — `PinUnlockScreen` (entry, attempt counter + exponential backoff)
- [ ] **4.7** `feature:security` — biometric prompt + fallback flow
- [ ] **4.8** `composeApp` — `AppLockController` observing lifecycle; routes to unlock when needed; honors `pref.background_lock_seconds`
- [ ] **4.9** `feature:settings` — Security section: enable PIN, change PIN, enable biometric, set background-lock duration
- [ ] **4.10** Recent-apps blur (Android `FLAG_SECURE` opt-in; iOS snapshot overlay in `applicationWillResignActive`)
- [ ] **4.11** Tests: `PinHasher` (already in Phase 1), `SecureStore` round-trip tests, attempt-counter backoff, lock-on-background timing
- [ ] **4.12** Verify: full PIN/biometric lifecycle on real device (emulator/simulator biometric APIs work but real device gives confidence)

---

## Concrete implementation notes (for fresh sessions)

### What already exists in core:security

**`SecureStore.kt`** — interface only, no implementation:
```
core/security/src/commonMain/kotlin/com/dv/moneym/core/security/SecureStore.kt
```
```kotlin
interface SecureStore {
    suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean = false)
    suspend fun get(key: String): ByteArray?
    suspend fun remove(key: String)
}
```

**`PinHasher.kt`** — expect class defined, Android actual uses PBKDF2-HMAC-SHA256 (javax.crypto, 600k iterations), iOS actual is a deterministic stub (not secure, marked TODO Phase 4). The `HashedPin` data class stores `algorithm`, `iterations`, `salt`, `digest` — field layout is backward-migration-safe.

**`core/security/build.gradle.kts`** already has:
- `androidMain.dependencies { androidx-biometric, androidx-security-crypto, androidx-activity-compose }`
- `kotlinx-coroutines-core` in commonMain

### What to implement in 4.1–4.3: SecureStore Android

Use `EncryptedSharedPreferences` backed by Android Keystore:

```kotlin
// androidMain
actual class SecureStoreImpl actual constructor() : SecureStore {
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context, "moneym_secure_prefs",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    // ...
}
```

`EncryptedSharedPreferences` needs `Context`. Use Koin Android context: `single<SecureStore> { SecureStoreImpl(androidContext()) }` in `composeApp/src/androidMain/di/AndroidPlatformModule.kt` (same file that registers `SqlDriverFactory`).

For biometric-bound keys (step 4.3), use `KeyGenParameterSpec.Builder` with `setUserAuthenticationRequired(true)` — separate KeyStore key per protected item. Only `requireBiometric = true` items use this; normal items use `EncryptedSharedPreferences`.

### What to implement in 4.2: SecureStore iOS

Uses Keychain Services via cinterop (`platform.Security.*`):

```kotlin
// iosMain
actual class SecureStoreImpl actual constructor() : SecureStore {
    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) {
        val query = CFDictionaryCreateMutable(null, 5, null, null).apply {
            CFDictionaryAddValue(this, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(this, kSecAttrAccount, CFStringCreateWithCString(null, key, kCFStringEncodingUTF8))
            // ...
            if (requireBiometric) {
                val access = SecAccessControlCreateWithFlags(null, kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly, kSecAccessControlBiometryCurrentSet or kSecAccessControlPrivateKeyUsage, null)
                CFDictionaryAddValue(this, kSecAttrAccessControl, access)
            }
        }
        SecItemDelete(query)
        SecItemAdd(query, null)
    }
}
```

The iOS actual doesn't need Context (Keychain is process-scoped). Register via `iosPlatformModule()` in `composeApp/src/iosMain/di/IosPlatformModule.kt`.

### What to implement in 4.4: BiometricAuthenticator

```kotlin
// commonMain
interface BiometricAuthenticator {
    suspend fun authenticate(reason: String): BiometricResult
}
sealed interface BiometricResult {
    data object Success : BiometricResult
    data object UserCancelled : BiometricResult
    data class Error(val message: String) : BiometricResult
}
expect class BiometricAuthenticatorImpl() : BiometricAuthenticator
```

**Android actual**: wrap `BiometricPrompt` in a `suspendCancellableCoroutine`. Needs `FragmentActivity` context. Get from Koin via `single { BiometricAuthenticatorImpl(get<Activity>() as FragmentActivity) }` — or use `LocalContext.current` in Compose and pass down.

**iOS actual**: wrap `LAContext.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)` in a coroutine.

### PinHasher iOS stub — replace in 4.1

The existing stub in `core/security/src/iosMain/.../PinHasher.ios.kt` logs a warning. Replace with real CommonCrypto PBKDF2:

```kotlin
// iosMain
import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.Security.*

actual class PinHasher actual constructor() {
    actual fun hash(pin: String): HashedPin {
        val salt = ByteArray(16)
        salt.usePinned { SecRandomCopyBytes(null, 16.convert(), it.addressOf(0)) }
        val digest = pbkdf2(pin, salt, ITERATIONS)
        return HashedPin(ALGORITHM, ITERATIONS, salt, digest)
    }
    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val pinBytes = pin.encodeToByteArray()
        val result = ByteArray(32)
        result.usePinned { rp ->
            pinBytes.usePinned { pp ->
                salt.usePinned { sp ->
                    CCKeyDerivationPBKDF(kCCPBKDF2, pp.addressOf(0), pinBytes.size.convert(),
                        sp.addressOf(0), salt.size.convert(), kCCPRFHmacAlgSHA256,
                        iterations.convert(), rp.addressOf(0), 32.convert())
                }
            }
        }
        return result
    }
    companion object { const val ALGORITHM = "PBKDF2WithHmacSHA256"; const val ITERATIONS = 600_000 }
}
```

### AppLockController pattern (step 4.8)

Goes in `composeApp/src/commonMain/kotlin/com/dv/moneym/AppLockController.kt`.

State machine:
- `Locked` (show unlock screen) / `Unlocked` (show main nav)
- On app start: check `pref.pin_enabled`; if true, start `Locked`
- On foreground: if `now - backgroundAt > pref.background_lock_seconds`, → `Locked`
- On background: record timestamp via `pref.last_background_at`

Integrate into `App.kt` by wrapping the existing `AppScreen` navigation:

```kotlin
@Composable
private fun AppContent() {
    val lockState = ... // collectAsState from AppLockController
    if (lockState == AppLockState.Locked) {
        PinUnlockScreen(onUnlocked = { controller.unlock() })
    } else {
        // existing navigation shell
    }
}
```

Platform lifecycle:
- **Android**: `ProcessLifecycleOwner.get().lifecycle` in `MainActivity` or `composeApp/androidMain`
- **iOS**: `UIApplicationDelegate.applicationDidEnterBackground` callback — add to `iosApp/iosApp/iOSApp.swift`

### AppScreen navigation — current state

Phase 3 implemented simple `AppScreen` sealed class in `composeApp/src/commonMain/kotlin/com/dv/moneym/App.kt`:

```kotlin
sealed interface AppScreen {
    object Transactions : AppScreen
    data class TransactionEdit(val id: TransactionId?) : AppScreen
    object Overview : AppScreen
    object Settings : AppScreen
}
```

Add `object PinUnlock` or `object PinSetup` to this sealed class. The lock screen sits outside the bottom-nav scaffold.

### ViewModel test pattern (established in Phase 3)

Every ViewModel test needs `Dispatchers.setMain` on iOS or it has timing failures:

```kotlin
private val testDispatcher = StandardTestDispatcher()

@BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
@AfterTest fun tearDown() { Dispatchers.resetMain() }
```

Use `runTestWithDispatchers` from `core:testing` for coroutine body.

### Known version / import facts

- `kotlinx-datetime: 0.7.0` — use `kotlin.time.Instant` and `kotlin.time.Clock` (NOT `kotlinx.datetime.Instant/Clock`)
- `kotlinx.datetime.LocalDate`, `LocalDateTime`, `TimeZone`, `todayIn` still from `kotlinx.datetime`
- Material icons dep: `org.jetbrains.compose.material:material-icons-core:1.7.3` (NOT the compose-multiplatform version number)
- `Icons.AutoMirrored.Filled.ArrowBack/List/KeyboardArrowLeft/Right` (not `Icons.Filled.*` — deprecated)
- SQLDelight data modules: plain `androidTarget()` — NOT `androidTarget { compilerOptions { ... } }` (incompatible with SQLDelight 2.0.x)
- SQLDelight .sq files: `src/commonMain/sqldelight/com/dv/moneym/data/<module>/TableName.sq`
- Generated types: `com.dv.moneym.data.<module>.TableName` (NOT `.db.TableName`)
- Expose `internal` impls via public factory fun (e.g. `createCategoryRepository(db, dispatchers)`) — `composeApp` cannot access `internal` from other modules
- `compose.*` DSL deprecated — use `libs.compose.*` in build files
- Add `implementation(libs.kotlin.test)` explicitly to every module's `commonTest` (not transitively reliable via core:testing)
- `data class` IDs (not `value class`) — `@JvmInline` not available on Kotlin/Native with this Kotlin version

### Koin module wiring pattern

Platform-specific modules live in:
- `composeApp/src/androidMain/kotlin/com/dv/moneym/di/AndroidPlatformModule.kt`
- `composeApp/src/iosMain/kotlin/com/dv/moneym/di/IosPlatformModule.kt`

Both return a `Module` passed to `App(platformModules = listOf(...))` in `MainActivity`/`MainViewController`.

`AppModules.kt` in `composeApp/commonMain/di/` lists all shared modules. After Phase 3 it contains: `coreCommonModule`, `coreDatastoreModule`, `dataCategoriesModule`, `dataAccountsModule`, `dataTransactionsModule`, `featureTransactionsModule`, `featureTransactionEditModule`.

Add `coreSecurityModule` and `featureSecurityModule` here in Phase 4.
