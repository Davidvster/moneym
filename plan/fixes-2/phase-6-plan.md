# Phase 6 — iPad transaction amount keyboard

## Symptom
On iPad, focusing the transaction amount field opens a "tiny num keyboard"; focusing the note field then back on amount opens the normal full keyboard.

## Background
`core/ui/.../MmAmountInput.kt` uses `KeyboardType.Decimal` → UIKit `decimalPad`. Native iPadOS renders decimalPad as a full-width keyboard with large digit keys (no dedicated numpad on iPad) — a tiny/floating pad is NOT the native presentation. Suspect Compose Multiplatform mapping artifact or floating-keyboard state.

## Steps
1. Run app on an iPad simulator, focus the amount field, screenshot the keyboard.
2. Focus note → back to amount, screenshot again; compare.
3. If degraded rendering confirmed, change amount field keyboard on iPad only (platform-aware keyboard type) keeping iPhone/Android `Decimal`.
4. Report expected-native-behavior answer to the user.

## Result (2026-06-12, iPad Pro 11 M4 simulator)
- Amount field (`KeyboardType.Decimal`) → full-width numeric keyboard. Note field → full QWERTY. Both correct; "tiny keyboard" NOT reproducible.
- Expected native iPadOS behavior: decimal pad renders as a full-width keyboard — finance apps universally use it. `Decimal` is the right type.
- The reported tiny numpad matches the iPadOS *floating keyboard* state (pinch-in on the keyboard minimizes it; state persists per device). Fix on device: pinch-out / two-finger spread on the floating keyboard to re-dock.
- No code change.
