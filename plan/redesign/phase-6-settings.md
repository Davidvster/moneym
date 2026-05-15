# Phase 6 — Settings + Sub-screens

Migrate `SettingsScreen` and add three new screens: TxListDisplay, CurrencyPicker, LanguagePicker. Depends on Phase 0–2.

---

## Files to modify / create

| File | Action |
|---|---|
| `feature/settings/src/commonMain/…/ui/SettingsScreen.kt` | Full rewrite |
| `feature/settings/src/commonMain/…/presentation/SettingsUiState.kt` | Add `themeMode`, `txDisplayPrefs`, `defaultCurrency`, `language` |
| `feature/settings/src/commonMain/…/presentation/SettingsViewModel.kt` | Read from `AppSettingsRepository` |
| `feature/settings/src/commonMain/…/ui/TxListDisplayScreen.kt` | **New** |
| `feature/settings/src/commonMain/…/ui/CurrencyPickerScreen.kt` | **New** |
| `feature/settings/src/commonMain/…/ui/LanguagePickerScreen.kt` | **New** |

Navigation: add routes `Settings.TxListDisplay`, `Settings.Currency`, `Settings.Language` to the settings nav graph (or `MainNav.kt`).

---

## SettingsScreen layout

```
Column(scroll) {
  Text("Settings", style=MM.type.title1, 4 20 16dp padding)

  SectionLabel("Appearance", padding=8 4dp)
  MmCard { overflow hidden
    MmRow {
      Icon(sun/moon, 18dp)
      Text("Theme", body)
      Spacer(weight=1)
      MmSegmented(["Light","Dark","Auto"], themeMode, size=sm)
    }
    MmRow(divider=false) {
      Icon(sliders, 18dp)
      Column {
        Text("Transaction list", body)
        Text(txDisplaySummary, caption, text2)
      }
      Spacer(weight=1)
      Icon(chevronRight, text3)
    } // → TxListDisplay
  }

  SectionLabel("Security", padding=16 4 8dp)
  MmCard { overflow hidden
    MmRow { Icon(lock) + "Enable PIN lock" + MmToggle(pinEnabled) }
    MmRow { Icon(fingerprint) + Column{"Unlock with biometrics" + "Face ID / Fingerprint"} + MmToggle(biometricEnabled) }
    MmRow { "Change PIN" + Icon(chevronRight) }
    MmRow(divider=false) { "Lock after" + subtitle + Icon(chevronRight) }
  }

  SectionLabel("Preferences", padding=16 4 8dp)
  MmCard { overflow hidden
    MmRow { Icon(info) + Column{"Default currency" + currencySubtitle} + Icon(chevronRight) }
    MmRow { Icon(globe) + Column{"Language" + languageSubtitle} + Icon(chevronRight) }
    MmRow(divider=false) { Icon(list) + "Manage categories" + Icon(chevronRight) }
  }

  SectionLabel("Data", padding=16 4 8dp)
  MmCard { overflow hidden
    MmRow { Icon(download) + "Export as JSON" + Icon(chevronRight) }
    MmRow { Icon(download) + "Export as CSV" + Icon(chevronRight) }
    MmRow(divider=false) { Icon(folder) + "Import data" + Icon(chevronRight) }
  }

  Text("MoneyM v2.0 · build 2026.05.15", captionMono, color=text3, center, 24dp padding)

  MmTabBar(active=TabRoute.Settings)
}
```

`txDisplaySummary` = `"${prefs.indicatorStyle.label} · ${if showNote "with note" else "no note"}"`

Theme segmented: on selection → `viewModel.setThemeMode(…)`. Root `App.kt` observes theme mode via `collectAsState` and passes `isDark` to `MoneyMTheme`.

---

## TxListDisplayScreen

New screen, accessible from Settings → "Transaction list".

