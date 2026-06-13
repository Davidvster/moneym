# Phase 1 — i18n quick wins

Two pure-i18n fixes in `feature/settings`. No behavior change. Add/modify string
keys in **all 29 locale files** under
`feature/settings/src/commonMain/composeResources/values*/strings.xml`
(base `values/` + 28 locale dirs:
`ar cs da de es et fi fr hi hr hu is it ja lt lv mk nb nl pl pt ru sk sl sv tr vi zh`).

`de`, `es`, `it` must be careful, correct translations. The rest may be
machine-assisted but must be plausible and correct, never left as English.

---

## Fix 1 — Currency-symbol subtitle uses placeholders

**Problem:** `PreferencesSection.kt:47` builds the subtitle from a hardcoded
English template:
```kotlin
val currencySymbolSubtitle = "Show $currencySymbol instead of $walletCurrency"
```
The resource `settings_use_currency_symbol_subtitle` exists but is a static
example ("Show € instead of EUR"), not actually used by the code.

**Do:**
1. Change the **base** value (`values/strings.xml`, line ~138) to positional
   placeholders:
   ```xml
   <string name="settings_use_currency_symbol_subtitle">Show %1$s instead of %2$s</string>
   ```
2. In **every locale** file, rewrite the existing
   `settings_use_currency_symbol_subtitle` value into the **same positional
   placeholder form**, preserving that language's phrasing. Examples:
   - de: `%1$s statt %2$s anzeigen`
   - es: `Mostrar %1$s en lugar de %2$s`
   - (apply equivalent for each language; keep `%1$s` = symbol, `%2$s` = code)
3. Edit `PreferencesSection.kt`:
   - Delete line 47 (`val currencySymbolSubtitle = ...`).
   - Replace the subtitle `Text(currencySymbolSubtitle, ...)` (~line 120-124)
     with:
     ```kotlin
     Text(
         stringResource(Res.string.settings_use_currency_symbol_subtitle, currencySymbol, walletCurrency),
         style = type.caption,
         color = colors.text2,
     )
     ```
   - Add the import:
     `import moneym.feature.settings.generated.resources.settings_use_currency_symbol_subtitle`
   - Keep `val currencySymbol = ...` (line 46) — still needed as the arg.

**Important (compose-resources):** the `$` in `%1$s` inside an XML string is
fine, but if the build complains, the placeholder must remain `%1$s` / `%2$s`
(positional). Do not use bare `%s`.

---

## Fix 2 — Transaction indicator-style labels are not translated

**Problem:** `TxListDisplayScreen.kt:329` renders the option label from the enum
name:
```kotlin
text = opt.name.replace(Regex("([A-Z])"), " $1").trim(),
```
producing untranslated "Icon Tile / Soft Icon / Bar / Dot / Minimal". (The
*descriptions* below them already use resources via `indicatorDescription()` at
lines 97-103 — leave those as-is.)

**Do:**
1. Add **5 new keys** to base `values/strings.xml` (next to the existing
   `settings_txdisplay_style_*` description keys, ~lines 58-62):
   ```xml
   <string name="settings_txdisplay_label_tile">Icon tile</string>
   <string name="settings_txdisplay_label_soft">Soft icon</string>
   <string name="settings_txdisplay_label_bar">Bar</string>
   <string name="settings_txdisplay_label_dot">Dot</string>
   <string name="settings_txdisplay_label_minimal">Minimal</string>
   ```
2. Add all 5 keys to **every locale** file with proper translations
   (de/es/it careful; rest machine-assisted). E.g. de: "Symbolkachel / Weiches
   Symbol / Balken / Punkt / Minimal".
3. In `TxListDisplayScreen.kt`, add a `@Composable indicatorLabel(style)`
   function mirroring `indicatorDescription()` (lines 97-103):
   ```kotlin
   @Composable
   private fun indicatorLabel(style: IndicatorStyle): String = when (style) {
       IndicatorStyle.IconTile -> stringResource(Res.string.settings_txdisplay_label_tile)
       IndicatorStyle.SoftIcon -> stringResource(Res.string.settings_txdisplay_label_soft)
       IndicatorStyle.Bar -> stringResource(Res.string.settings_txdisplay_label_bar)
       IndicatorStyle.Dot -> stringResource(Res.string.settings_txdisplay_label_dot)
       IndicatorStyle.Minimal -> stringResource(Res.string.settings_txdisplay_label_minimal)
   }
   ```
4. Replace line 329 (`text = opt.name.replace(...)`) with `text = indicatorLabel(opt)`.
5. Add the 5 imports for the new keys (compose-resources needs one import per
   key, e.g. `import moneym.feature.settings.generated.resources.settings_txdisplay_label_tile`).
   The `Regex` import may become unused — remove it if so.

---

## Verify
```bash
./gradlew :feature:settings:compileDebugKotlinAndroid
```
Then confirm no locale is missing the new keys: every `values-*/strings.xml`
must contain `settings_txdisplay_label_tile` (and the other 4) and an updated
`settings_use_currency_symbol_subtitle` with `%1$s`/`%2$s`.

Report: files changed, any locale you were unsure about, and the compile result.
Do NOT commit — the main thread commits.
