# MoneyM Redesign — Design Handoff

A complete redesign of the MoneyM expense/income tracker app: neutral, minimal, platform-agnostic,
with full light + dark mode support.

---

## About This Bundle

The files in this folder are **design references**, not production code. They are an HTML/React
prototype of the intended look and behaviour. Your job is to **recreate these designs in the
existing MoneyM codebase** (Compose Multiplatform / Kotlin Multiplatform targeting iOS + Android)
using the project's existing patterns — not to ship the HTML.

Read this README first. Open `MoneyM Redesign.html` in a browser to explore the interactive canvas (
foundations + components + all screens). Refer to `source/screens.jsx` for the exact composition of
each screen.

## Fidelity

**High-fidelity.** Every color, type token, spacing value, radius, and icon is final. Translate them
1:1 to Compose `Color`, `Dp`, `TextStyle`, and `Shape` values. The screens are pixel-accurate at 390
dp width — scale to other widths via responsive layout, not by re-sizing constants.

---

## Tech-Mapping Notes (Compose Multiplatform)

| Design concept                        | Compose equivalent                                                                                                                                                     |
|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CSS variable `--text` etc.            | `MaterialTheme.colorScheme.onSurface` (custom scheme) or a custom `MoneyMColors` object                                                                                |
| Light/dark mode toggle                | `isSystemInDarkTheme()` → swap a custom `CompositionLocal<MoneyMColors>`                                                                                               |
| `Geist` / `Geist Mono` font           | Bundle `geist_*.ttf` / `geist_mono_*.ttf` from Google Fonts as resources, register as `FontFamily`                                                                     |
| 8px transaction-row category dot      | `Box(Modifier.size(MM.dimen.padding_1x).clip(CircleShape).background(catColor))`                                                                                       |
| 38px filled icon tile (default)       | `Box(Modifier.size(3MM.dimen.padding_1x).clip(RoundedCornerShape(11.dp)).background(catColor))` with a centered `Icon(...)` in white                                   |
| Donut chart                           | `Canvas { drawArc(...) }` — see *Charts* below                                                                                                                         |
| Bottom Tab Bar                        | Custom `Row` — **do not** use `NavigationBar` from Material3 (too many opinionated paddings/lozenges). Build it from `Icon` + `Text` + clickable `Modifier.weight(1f)` |
| Segmented control                     | Custom `Row` inside a pill-shaped `Surface`; selected item gets its own elevated pill                                                                                  |
| FAB / "+ New"                         | **No FAB.** A full-width pinned `Button` at the bottom of the list, above the tab bar                                                                                  |
| Sheet (Category picker, New category) | `ModalBottomSheet` with `shape = RoundedCornerShape(topStart=MM.dimen.padding_2_5x, topEnd=MM.dimen.padding_2_5x)`                                                     |
| Numeric keypad on PIN screen          | Grid of `Surface(shape = RoundedCornerShape(MM.dimen.padding_2x))` buttons, 80×72dp                                                                                    |

### Why no Material3 defaults

The current MoneyM build is "too Material" — Material 3's defaults render fine on Android but look
out of place on iOS. The redesign deliberately avoids:

- `NavigationBar` / `BottomAppBar` (lozenge pill behind active item)
- `FloatingActionButton` (jarring on iOS)
- `M3 Card` with its default 12dp elevation shadow (use 0dp + 1px border instead)
- `FilledTonalButton` (too colorful)
- Ripple effects on iOS (Compose Multiplatform usually opts you out, but verify)

Use `androidx.compose.foundation` (lower-level) over `androidx.compose.material3` wherever you can,
or write your own theme primitives.

---

## Design Tokens

### Colors

Define a `MoneyMColors` data class and provide both light + dark instances via a `CompositionLocal`.
Token names mirror the CSS variables in `source/tokens.css`.

#### Neutrals — Light

| Token          | Hex       | Purpose                              |
|----------------|-----------|--------------------------------------|
| `bg`           | `#FFFFFF` | Screen background                    |
| `surface`      | `#FAFAFA` | Card / row background                |
| `surface2`     | `#F4F4F4` | Subtle filled chips, segmented track |
| `border`       | `#ECECEC` | Default 1px border                   |
| `borderStrong` | `#D4D4D4` | Toggle outline, divider strong       |
| `divider`      | `#F0F0F0` | List row divider                     |
| `text`         | `#0A0A0A` | Primary text                         |
| `text2`        | `#6B6B6B` | Secondary text                       |
| `text3`        | `#A3A3A3` | Tertiary / placeholders / icons      |

