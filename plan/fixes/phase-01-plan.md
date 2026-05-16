# Phase 1: Biometrics

## Problem
Biometrics don't work. The Android impl needs a `FragmentActivity` reference and the iOS impl needs the LAContext to be used at call time. The PIN unlock screen does show a biometric button when `biometricAvailable == true`, but the issue is that `BiometricAuthenticatorImpl.activityRef` must be set, MainActivity does this, and the isAvailable check fails in the Settings screen on first init before the activity is fully set. There is also no FR/Face-ID icon difference — the button shows a static fingerprint icon regardless of which biometric is available.

## Files to modify
- `feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinUnlockScreen.kt` — verify biometric button triggers correctly; pass `onBiometric` only when `state.biometricAvailable`
- `feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/presentation/PinUnlockViewModel.kt` — check biometric availability is refreshed; biometric button already calls `triggerBiometric()`
- `core/security/src/androidMain/kotlin/com/dv/moneym/core/security/BiometricAuthenticatorImpl.android.kt` — ensure `isAvailable` is not cached and is evaluated fresh; add `BIOMETRIC_WEAK` as fallback
- `core/security/src/iosMain/kotlin/com/dv/moneym/core/security/BiometricAuthenticatorImpl.ios.kt` — fix: LAContext needs to be recreated fresh for each `isAvailable` call; add `LABiometryType` detection to distinguish Face ID vs Touch ID
- `core/security/src/commonMain/kotlin/com/dv/moneym/core/security/BiometricAuthenticator.kt` — add `val biometryType: BiometryType` to the interface
- `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmIcons.kt` — check if `MmIcons.faceId` or similar icon exists; add if needed
- `feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinKeypad.kt` — the biometric button icon should vary based on biometry type (face vs fingerprint)
- `feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/presentation/PinUnlockUiState.kt` — add `biometryType` field

## Implementation steps

1. Add `BiometryType` enum to `BiometricAuthenticator.kt`:
   ```kotlin
   enum class BiometryType { Fingerprint, FaceId, None }
   ```
   Add `val biometryType: BiometryType` to the `BiometricAuthenticator` interface.

2. Update Android impl (`BiometricAuthenticatorImpl.android.kt`):
   - In `isAvailable`, also try `BIOMETRIC_WEAK` as fallback using `or`:
     ```kotlin
     mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
     ```
   - `biometryType` always returns `BiometryType.Fingerprint` on Android (can't reliably detect Face ID vs fingerprint without API 30+; use Fingerprint as safe default).

3. Update iOS impl (`BiometricAuthenticatorImpl.ios.kt`):
   - Create a fresh `LAContext()` for each `isAvailable` call to avoid caching.
   - Detect biometry type using `LAContext().biometryType`:
     - `LABiometryTypeFaceID` → `BiometryType.FaceId`
     - else → `BiometryType.Fingerprint`

4. Update `PinUnlockUiState.kt`: add `biometryType: BiometryType = BiometryType.Fingerprint`.

5. Update `PinUnlockViewModel.kt`: read `biometricAuth.biometryType` and store in state.

6. Update `PinKeypad.kt`: accept `biometryType` param and show face-id icon vs fingerprint icon accordingly. Check `MmIcons` for available icons.

7. Update `PinUnlockScreen.kt` to pass `biometryType` to `PinKeypad`.

8. Check `MmIcons.kt` and add a `faceId` icon vector if missing (using a simple face-square icon from Material Icons).

## Acceptance criteria
- [ ] On Android, biometrics prompt opens (fingerprint / face) when the biometric button is tapped on the PIN unlock screen
- [ ] On iOS, Face ID prompt opens on devices with Face ID; fingerprint (Touch ID) on older devices
- [ ] Biometric button icon matches the biometry type (face icon for Face ID, fingerprint icon for Touch ID/fingerprint)
- [ ] `isAvailable` returns `false` (and button is hidden) when no biometric hardware or when biometrics are not enrolled
- [ ] After successful biometric auth, the app unlocks
