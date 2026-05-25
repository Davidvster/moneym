# Phase 8.1 — Backup crypto envelope

Adds reusable AES-256-GCM + PBKDF2-HMAC-SHA256 encryption with a serializable envelope. The crypto lives in `core/security` so any module (local backup, remote backup, future export-anywhere) can consume it.

## Files

- `core/security/.../BackupCrypto.kt` — interface, `EncryptedBackup` envelope, `KdfParams`, `CipherParams`, `BackupCryptoError`, constants.
- `core/security/.../DefaultBackupCrypto.kt` — single common implementation using `dev.whyoleg.cryptography` (`JDK` provider on Android, `Apple` provider on iOS).
- `core/security/.../BackupEnvelopeJson.kt` — `Json` codec for the envelope (used by the remote backup pipeline in Phase 8.4).
- `core/security/.../BackupCryptoBase64.kt` — internal base64 helpers.
- `commonTest/.../BackupEnvelopeSerializationTest.kt` — schema round-trip + forward-version check.
- `androidUnitTest/.../BackupCryptoRoundTripTest.kt` — real encrypt/decrypt + wrong-passphrase failure path. Iterations reduced to 1 000 for test speed via an internal constructor knob.

## Dependencies added

`cryptography-kotlin` 0.5.0:

- `dev.whyoleg.cryptography:cryptography-core` (common)
- `dev.whyoleg.cryptography:cryptography-provider-jdk` (android)
- `dev.whyoleg.cryptography:cryptography-provider-apple` (ios)

Tracked in `gradle/libs.versions.toml` under `cryptographyKotlin`.

Reason for the library: Kotlin/Native's bundled `platform.CoreCrypto` cinterop exposes `CCCryptorCreateWithMode` but **not** the `CCCryptorGCM*` family. Rather than ship a custom cinterop `.def`, we use cryptography-kotlin which already wraps Apple's `AES.GCM` and `PBKDF2`.

## Verification

```
./gradlew :core:security:compileDebugKotlinAndroid \
          :core:security:linkDebugFrameworkIosSimulatorArm64 \
          :core:security:testDebugUnitTest
```

All green.

## Notes for future phases

- The envelope format is **v1**. Phase 8.4 stores envelopes as the actual blob uploaded to Drive (after JSON-encoding the envelope alongside the ciphertext bytes — final shape decided in 8.4).
- `DefaultBackupCrypto` is **only** wired in Koin in Phase 8.6; this phase ships unused code that compiles and is independently testable.