#### Neutrals — Dark

| Token          | Hex       |
|----------------|-----------|
| `bg`           | `#0A0A0A` |
| `surface`      | `#141414` |
| `surface2`     | `#1C1C1C` |
| `border`       | `#232323` |
| `borderStrong` | `#353535` |
| `divider`      | `#1E1E1E` |
| `text`         | `#FAFAFA` |
| `text2`        | `#A3A3A3` |
| `text3`        | `#6B6B6B` |

The two ramps mirror each other so all UI built with semantic tokens (`text`, `surface`, `border`)
behaves identically in both modes.

#### Accent — green (sparing)

| Token    | Hex       | Usage                                                                                                                                                                                                                                                |
|----------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `accent` | `#16A34A` | **Only** for: income amounts in lists, primary "Save / Add transaction" button, the green check confirming selection in pickers, and the green "↓ Income" arrow on overview summary cards. Never for chrome, never for chips, never for backgrounds. |

#### Category palette (the ONLY place color is used freely)

| Category      | Hex       | Icon     |
|---------------|-----------|----------|
| Health        | `#C2566B` | heart    |
| Entertainment | `#8B6FB0` | film     |
| Salary        | `#4A8E5C` | banknote |
| Transport     | `#4F8694` | car      |
| Utilities     | `#B89148` | bolt     |
| Groceries     | `#7A9572` | basket   |
| Eating out    | `#C97A4F` | utensils |
| Rent          | `#5A7BA8` | home     |
| Shopping      | `#B07089` | bag      |
| Other         | `#8A8A8A` | tag      |

These hues are mid-tone and identical in light and dark mode — don't shift them with theme.

### Typography

Two families, both from Google Fonts:

- **Geist** (sans) — UI text, headings
- **Geist Mono** — every currency value, dates, codes, sizes, percentages

```kotlin
val Geist = FontFamily(
    Font(R.font.geist_regular, weight = FontWeight.Normal),
    Font(R.font.geist_medium, weight = FontWeight.Medium),
    Font(R.font.geist_semibold, weight = FontWeight.SemiBold),
    Font(R.font.geist_bold, weight = FontWeight.Bold),
)
val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, weight = FontWeight.Normal),
    Font(R.font.geist_mono_medium, weight = FontWeight.Medium),
    Font(R.font.geist_mono_semibold, weight = FontWeight.SemiBold),
)
```

#### Scale

| Token       | Size  | Weight         | Letter spacing      | Use                                              |
|-------------|-------|----------------|---------------------|--------------------------------------------------|
| `display`   | 56 sp | 600 (SemiBold) | −1.6 sp             | Big amount on the PIN/transaction add screen     |
| `title-1`   | 28 sp | 600            | −0.6 sp             | Screen titles (Transactions, Overview, Settings) |
| `title-2`   | 22 sp | 600            | −0.4 sp             | Section heads, "MoneyM" lockscreen               |
| `title-3`   | 17 sp | 600            | −0.2 sp             | Card titles                                      |
| `body`      | 15 sp | 500 (Medium)   | −0.1 sp             | List row primary text, button labels             |
| `body-mono` | 15 sp | 500            | −0.2 sp             | Currency values, dates in fields                 |
| `caption`   | 13 sp | 500            | 0                   | Secondary row text, helper text                  |
| `micro`     | 11 sp | 600            | 0.08 sp + UPPERCASE | Section labels ("SECURITY", "PREFERENCES")       |

> **All currency values use GeistMono** + `OpenType` tabular features (`tnum`). In Compose:
`TextStyle(fontFamily = GeistMono, fontFeatureSettings = "tnum")`.

### Spacing (4 px base)

`4, 8, 12, 16, 20, 24, 32, 40, 48` — name them `s1…s12`.

### Radius

| Token  | Value                              |
|--------|------------------------------------|
| `xs`   | 6 dp                               |
| `sm`   | 8 dp                               |
| `md`   | 12 dp                              |
| `lg`   | 16 dp                              |
| `xl`   | 20 dp                              |
| `pill` | 9999 dp (`RoundedCornerShape(50)`) |

### Shadows

Both light and dark stay almost flat. Use 1px borders + tiny shadows.

- `card`: `0 1 2 rgba(0,0,0,0.04)` + `0 1 1 rgba(0,0,0,0.02)` (light), `0 1 2 rgba(0,0,0,0.3)` (
  dark)
