# Phase 6 Plan: Settings, Bottom Sheet, System UI, and Sync Icon Polish

## Goal

Fix user items 3, 6, and 7:

- Lock-after bottom sheet should behave like the theme picker: tapping an option immediately applies it, with only a checkmark on the current choice and no OK/Cancel buttons.
- Android dark-mode modal bottom sheets should not make the top system/status bar unreadable.
- Cloud sync icon should use thinner arrows and a larger cloud shape that fills the icon bounds more like the search/filter icons.

Commit after verification.

## Expected Files/Modules

- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/components/LockTimeoutPickerDialog.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/components/AppearanceSection.kt` as the visual reference.
- `feature/settings/src/commonTest/kotlin/com/dv/moneym/feature/settings/overview/SecuritySettingsViewModelTest.kt` if behavior tests need updates.
- `shared/src/androidMain/kotlin/com/dv/moneym/SystemBarStyleEffect.android.kt`
- `shared/src/commonMain/kotlin/com/dv/moneym/App.kt` only if the system bar effect API needs a small shared-state change.
- `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmDeleteSheet.kt`
- `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmCategoryPickerSheet.kt`
- `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmWalletPickerSheet.kt`
- Other `ModalBottomSheet` call sites only if needed for a consistent centralized fix.
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/components/CloudSyncIcon.kt`
- `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmIcons.kt`
- `plan/overview-ai/status.md`

## Implementation Notes

### Lock-After Sheet

- Match `ThemePickerSheet` behavior and style:
  - rounded top shape
  - custom drag handle
  - `dragHandle = null`
  - check icon for the current option
  - tap an option calls `onConfirm(seconds)` immediately
  - no local pending selected state
  - no OK/Cancel buttons
- The parent already controls dismissal; if needed, parent should close after `onConfirm`.
- Remove unused string imports only. Do not delete localized strings unless all 28 resource files are handled in the same change.

### Android System Bar With Bottom Sheets

- Inspect the existing `SystemBarStyleEffect(isDark)` in `shared`.
- Make status bar icon appearance remain legible in dark mode while a Material modal bottom sheet/scrim is visible.
- Prefer a centralized, low-risk fix in the Android system bar effect or shared sheet styling rather than one-off hacks per screen.
- Also set navigation bar appearance if it is missing and relevant.
- Verify the logic still preserves light mode legibility.

### Cloud Sync Icon

- Update both the animated canvas icon and the static `Icon.CloudSync` vector so idle and syncing visuals match.
- Make the cloud occupy more of the 24dp viewport.
- Make the arrow strokes slightly thinner than the cloud stroke, or otherwise visually lighter.
- Keep icon semantics/content descriptions unchanged.

## Tests / Verification

- Run:

```bash
./gradlew --no-configuration-cache :feature:settings:testDebugUnitTest :feature:transactions:testDebugUnitTest :shared:compileDebugKotlinAndroid
```

- Run:

```bash
git diff --check
```

- If tests cannot visually cover the icon/system-bar work, add a brief note to `status.md` explaining the manual/structural verification.

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md` with:

- Phase 6 status and verification command results.
- The commit hash after committing.

Commit only Phase 6 changes.
