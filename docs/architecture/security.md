# Security

Scope of v1: optional PIN locking, optional biometric unlock (only available if a PIN is set), and an OS-level secure store for the PIN hash. Everything security-related lives in `core:security` so we have one audit surface.

## Threat model (v1)

- **In scope**: casual device sharing, lost-but-locked phones, opportunistic snooping by someone who picks up an unlocked device for a minute.
- **Out of scope**: forensic extraction of a rooted/jailbroken device, attackers with physical access plus debugger, side-channel attacks on the OS keystore.
- **Implication**: we lean on the OS keystore for everything. We do not implement custom crypto. We do not encrypt the SQLDelight database in v1 (Android keystore-backed SQLCipher is a future option; iOS gets disk-level encryption for free).

## PIN flow

### Setup

1. User enables PIN in Settings (or during onboarding).
2. User enters 4–6 digit PIN twice.
3. We generate a random 16-byte salt.
4. We compute `hash = PBKDF2-HMAC-SHA256(pin || salt, iterations = 600_000)` — 32-byte digest. The algorithm name, iteration count, and salt are stored alongside the digest so we can migrate to Argon2id later without forcing a re-PIN (see ADR-010).
5. We store `(salt, hash, algorithm, params)` as a serialized record in the OS keystore (see "Secure storage" below). Plain prefs do **not** see the hash.
6. We flip `pref.pin_enabled = true`.

### Verification

1. User enters PIN.
2. We read the stored record, recompute the hash with the same salt/params, and compare in constant time.
3. On success: set the in-memory `Unlocked` flag, advance navigation.
4. On failure: increment a stored attempt counter; after N failed attempts (default 5) introduce backoff (1s, 5s, 30s, 5min, 30min).

### Disable

User must enter the current PIN to disable. Then we delete the keystore record and set `pref.pin_enabled = false`.

## Biometrics

Biometric is **never** the source of truth — it gates access to the PIN hash record in the keystore.

### Setup

1. Only available if `pin_enabled = true`.
2. User toggles "Use biometric unlock". We mark the PIN's keystore record as biometric-bound:
   - **Android**: re-store the record with `KeyGenParameterSpec` requiring `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)`.
   - **iOS**: re-store the keychain item with `kSecAttrAccessControl = SecAccessControlCreateWithFlags(..., .biometryCurrentSet, ...)`.
3. `pref.biometric_enabled = true`.

### Verification

1. User taps "Unlock with biometric".
2. Platform shows the system biometric prompt (`BiometricPrompt` on Android, `LAContext.evaluatePolicy` on iOS).
3. On success, the OS unlocks the keystore record; we read it and consider the user authenticated without entering the PIN.
4. On failure or cancel, fall back to the PIN entry screen.

### Edge cases

- **New biometric enrolled**: `setInvalidatedByBiometricEnrollment` on Android wipes the key. We treat this as "biometric disabled" and ask the user to re-enable, requiring a PIN entry first. iOS `.biometryCurrentSet` has the same semantics.
- **No biometric hardware**: toggle is hidden.
- **Biometric locked out** (too many failed attempts at OS level): fall through to PIN.

## Secure storage abstraction

`core:security` defines:

```kotlin
interface SecureStore {
    suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean = false)
    suspend fun get(key: String, biometricPrompt: BiometricPromptSpec?): ByteArray?
    suspend fun remove(key: String)
}
```

**Android** (`actual`): backed by Android Keystore (an AES-GCM key whose key material lives in the Trusted Execution Environment), wrapping values stored in `EncryptedSharedPreferences`. Biometric-gated keys use `setUserAuthenticationRequired(true)`.

**iOS** (`actual`): backed by Keychain Services. Biometric-gated items use `SecAccessControl` with `.biometryCurrentSet`. The `BiometricPromptSpec` carries the localized reason string passed to `LAContext.evaluatePolicy`.

The interface is intentionally narrow — anything that needs more (e.g. asymmetric crypto, attestation) is a sign we've outgrown v1 scope.

## App lock lifecycle

Implemented in `composeApp`'s root, observing both the navigation graph and the platform lifecycle:

- Cold launch and `pin_enabled = true` → route to unlock screen before the main graph.
- Going to background → record timestamp.
- Returning to foreground:
  - If `now - background_timestamp > pref.background_lock_seconds`, route to unlock.
  - Otherwise resume.
- Default `background_lock_seconds = 30`. User-configurable in settings (`0` = always lock, `60`, `300`, etc.).
- The unlock screen sits **above** the main navigation stack so the user returns exactly where they were after unlocking. We also blur/blank the previous screen in the recent-apps switcher (Android: `FLAG_SECURE` opt-in; iOS: cover with a snapshot view in `applicationWillResignActive`).

## What we don't do (and why)

- **No database encryption in v1.** Both platforms encrypt their app sandbox at the OS level when a device passcode is set. SQLCipher is the future hook; it requires deriving a DB key from the PIN, which complicates the unlock flow.
- **No custom crypto.** PBKDF2-HMAC-SHA256 via the platform's standard crypto APIs only. No "let's hash with SHA256" shortcuts. The PBKDF2 implementation lives in `core:security` as a small pure-Kotlin function backed by `javax.crypto` on Android and CommonCrypto on iOS.
- **No "remember PIN for X minutes" in settings.** That's what biometrics is for.
- **No cloud-stored secrets.** When cloud backup arrives, exports will not include the PIN material or any keystore-bound data.

## Files

- `core/security/.../SecureStore.kt` — interface
- `core/security/.../SecureStore.android.kt` — Android Keystore actual
- `core/security/.../SecureStore.ios.kt` — Keychain actual
- `core/security/.../PinHasher.kt` — PBKDF2-HMAC-SHA256 wrapper (ADR-010); records algorithm + params for forward-migration
- `core/security/.../BiometricAuthenticator.kt` — expect/actual prompt
- `feature/security/...` — PIN setup/unlock screens