- `pop`:  `0 4 14 rgba(0,0,0,0.06)` + `0 1 4 rgba(0,0,0,0.04)` (light), `0 4 14 rgba(0,0,0,0.35)` (
  dark)

Cards always have a `1.dp` border in `border` color **in addition to** their shadow.

---

## Screens

Coordinates and sizes below are at **390 dp** screen width (the design canvas reference). Use
weight-based / fillMaxWidth-based layout in Compose so they reflow on smaller / larger screens.

### 01 · Lock screen (PIN)

- Centered column, vertically centered.
- 56×56 dp black square (`text` color, NOT accent) with white "M" — 16dp radius, 28sp Geist Bold.
- 22sp "MoneyM" title, 14sp "Enter your PIN" caption beneath in `text2`.
- 4 dots, 12dp circles, 18dp gap. Filled = `text`; empty = transparent with 1.5dp border in
  `borderStrong`.
- 3×4 keypad. Keys are **80×72 dp rounded squares**, 16dp radius (`r-lg`).
    - Background `surface`, 1px `border` border, 28sp Geist Regular numeral.
    - Top-left of the bottom row = Face ID icon (transparent, no border).
    - Bottom-right of the bottom row = backspace (transparent, no border).
- 16dp gap between keys.

### 02 · Transactions

- Header (16dp horiz padding):
    - "Transactions" — `title-1`
    - Search icon button (40×40dp, transparent)
    - Month switcher: ← `[May 2026]` →, with right side showing "NET" label and the net amount in
      `accent` green if positive.
- Segmented control (pill): `All / Expenses / Income`. 36dp tall, `surface-2` track, white selected
  pill with subtle shadow.
- Date section labels: `micro` uppercase in `text-3`, padded 20dp.
- **Transaction row** (default style — see "Transaction list display" screen for customizable
  variants):
    - 14dp vertical padding, 20dp horizontal.
    - **Leading**: 38×38 dp colored tile (category color), 11dp radius, white icon centered, 21px
      size.
    - **Primary text**: the user's note/description, 15sp Geist Medium, `text` color.
    - **Secondary text** (12sp, `text-2`): the category name, beneath the note.
    - **Trailing**: currency value — `body-mono` Geist Mono 15sp, 500 weight. Expenses prefixed
      with "−" (`text` color). Income prefixed with "+" in `accent` green, 600 weight.
- Bottom pinned full-width primary button "New transaction" with plus icon. NOT a FAB — sits as a
  52dp tall block above the tab bar.
- Bottom tab bar (described below).

### 03 · New Transaction / 04 · Edit Transaction

Both screens use the same form; "Edit" adds a delete (trash) icon to the header.

- Header bar (52dp): close X (40dp icon button), centered title "New transaction" or "Edit
  Transaction" (17sp 600), trash icon (in `#DC2626`) only when editing.
- Body (20dp horiz padding):
    - Segmented `Expense / Income` (pill, full-width row).
    - Centered amount display: "EUR" (13sp mono `text-3`) + the value (52sp GeistMono 600, −2sp
      tracking). Value is `text-3` when 0.00, `text` when filled.
    - `Date` field (52dp tall, 12dp radius, `surface` bg, `border` outline) — label "Date" floats
      top-left as a `micro` label.
    - `Note (optional)` field — same shape.
    - "CATEGORY" `micro` label.
    - Wrap of category chips (34dp tall, pill shape). Each chip = `[icon tile 20dp] [name]`.
      Selected chip = `text` bg, white text.
- **Pinned bottom save bar** (16dp padding, `divider` top border):
    - Full-width 52dp `accent` button (`#16A34A` bg, white fg, white check icon). **Always green** —
      even before a category is picked. Label: "Add transaction" / "Save changes".

### 05 · Overview · Month   (artboard height: ~1880 dp tall)

This screen is tall — it doesn't internally scroll; the OS native scroll handles it.

- Header: "Overview" title-1, Segmented `Month / Year` on the right, month switcher beneath.
- **Two summary cards** side by side (16dp padding, 12dp radius, `surface` bg, `border` outline):
    - Income: `↓` arrow in `accent`, "INCOME" micro label, big `accent` green amount.
    - Expenses: `↑` arrow in `text`, "EXPENSES" micro label, big amount in `text`.
- **Spending by category card**:
    - Header row: title "Spending by category" + a small `%` / `EUR` segmented control top-right (
      toggles the legend values).
    - Donut chart on the left (130dp size, 18dp stroke).
    - Legend on the right:
        - First row: "TOTAL · 100%" or "TOTAL · EUR 895.90" with a divider beneath. **Total lives
          here, not in donut center.**
        - Each subsequent row: 6dp colored dot + category name + value (% or EUR).
