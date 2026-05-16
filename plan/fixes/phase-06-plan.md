# Phase 6: Initial Setup Redesign (Onboarding)

## Problem
The onboarding Currency picker and Lock screen setup use Material3 `Scaffold` + `TopAppBar` which doesn't match the rest of the app's design language. The app uses a custom design system (`MM.colors`, `MM.type`, custom cards, `ScreenHeader`, `MmButton`, etc.). The currency picker in Settings (`CurrencyPickerScreen.kt`) already has the correct design — reuse it.

## Files to modify
- `feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/ui/OnboardingScreen.kt` — redesign `CurrencyStep` and `SecurityStep`

## Implementation steps

### CurrencyStep

Replace the `Scaffold` + `TopAppBar` with the app's design system:

```kotlin
@Composable
private fun CurrencyStep(
    selected: String,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    // Reuse CurrencyPickerContent layout (or replicate it inline)
    // Use: Column + statusBarsPadding + custom header text + MmField search + LazyColumn + MmButton
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredAll by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) allCurrencies
            else {
                val q = searchQuery.trim().lowercase()
                allCurrencies.filter { c ->
                    c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
                }
            }
        }
    }
    val filteredPopular by remember(filteredAll) {
        derivedStateOf { popularCurrencies.filter { p -> filteredAll.any { it.code == p.code } } }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg)
    ) {
        // Header
        Column(
            modifier = Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Text(stringResource(Res.string.onboarding_welcome), style = type.title1, color = colors.text)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(Res.string.onboarding_currency_title), style = type.body.copy(color = colors.text2))
        }

        // Search field
        MmField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search currency…",
            prefix = { Icon(MmIcons.search, contentDescription = null, tint = colors.text3, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Currency list (reuse logic from CurrencyPickerContent)
        LazyColumn(modifier = Modifier.weight(1f)) {
            // popular section + all currencies section, each row using CurrencyRow style
        }

        // Continue button at bottom
        Box(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = 16.dp, vertical = 16.dp)) {
            MmButton(
                text = stringResource(Res.string.onboarding_continue),
                onClick = onContinue,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}
```

Key: Import `allCurrencies`, `popularCurrencies`, `CurrencyRow` from `feature/settings`. Since they're in a different module, either:
- Move/copy the currency data and `CurrencyRow` composable to a shared location (e.g., `core/ui`)
- Or duplicate the necessary list + composable inline in `OnboardingScreen.kt` (simpler, avoids cross-module dep)

The simpler approach: duplicate the currency list data and `CurrencyRow` composable inside OnboardingScreen (they're purely display code).

### SecurityStep

Replace `Scaffold` + `TopAppBar` with the design system:

```kotlin
@Composable
private fun SecurityStep(
    pinEnabled: Boolean,
    onSetupPin: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg)
    ) {
        // Header area with statusBarsPadding
        Column(modifier = Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
            Text(stringResource(Res.string.onboarding_security_title), style = type.title1, color = colors.text)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(Res.string.onboarding_security_subtitle), style = type.body.copy(color = colors.text2))
        }

        // PIN toggle card
        MmCard(Modifier.padding(horizontal = 16.dp)) {
            MmRow(onClick = if (!pinEnabled) onSetupPin else null, divider = false) {
                Icon(MmIcons.lock, contentDescription = null, tint = colors.text, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.onboarding_pin_label), style = type.body, color = colors.text)
                    if (pinEnabled) {
                        Text(stringResource(Res.string.onboarding_pin_set), style = type.caption.copy(color = colors.accent))
                    }
                }
                MmToggle(checked = pinEnabled, onCheckedChange = { if (it && !pinEnabled) onSetupPin() })
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom buttons
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MmButton(
                text = stringResource(if (pinEnabled) Res.string.onboarding_done else Res.string.onboarding_skip),
                onClick = onFinish,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
            if (!pinEnabled) {
                MmButton(
                    text = stringResource(Res.string.onboarding_skip_short),
                    onClick = onSkip,
                    variant = MmButtonVariant.Ghost,
                    size = MmButtonSize.Lg,
                    fullWidth = true,
                )
            }
        }
    }
}
```

## Acceptance criteria
- [ ] Onboarding currency step uses `MM.colors`, `MM.type` — no Material `Scaffold`/`TopAppBar`
- [ ] Currency search field matches the Settings CurrencyPickerScreen style
- [ ] Currency list rows match the Settings CurrencyPickerScreen style (symbol box, code+name, check mark)
- [ ] Continue button is pinned to the bottom using the standard `MmButton` Primary style
- [ ] Security step uses the same design language (custom column, `MmCard` with `MmRow` for PIN toggle)
- [ ] Both steps respect dark/light theme
