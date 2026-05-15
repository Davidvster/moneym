# Phase 0 — Design System Foundation

Replace the current M3-backed theme with a fully custom token system. No screen changes yet — just the token layer that every later phase consumes.

---

## Goal
`core/designsystem` owns all design tokens. After this phase every composable can call `MM.colors.text`, `MM.type.title1`, `MM.space.s4`, `MM.radius.md` instead of `MaterialTheme.*`.

---

## Files to modify / create

### `core/designsystem/src/commonMain/…/designsystem/`

| File | Action |
|---|---|
| `MoneyMColors.kt` | Replace with new `MoneyMColors` data class (9 semantic tokens + accent + danger + 10 category colors). Add `MoneyMLight` / `MoneyMDark` instances. Add `LocalMoneyMColors`. |
| `MoneyMTypography.kt` | Replace with `MoneyMType` data class (display, title1-3, body, bodyMono, caption, captionMono, micro). Add `LocalMoneyMType`. Keep `moneyMTypography()` shim for M3 compat during migration. |
| `MoneyMSpacing.kt` | Rename current fields to match new `MoneyMSpace` names (s1=4dp … s12=48dp). Add `LocalMoneyMSpace`. |
| `MoneyMRadius.kt` | **New** — `MoneyMRadius(xs=6, sm=8, md=12, lg=16, xl=20, pill=50%)`; `LocalMoneyMRadius`. |
| `MM.kt` | **New** — `object MM` with `@Composable` accessors `colors`, `type`, `space`, `radius`. |
| `MoneyMTheme.kt` | Add `MoneyMType` parameter. Provide all 4 `CompositionLocal`s. Keep wrapping `MaterialTheme` for M3 fallback during migration (remove in Phase 8 polish). |
| `Fonts.kt` | **New** — `expect val Geist: FontFamily` + `expect val GeistMono: FontFamily`. |
| `androidMain/Fonts.android.kt` | **New** — `actual val Geist` assembled from `Font(Res.font.geist_*)`. Same for Mono. |
| `iosMain/Fonts.ios.kt` | **New** — same pattern. |

### Font resources

Copy `.ttf` files from `docs/Geist/` and `docs/Geist_Mono/` into:
- `composeApp/src/commonMain/composeResources/font/`

Weights needed: Regular, Medium, SemiBold, Bold (Geist); Regular, Medium, SemiBold (GeistMono).

### `CategoryColor.kt` / `MoneyMCategoryIcons.kt`

Update category palette to new hex values:
| Category | New hex |
|---|---|
| Health | `#C2566B` |
| Entertainment | `#8B6FB0` |
| Salary | `#4A8E5C` |
| Transport | `#4F8694` |
| Utilities | `#B89148` |
| Groceries | `#7A9572` |
| Eating out | `#C97A4F` |
| Rent | `#5A7BA8` |
| Shopping | `#B07089` |
| Other | `#8A8A8A` |

---

## Key implementation notes

- `MoneyMType` is constructed with `Geist` / `GeistMono` font families. The `MoneyMTheme` composable calls `moneyMType(Geist, GeistMono)` internally.
- `bodyMono`, `captionMono`, `display` use `fontFeatureSettings = "tnum"` for tabular figures.
- `micro` style must be applied as `style.copy(textTransform = …)` at call sites — Compose doesn't support CSS `text-transform` natively; callers use `.uppercase()` on the string.
- Keep `moneyMTypography()` returning a `Typography` for the M3 `MaterialTheme` wrapper — so existing screens don't break before they're migrated.
- Category colors are **not** theme-sensitive — same hex in both modes.

---

## Verification
1. App builds and runs on Android (no font crash).
2. `MM.colors.text` resolves correctly — check via a throwaway `Text(MM.colors.text.toString())`.
3. Dark/light toggle in settings (or force in `MoneyMTheme(isDark = true)`) flips colors correctly.
4. Geist renders in a test screen — confirm by using `MM.type.title1` on any existing screen title.