- **Cumulative spend card**:
    - Title row.
    - Big amount (22sp 600 mono): cumulative spend through today.
    - Area+line chart (SVG): line in `text`, area fill `text @ 6% opacity`, dashed gridlines (4
      horizontal at 25/50/75 %), dashed vertical "today" line, a 4dp dot at today's value.
    - X axis: 1, 8, 15, 22, 31 — mono 10sp, `text-3`.
- **Daily trend by category card**:
    - Header "Daily trend by category".
    - For each category: row with [icon tile 32dp] [name + tx count beneath] [total on right].
      Beneath that, a tiny bar chart of daily values, 26dp tall, all bars in the category color.
      Days with no spend = `0.18` opacity (light visible track). Days with spend = `0.7` opacity;
      today = `1.0`.
- Tab bar at bottom.

### 06 · Overview · Year   (artboard height: ~1900 dp tall)

Same structure as Month but year-scoped:

- Year switcher instead of month.
- Spending by category — annual totals.
- "Monthly spending" card with 12 bars (current month = `text` color, others = `borderStrong`).
- "Monthly trend by category" card — same layout as Month's daily trend, but 12 monthly bars per
  category.

### 07 · Settings   (artboard height: ~1180 dp tall)

Grouped lists, iOS-style.

- "Settings" title-1.
- Section labels are `micro` (uppercase, `text-3`).
- Each section is a `Card` (16dp radius, `surface` bg, `border` outline) containing rows separated
  by 1px `divider`.

Sections:

1. **Appearance**
    - Theme: leading icon (sun/moon), label "Theme", trailing 3-option segmented
      `Light / Dark / Auto`.
    - Transaction list: leading sliders icon, label "Transaction list", subtitle "Icon tile · with
      note", trailing chevron right.
2. **Security**
    - Enable PIN lock — toggle (28dp tall, see Toggle component).
    - Unlock with biometrics — leading fingerprint icon, label "Unlock with biometrics", subtitle "
      Face ID / Fingerprint", trailing toggle.
    - Change PIN — chevron.
    - Lock after — chevron + subtitle "Always".
3. **Preferences**
    - Default currency (leading info icon, subtitle "EUR — Euro").
    - Language (globe icon, subtitle "English").
    - Manage categories (list icon).
4. **Data**
    - Export as JSON.
    - Export as CSV.
    - Import data.

- Footer: `MoneyM v2.0 · build 2026.05.15` (mono 11sp `text-3`).
- Tab bar at bottom.

### 08 · Category picker (sheet)

Modal bottom sheet over a dim backdrop.

- 36dp grabber.
- "Choose category" title + close button.
- Search field.
- Rows: 36dp icon tile + category name + green check on selected.
- "+ New category" secondary button at bottom.

### 09 · Currency picker   (artboard height: ~1280 dp tall)

Full screen with `ScreenHeader` (back arrow + "Currency" title).

- Search field at top.
- Sections "POPULAR" and "ALL CURRENCIES".
- Each row: 36dp `surface-2` tile with the currency symbol in
  mono, [code + name on top row, region on bottom], green check on selected.

### 10 · Language picker   (~1380 dp tall)

- Top "Use device language" toggle in its own card.
- "ALL LANGUAGES" section. Rows: 36dp `surface-2` tile with the 2-letter code (mono 11sp
  uppercase), [native name on top, English name beneath], green check on selected.

### 11 · Manage categories   (~1020 dp tall)

- ScreenHeader with "Categories" + a "New" ghost button on the right.
- Segmented `Expense / Income`.
- Count subtitle "X expense categories · drag to reorder".
- Rows: drag handle (6 dots), 36dp icon tile, name, chevron right.
- Bottom secondary "New expense category" button.

### 12 · New category (sheet)

- Sheet header with close + "New category" title.
- Centered preview chip showing live icon + name.
- "Name" field.
- "Type" segmented `Expense / Income`.
- "Color" picker: grid of 36dp swatches (15 colors). Selected swatch gets a black 2dp ring with 2dp
  white gap.
- "Icon" picker: grid of 44dp tiles (15 icons). Selected tile = filled with the selected color,
  white icon. Others = `surface` bg with `border`.
- Bottom pinned green "Create category" accent button.

### 13 · Transaction list display

A settings screen for customising the row style.

