# Phase 8 — Polish & M3 Removal

Final cleanup after all screens are migrated. Remove M3 fallbacks, fix edge cases, add animations.

---

## Goals
- Remove all residual `MaterialTheme.*` token calls from feature screens.
- Remove the `MaterialTheme` wrapper from `MoneyMTheme` (it was kept as a shim during migration).
- Fix any platform-specific rendering differences (iOS vs Android).
- Wire up remaining interactions noted in the design spec.

---

## Tasks

### Remove M3 shim
- `MoneyMTheme.kt`: delete the inner `MaterialTheme(…)` wrapper. Keep only `CompositionLocalProvider`.
- Verify: grep the codebase for `MaterialTheme.colorScheme` and `MaterialTheme.typography` — should return zero results in feature/ui files after this step.
- Keep: `ModalBottomSheet`, `DatePickerDialog`, `AlertDialog` from M3 — those are acceptable exceptions (no good KMP-native alternative).

### Animation pass
- **Segmented control**: `animateFloatAsState` for selected pill X offset, `spring(dampingRatio=0.8)`.
- **Toggle**: `animateFloatAsState` for thumb X, `spring(stiffness=400f)`.
- **PIN shake**: `Animatable` keyframe shake (already spec'd in Phase 3 — verify it's implemented).
- **Tab bar active indicator**: crossfade icon stroke weight on tab switch.
- **Sheet**: `ModalBottomSheet` enter/exit handled by Compose — no extra work.

### Edge cases
- **Empty states**: Transactions list with no data → centered illustration (category icon + "No transactions yet" in `text3`). Overview with no data → zeroed cards, empty charts.
- **Long category names**: chip text truncates with `maxLines=1, overflow=Ellipsis`. Category name in transaction row truncates with `maxLines=1`.
- **Very large amounts**: `MmMoney` with 7+ digits — test `1,234,567.89` renders without overflow.
- **RTL**: Arabic language — verify `LayoutDirection.Rtl` doesn't break `Row` ordering. Use `Modifier.padding(start/end)` not `left/right`.

### iOS-specific
- Remove any lingering ripple indicators on iOS — check `LocalIndication` is `null` for custom interactive elements.
- Status bar: iOS home indicator (bottom safe area) — verify `MmTabBar` 24dp bottom padding is adequate; use `WindowInsets.navigationBars` for dynamic safe area.
- Verify Geist font renders on iOS simulator — if iOS font loading via `UIFont` is needed, implement `iosMain/Fonts.ios.kt`.

### Android-specific
- Edge-to-edge: verify `enableEdgeToEdge()` is called in `MainActivity`. `MmTabBar` bottom padding should consume `WindowInsets.navigationBars`.
- Predictive back gesture: verify transaction edit sheet dismisses on back swipe without crash.

### Onboarding screen
- The onboarding flow uses the new `MmButton`, `MmField` components.
- Minor layout pass to match new type/spacing tokens — not a full redesign.

### String resources audit
- After screen rewrites, verify no hardcoded strings remain in feature UI files.
- Run `grep -r '"[A-Z]' feature/*/ui/` to catch any leftover hardcoded labels.

---

## Verification checklist
- [ ] `grep -r "MaterialTheme.colorScheme\|MaterialTheme.typography" feature/` → 0 results
- [ ] All 13 screens render correctly in light mode on Android emulator
- [ ] All 13 screens render correctly in dark mode
- [ ] iOS build compiles and all screens render (fonts load correctly)
- [ ] Segmented and Toggle animations are smooth (no jank)
- [ ] Empty states shown when no data
- [ ] Edge-to-edge correct on Android (nav bar not obscuring tab bar)
- [ ] Safe area correct on iOS (bottom home indicator not obscuring tab bar)
