# Phase 1 — Core UI Component Library

Build every reusable primitive in `core/ui`. All components use `MM.*` tokens (Phase 0). No M3 components except `ModalBottomSheet` (for sheets) and `Surface` as a plain container.

---

## Goal
`core/ui` goes from empty to a complete component library matching the design spec. Every feature screen in later phases imports from here — never re-implements.

---

## Files to create

All under `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/`

### Primitives

**`MmButton.kt`**
```
Variants: primary | secondary | ghost | outline | accent | danger
Sizes: sm (32dp) | md (44dp) | lg (52dp)
Shape: RoundedCornerShape(12.dp)
Leading icon slot (8dp gap).
Disabled: surface2 bg, text3 fg, 1dp border, no interaction.
No ripple on iOS — use pointerInput + scale animation instead.
```

**`MmIconButton.kt`**
```
40dp hit target, transparent bg, no border.
Variants: default (text color) | accent | danger.
```

**`MmSegmented.kt`**
```
Pill track (36dp tall, or 32dp for size=sm).
3px inner padding. surface2 bg. pill radius.
Selected pill: bg color, 1dp shadow, SemiBold label.
Unselected: text2 color, Medium label.
Animated pill position via Animatable<Float>.
```

**`MmChip.kt`**
```
Pill, 34dp tall. transparent bg + border outline (default).
Selected: text bg + inverted fg.
Leading slot: color dot OR CategoryIcon tile.
```

**`MmField.kt`**
```
52dp tall (auto-height if multiline).
12dp radius, surface bg, 1dp border.
Label rendered ABOVE the box as micro text2 — NOT floating inside.
prefix / suffix in text3.
```

**`MmToggle.kt`**
```
44×26dp track, pill shape.
Off: surface2 bg + border outline.
On: text bg + text outline.
Thumb: 20dp circle in bg color.
Disabled: 45% opacity, no interaction.
Animated thumb via Animatable<Float>.
```

**`MmRow.kt`**
```
Default: 12dp gap, 56dp min height, 14×20dp padding (configurable).
divider param: 1dp bottom divider line in divider color.
Last-row callers pass divider=false.
```

**`MmCard.kt`**
```
surface bg, 16dp radius, 1dp border.
padded: Boolean param adds 20dp padding inside.
Shadow: tiny (1dp blur) — use Modifier.shadow(1.dp, shape, …).
```

**`MmTabBar.kt`**
```
Row of 3 tabs. 1dp divider top. bg background. 24dp bottom padding (safe area). 8dp top padding.
Tab: Column { Icon(22dp) + Text(10sp 500) }, 4dp gap.
Active: text color, strokeWidth 1.8, weight 600.
Inactive: text3 color, strokeWidth 1.5, weight 500.
NO Material NavigationBar.
```

**`MmMoney.kt`**
```
@Composable fun MmMoney(value: Double, sign: String = "", size: TextUnit, weight: FontWeight, modifier: Modifier = Modifier, color: Color = MM.colors.text)
Renders: "{sign}{currency} {value}" with bodyMono style (GeistMono, tnum).
Negative sign uses U+2212 (− mathematical minus), not ASCII hyphen.
Always 2 decimal places.
```

**`CategoryIconTile.kt`**
```
@Composable fun CategoryIconTile(category: Category, size: Dp, variant: IndicatorStyle)
variant=IconTile: filled color square, radius=size*0.30, white icon at size*0.55.
variant=SoftIcon: 13% alpha bg, full-opacity colored icon.
variant=Bar: 4dp × size vertical bar.
variant=Dot: 8×8dp circle.
```

**`SectionLabel.kt`** — `micro` style, text3 color, uppercase at call site.

**`ScreenHeader.kt`** — Back chevron + centered title + optional trailing slot. 1dp divider bottom.

### Charts

**`DonutChart.kt`**
```
Canvas { drawArc per slice, rotated -90°, useCenter=false, style=Stroke(18.dp) }
No center label — total lives in legend.
```

**`CumulativeChart.kt`**
```
Canvas drawing: area path (text @ 6% opacity fill), line (text, 1.5dp stroke), 3 dashed horizontal gridlines (25/50/75%), dashed vertical "today" line (text3), 4dp dot at today.
Compose Path API — no SVG.
X-axis labels: 1, 8, 15, 22, 31 in captionMono text3.
```

**`MiniBars.kt`**
```
Canvas { per-bar drawRoundRect }. 26dp tall.
Zero-value bars: 1.5dp height, 18% opacity.
Non-zero: 70% opacity. highlight index: 100%.
```

### Icon set

**`MmIcons.kt`**
```
object MmIcons — one ImageVector per icon, built via
  ImageVector.Builder { path { … } }
All SVG paths extracted from docs/design/source/ui-primitives.jsx Icon component.
Icons needed (30+):
  plus, close, search, chevronLeft, chevronRight, chevronDown, check,
  trash, backspace, faceId, fingerprint, lock, sun, moon, sliders,
  globe, list, download, folder, arrowUp, arrowDown, info,
  heart, film, car, bolt, basket, utensils, home, bag, tag,
  banknote, gift
```

---

## Key implementation notes

- All components call `MM.colors.*` / `MM.type.*` — no hardcoded colors or sizes.
- No `clickable { }` on iOS path — use `pointerInput(Unit) { detectTapGestures { … } }` so no ripple shows on iOS. Android ripple acceptable but keep subtle via `indication = rememberRipple(bounded = true, color = MM.colors.text.copy(alpha = 0.06f))`.
- `MmTabBar` takes an `activeTab: TabRoute` param and an `onTabSelected: (TabRoute) -> Unit` callback.
- `CategoryIconTile` uses `rememberCategoryStyle(category.name)` from designsystem to map category → color/icon. For user-created categories it falls back to category's stored `color` long value + icon string.
- `MmToggle` animation: animate thumb X from `2dp` (off) to `track.width - thumb.size - 2dp` (on) with `spring()`.
- `MmSegmented` selection pill animates via `animateFloatAsState` on the selected index fraction.

---

## Verification
1. Create a `@Preview` composable in `core/ui` showing every component in light + dark.
2. Each component renders without crash on Android emulator.
3. `MmMoney(value = -333.0, sign = "−")` shows "− EUR 333.00" in GeistMono.
4. `DonutChart` renders as a ring (not a pie) — confirm `useCenter = false`.
5. `CumulativeChart` area fill is barely visible (~6% opacity).