- ScreenHeader "Transaction list".
- **Live preview** at top in a `surface-2` panel: a small card with 3 sample transaction rows that
  update in real time as toggles change.
- "COLOR INDICATOR" section — 5 radio rows:
    - **Icon tile** — Filled color tile, white icon (DEFAULT)
    - **Soft icon** — Tinted tile, colored icon
    - **Color bar** — Vertical accent bar (4×38dp)
    - **Color dot** — Subtle 8px dot
    - **Minimal** — No color indicator
- Each row shows a mini sample on the left + label + description, with a custom radio circle (22dp,
  `accent` when selected).
- "SHOW" section — toggle rows:
    - Category name (default on)
    - Note / description (default on)
- "DENSITY" section: segmented `Compact / Comfortable`.

---

## Reusable Components

These are the primitives every screen is built from. Translate each to a Compose `@Composable`.

### `Button`

Variants: `primary` (text bg, bg fg), `secondary` (surface-2 bg, text fg, border), `ghost` (
transparent), `outline` (transparent, borderStrong), `accent` (#16A34A bg, white fg), `danger` (
transparent, #DC2626 fg, border).
Sizes: sm 32dp / md 44dp / lg 52dp height. 12dp radius. 8dp gap between leading-icon and label.

**Disabled state** (applies to every variant): `surface-2` background, `text-3` foreground, 1px
`border` outline, no shadow, `cursor: not-allowed`, click-handler is a no-op. The disabled token
overrides the variant so the affordance is unambiguous regardless of what the enabled state looked
like.

### `IconButton`

Circular 40dp hit target, transparent bg, no border, currentColor icon. Accent variant uses
`accent`, danger uses `#DC2626`.

### `Segmented`

Pill-shaped track 36dp tall (or 32dp small), 3px inner padding, `surface-2` bg, full pill radius.
Selected option = `bg` colored pill with a 1px shadow + 600 weight; unselected = `text-2` 500
weight.

### `Chip`

Pill 34dp, transparent bg, `border` outline, `text` fg. Selected = `text` bg + `text-inverse` fg.
Supports a leading element (color dot or icon tile).

### `Field`

52dp tall (auto-height when multiline), 12dp radius, `surface` bg, 1px `border`. Floating-style
label is rendered as a `micro` `text-2` line above the box, NOT inside. Prefix & suffix in `text-3`.

### `Toggle`

44×26dp track, full pill. Off: `surface-2` bg + `border` outline. On: `text` bg + `text` outline.
Thumb is a 20dp circle in `bg` color with a tiny shadow.

**Disabled state**: same on/off visuals but with `opacity: 0.45` and `cursor: not-allowed`. The
toggle cannot be tapped while disabled. Pair a disabled toggle with secondary-colored label text in
the surrounding row so the whole row reads as inactive.

### `Row`

Default list row: 12dp gap children, 56dp min height, 14dp×20dp padding (configurable). 1px
`divider` bottom border (last row of card omits it).

### `Card`

`surface` bg, 16dp radius, 1px `border`. Padding optional (use 20dp when padded=true).

### `TabBar`

Bottom nav: 1px `divider` top border, `bg` bg, 24dp bottom safe area, 8dp top padding. 3 tabs each
flex:1: 22dp icon + 10sp 500 label, vertical layout, 4dp gap. Active = `text` color + 1.8 stroke
icon + 600 label. Inactive = `text-3` color + 1.5 stroke icon + 500 label.

### `Money(value, sign, ...)`

Renders `{sign}{currency} {value}` with `font-feature-settings: "tnum"`. Negative uses literal
minus "−" (`U+2212`), not "-". Decimals always 2.

### `CategoryIcon(category, size, variant)`

Variants:

- `tile` (default): solid color square, 11dp radius (`size * 0.30`), 1.8px white stroke icon, icon
  at `size * 0.55`.
- `soft`: 13% alpha bg of the cat color, full-opacity colored icon.
- `bar`: 4×size dp vertical bar.
- `dot`: 8×8 dp dot.

### `Donut`

Pie slice rendered via SVG `<circle>` with `stroke-dasharray`. In Compose: `Canvas` →
`drawArc(useCenter = false, style = Stroke(stroke.dp))` per slice, rotated −90° from top.

### `CumulativeChart`

Line + area + dashed grid + dashed "today" line + dot marker.

- Path: from day 0 to today's index. Y is inverted (high value = low Y in svg coords).
- Area fill: extend path down to y=height at last point, then back to x=0,y=height.
- Today indicator: dashed vertical line + 4dp solid dot.

### `MiniBars(data, color, highlight)`

Tiny bar chart, 26dp tall. Each value → bar. Zero values render at 1.5dp with 18% opacity (so the
time axis is still visible). Non-zero bars at 70% opacity (highlight index gets 100%).

---

## Interaction & Behavior

- **PIN entry**: tap a key → fills next dot. After 4 digits, validate. Wrong PIN: shake the dot
  row (translateX ±8dp, 4 cycles, 300ms total). Face ID button: trigger biometric prompt.
- **Theme**: instantly switch all colors via the `MoneyMColors` `CompositionLocal`. Auto uses
  `isSystemInDarkTheme()`.
- **Spending by category toggle**: tapping the `%` / `EUR` segmented control switches all legend
  values without re-fetching data.
- **Transaction list customisation**: every change in `13 · Transaction list display` updates the
  preview AND persists to settings — the actual transactions list reads these prefs on render.
- **Add Transaction validation**: amount > 0 AND category selected → enable green button. Until
  then, keep the button green visually but show a dimmer state on tap if invalid, or surface inline
  errors. (User asked for "always green" — choose your validation style as long as the rest button
  stays green.)
- **Drag to reorder categories**: long-press the drag handle on `11 · Manage categories` → drag to
  new position. Use Compose Foundation's `DragAndDrop` or `reorderable` library.

---

## State Model

```kotlin
data class Transaction(
    val id: TransactionId,
    val amount: Money,           // BigDecimal-backed
    val type: TxType,            // Expense | Income
    val categoryId: CategoryId,
    val note: String,
    val date: LocalDate,
)

data class Category(
    val id: CategoryId,
    val name: String,
    val type: TxType,
    val color: Long,             // 0xFF8B6FB0 style
    val icon: CatIcon,           // sealed enum of supported icons
    val order: Int,
)

data class TxDisplayPrefs(
    val indicatorStyle: IndicatorStyle = IconTile,
    val showCategoryName: Boolean = true,
    val showNote: Boolean = true,
    val density: Density = Comfortable,
)

data class AppSettings(
    val theme: ThemeMode,              // Light | Dark | Auto
    val pinLockEnabled: Boolean,
    val biometricsEnabled: Boolean,
    val lockAfter: LockTimeout,        // Always | Seconds30 | Minute1 | Minute5
    val defaultCurrency: String,       // "EUR"
    val language: String,              // "en"
    val txDisplay: TxDisplayPrefs,
)
```

---

## Files in This Bundle

- `MoneyM Redesign.html` — open in a browser to see all screens as an interactive design canvas.
  Zoom and pan with mouse wheel + drag. Click an artboard's title bar's expand icon to view it
  fullscreen.
- `source/tokens.css` — CSS variable definitions (every color, radius, font, spacing token).
- `source/ui-primitives.jsx` — React implementation of every reusable component (Button, Chip, Icon,
  CategoryIcon, Donut, MiniBars, TabBar, …). The Icon component contains the SVG paths for all 30+
  icons used.
- `source/foundations.jsx` — cards demoing typography, neutrals, category palette, spacing/radii.
- `source/components.jsx` — cards demoing each component in both light + dark.
- `source/screens.jsx` — full screen implementations. **Read this to see exact composition of every
  screen.**
- `source/design-canvas.jsx` — the canvas viewer (not part of the design, just hosts the artboards).

## Assets

- No images required. All iconography is inline SVG (see `Icon` component in `ui-primitives.jsx` for
  the full set).
- Fonts: Geist + Geist Mono. Get the `.ttf` files from Google Fonts and bundle them as Android/iOS
  resources.

## Notes & Caveats

- **No emoji** anywhere — the design is icon-driven via the bundled SVG set.
- **No FAB.** Use a pinned bottom button. iOS users find FABs jarring.
- **Expenses are not red.** Only income is colored (`accent` green). Expenses are neutral `text`
  with a "−" prefix.
- **Total in donut center is gone.** Total now lives as the first row of the legend.
- **All currency rendering** must use GeistMono with tabular figures, so numbers visually align in
  columns.
- **The 8px category dot** is now a fallback option (under "Color dot" indicator style). The default
  is the 38dp icon tile.
- The **Lock screen "M" logo** is rendered in plain text (Geist Bold 28sp) on a black square — no
  custom logo asset needed.

---

If anything in the bundle is ambiguous, fall back to `source/screens.jsx` — that file is the source
of truth for layout and composition.