```
Column {
  ScreenHeader("Transaction list")

  // Live preview panel
  Box(bg=surface2, borderBottom=1dp divider, 20 16 24dp padding) {
    SectionLabel("Preview", 0 4dp padding)
    MmCard { overflow hidden
      sampleTransactions.forEachIndexed { i, tx ->
        TxRow(tx, currentPrefs, divider = i < 2)
      }
    }
  }

  SectionLabel("Color indicator")
  MmCard(margin=0 16dp) { overflow hidden
    IndicatorStyle.values().forEachIndexed { i, opt ->
      Row(clickable, 14 16dp padding, borderBottom if not last) {
        // Mini sample (38dp wide)
        when(opt) {
          IconTile -> CategoryIconTile(sample, 32dp, IconTile)
          SoftIcon -> CategoryIconTile(sample, 32dp, SoftIcon)
          Bar      -> CategoryIconTile(sample, 32dp, Bar)
          Dot      -> CategoryIconTile(sample, 10dp, Dot)
          Minimal  -> Box(32×1dp, border color)
        }
        Column(weight=1) {
          Text(opt.label, body)
          Text(opt.description, caption, text2)
        }
        // Custom radio circle — NOT RadioButton
        Box(22dp circle, border=1.5dp accent if selected else borderStrong) {
          if selected: filled accent bg + Icon(check, 12dp, white)
        }
      }
    }
  }

  SectionLabel("Show")
  MmCard(margin=0 16dp) {
    MmRow(clickable) { "Category name" + MmToggle(showCategoryName) }
    MmRow(clickable, divider=false) { "Note / description" + MmToggle(showNote) }
  }

  SectionLabel("Density")
  MmCard(margin=0 16dp 24dp) {
    MmRow(divider=false) {
      Text("Row size", body, weight=1)
      MmSegmented(["Compact","Comfortable"], density)
    }
  }
}
```

State: `collectAsState` from `AppSettingsRepository.observeTxDisplayPrefs()`. Every change calls `setTxDisplayPrefs(updated)` immediately — no "Save" button.

---

## CurrencyPickerScreen

```
Column {
  ScreenHeader("Currency")
  MmField(placeholder="Search currency", prefix=Icon(search), margin=14 16 8dp)

  LazyColumn {
    stickyHeader { SectionLabel("Popular") }
    items(popularCurrencies) { c -> CurrencyRow(c, selected = c.code == currentCode) }
    stickyHeader { SectionLabel("All currencies") }
    items(allCurrencies) { c -> CurrencyRow(c, selected = c.code == currentCode) }
  }
}
```

`CurrencyRow`: 36dp `surface2` tile with mono symbol, code + name row, region below, green check if selected. On tap: `viewModel.setDefaultCurrency(code)`, pop back.

---

## LanguagePickerScreen

```
Column {
  ScreenHeader("Language")
  Text(description, caption, text2, 20dp padding)

  Box(0 16 8dp padding) {
    MmRow(surface bg, 1dp border, md radius) {
      Icon(globe) + Text("Use device language") + MmToggle(useDevice)
    }
  }

  SectionLabel("All languages")
  LazyColumn {
    items(languages) { l ->
      Row(14 20dp padding) {
        Box(36dp, sm radius, surface2 bg) { Text(l.code.uppercase(), micro mono, text2) }
        Column {
          Text(l.native, body)
          Text(l.name, caption, text2)
        }
        if (l.code == selected) Icon(check, accent)
      }
    }
  }
}
```

---

## Key implementation notes

- Theme change is instant — `MoneyMTheme(isDark = …)` driven by `StateFlow<ThemeMode>` at `App.kt`.
- `useDeviceLanguage` on: disable language list rows at 45% opacity; use `LocaleController.deviceLocale`.
- Currency search: client-side filter via `remember { derivedStateOf { … } }`.
- `TxRow` reused from `core/ui` (Phase 3) — the live preview uses real composable, not a screenshot.

---

## Verification
1. Theme segmented immediately switches entire app to dark/light.
2. TxListDisplay: live preview updates on every toggle/radio change.
3. TxListDisplay persists — navigate away and back, selection retained.
4. Currency picker: selecting EUR writes pref, Settings shows "EUR — Euro" subtitle.
5. App version footer shows correct string in GeistMono.
