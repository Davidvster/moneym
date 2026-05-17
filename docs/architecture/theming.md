# Design system and theming

MoneyM is intentionally **minimal**: a near-monochrome canvas where the only color comes from
category dots. The visual identity is restraint — generous whitespace, strong typographic hierarchy,
no decorative gradients or shadows.

## Palette

### Light

| Token              | Value     | Use                                |
|--------------------|-----------|------------------------------------|
| `background`       | `#FAFAFA` | App background                     |
| `surface`          | `#FFFFFF` | Cards, sheets                      |
| `surfaceVariant`   | `#F2F2F2` | Inputs, dividers, chip backgrounds |
| `onBackground`     | `#0A0A0A` | Primary text                       |
| `onSurface`        | `#0A0A0A` | Primary text on surfaces           |
| `onSurfaceVariant` | `#5C5C5C` | Secondary text, hints              |
| `outline`          | `#D6D6D6` | Borders, dividers                  |
| `outlineVariant`   | `#EAEAEA` | Subtle separators                  |
| `primary`          | `#0A0A0A` | Buttons, active states             |
| `onPrimary`        | `#FFFFFF` | Text on primary                    |
| `error`            | `#7A1F1F` | Error states (kept muted)          |

### Dark

| Token              | Value     | Use                                |
|--------------------|-----------|------------------------------------|
| `background`       | `#0B0B0B` | App background                     |
| `surface`          | `#141414` | Cards, sheets                      |
| `surfaceVariant`   | `#1E1E1E` | Inputs, dividers, chip backgrounds |
| `onBackground`     | `#F2F2F2` | Primary text                       |
| `onSurface`        | `#F2F2F2` | Primary text on surfaces           |
| `onSurfaceVariant` | `#9C9C9C` | Secondary text, hints              |
| `outline`          | `#2E2E2E` | Borders, dividers                  |
| `outlineVariant`   | `#1E1E1E` | Subtle separators                  |
| `primary`          | `#F2F2F2` | Buttons, active states             |
| `onPrimary`        | `#0A0A0A` | Text on primary                    |
| `error`            | `#C46A6A` | Error states (kept muted)          |

Values are recommendations; tune them once we see them on real screens.

### Category colors

Categories are the only place where the palette opens up. We expose a curated set of 12 muted hues
for users to pick from; users may also pick a custom hex. The 12 defaults (used by seed categories
and offered first in the picker):

```
#7E9C8C  #C97B57  #5F6F8A  #3B7080  #B89A4B  #9B5C7D
#7C5C9B  #6D6D6D  #4A7A56  #B0623B  #4D6E92  #8A8A8A
```

All chosen for ~AA contrast against both light and dark surfaces. When rendering category color on a
surface, render it as a small dot/swatch or a thin bar — never as a large fill behind text. We add a
tonal-overlay fallback in `core:designsystem`'s `categoryColor()` helper if a user-picked color
fails the contrast check.

## Typography

Use Material 3 typography scale with one font family. Default: the system font (San Francisco on
iOS, Roboto on Android) — no custom font in v1. If we add one later (we won't unless someone asks),
it's loaded as a Compose resource font.

Hierarchy we'll actually use:

- `displayLarge` — onboarding hero
- `headlineLarge` — screen titles
- `titleMedium` — section headers, day group labels
- `bodyLarge` — primary text, transaction notes
- `bodyMedium` — secondary text
- `labelSmall` — chips, meta

Numbers (amounts) deserve a tabular-figure variant for column alignment. We pass
`fontFeatureSettings = "tnum"` on amount text.

## Spacing

Quantized scale, accessed via `MoneyMTheme.spacing`:

```
xxs = 2.dp
xs  = 4.dp
sm  = MM.dimen.padding_1x
md  = MM.dimen.padding_1_5x
lg  = MM.dimen.padding_2x
xl  = 24.dp
xxl = MM.dimen.padding_4x
```

No raw `.dp` outside `core:designsystem`.

## Components in core:ui

Built once, used everywhere. Each is a stateless Composable that accepts state + lambdas:

- `MoneyMScaffold` — app bar + body + bottom nav (when relevant)
- `MoneyMTopAppBar`
- `MoneyMPrimaryButton`, `MoneyMTextButton`
- `MoneyMTextField`, `MoneyMAmountField` (currency-aware)
- `CategoryDot`, `CategoryChip`
- `TransactionRow` (icon, title, amount, day-of-week)
- `DayHeader` (sticky list header)
- `EmptyState` (icon + title + body + optional action)
- `MoneyMLoader`
- `MoneyMBottomSheet` wrapper

Anything more complex is feature-local under `feature/<name>/ui/components/`.

## Icons

Icons go through `MoneyMIcons` (a `core:designsystem` object). v1 forwards to Material Symbols /
Material Icons. Wrapping keeps the door open for a custom icon set later without touching every
callsite.

Category icons are referenced by `icon_key` strings on the `CategoryRow` table — the icon registry
maps keys to drawables. Adding an icon = registry entry + asset.

## Dark mode handling

- `MoneyMTheme(darkTheme = isSystemInDarkTheme())` is the default.
- `pref.theme_mode` overrides — `"system" | "light" | "dark"`.
- We don't animate the theme transition. Instant switch.

## Motion

Minimalist means restrained motion:

- Default Material 3 transitions only.
- No parallax. No spring overshoots. No hero animations.
- Bottom sheets and dialogs use the default Material easing.

## Accessibility

- All interactive elements have `contentDescription` or `Modifier.semantics`.
- Minimum tap target: MM.dimen.padding_6x.
- Color contrast on text ≥ 4.5:1 on both themes — verified for the token palette above.
- Category color is **never** the only signal: pair with text and icon.
- Dynamic font sizing is respected — no fixed font sizes in dp.
