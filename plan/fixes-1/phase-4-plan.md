# Phase 4 — Notification (wallet) sync app picker

Repo: /Users/davidvalic/Developer/MoneyM. **Android-only feature** (iOS has no
notification-listener API; `InstalledAppsProvider` returns empty on iOS — icons
never built there). Touches `core/platform`, `feature/walletsync`.

Files:
- `core/platform/.../commonMain/.../InstalledAppsProvider.kt` (interface + `InstalledApp` data class)
- `core/platform/.../androidMain/.../InstalledAppsProvider.android.kt` (`AndroidInstalledAppsProvider`)
- `core/platform/build.gradle.kts`
- `feature/walletsync/.../home/WalletSyncHomeUiState.kt`
- `feature/walletsync/.../home/WalletSyncHomeScreen.kt` (`AppPickerSheet`, app row, selected-count text)
- `feature/walletsync/.../composeResources/values*/strings.xml` (29 files)

---

## Fix 1 — App icons (Android)

**1a.** `core/platform/build.gradle.kts`: add `implementation(libs.compose.ui)` to
the `commonMain.dependencies { }` block (it's already in `androidMain`; needed in
common so `ImageBitmap` is referenceable in the shared `InstalledApp`).

**1b.** `InstalledApp` — add an icon field:
```kotlin
import androidx.compose.ui.graphics.ImageBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null,
)
```

**1c.** `AndroidInstalledAppsProvider.installedApps()` — load each app's icon,
downscaled, on the existing `Dispatchers.IO` block. Render the resolved
drawable into a fixed-size bitmap (handles adaptive icons):
```kotlin
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
// ...
val icon = runCatching {
    resolved.loadIcon(pm)?.toImageBitmap(ICON_PX)
}.getOrNull()
InstalledApp(packageName = pkg, label = resolved.loadLabel(pm).toString(), icon = icon)
// ...
private const val ICON_PX = 96

private fun Drawable.toImageBitmap(sizePx: Int): ImageBitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return bmp.asImageBitmap()
}
```
Keep the existing `distinctBy`/`sortedBy`. Icon loading stays off the main
thread (already inside `withContext(Dispatchers.IO)`). Use `resolved.loadIcon(pm)`
(`ResolveInfo.loadIcon`) — `loadIcon` exists on `ResolveInfo`.

**1d.** Render the icon in the app row of `AppPickerSheet` (currently the `Row`
at lines ~253-272). Add a leading `Image` before the label, with a neutral
placeholder Box when `icon == null`:
```kotlin
if (app.icon != null) {
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        modifier = Modifier.size(space.padding_5x).clip(RoundedCornerShape(MM.dimen.padding_1x)),
    )
} else {
    Box(
        Modifier.size(space.padding_5x)
            .clip(RoundedCornerShape(MM.dimen.padding_1x))
            .background(colors.surface),
    )
}
```
Imports: `androidx.compose.foundation.Image`, `clip`, `RoundedCornerShape`,
`background` (if not present). Keep the `MmCheckbox`. The icon goes before the
checkbox or between checkbox and text — place it between checkbox and the label
text, with `padding(start = space.padding_1x)`.

---

## Fix 2 — Show package name under the label

In the app row, replace the single label `Text` with a `Column` showing label
(body) + packageName (caption, `colors.text2`, single line, ellipsis):
```kotlin
Column(modifier = Modifier.weight(1f).padding(start = space.padding_1x)) {
    Text(app.label, style = MM.type.body, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(app.packageName, style = MM.type.caption.copy(color = colors.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
}
```

---

## Fix 3 — Suggested-apps section

**3a.** Define a curated suggested-package set in `WalletSyncHomeUiState.kt`
(top-level, same file):
```kotlin
val WALLET_SYNC_SUGGESTED_PACKAGES: Set<String> = setOf(
    "com.google.android.apps.walletnfcrel",   // Google Wallet
    "com.google.android.apps.nbu.paisa.user", // Google Pay
    "com.paypal.android.p2pmobile",           // PayPal
    "com.revolut.revolut",                     // Revolut
    "com.transferwise.android",                // Wise
    "de.number26.android",                     // N26
    "com.squareup.cash",                       // Cash App
    "com.venmo",                               // Venmo
)
```

**3b.** Add computed split props to `WalletSyncHomeUiState` (mirroring the
existing `filteredApps` getter):
```kotlin
val suggestedApps: List<InstalledApp>
    get() = filteredApps.filter { it.packageName in WALLET_SYNC_SUGGESTED_PACKAGES }
val otherApps: List<InstalledApp>
    get() = filteredApps.filterNot { it.packageName in WALLET_SYNC_SUGGESTED_PACKAGES }
```

**3c.** In `AppPickerSheet`'s `LazyColumn`, render a "Suggested" section header +
suggested rows (only if `suggestedApps` non-empty), then an "All apps" header +
`otherApps` rows. Extract the row into a private `@Composable AppRow(app, selected,
onToggle)` to avoid duplication. Use `stickyHeader` or plain `item { }` headers
styled as caption/section labels (`colors.text2`, uppercase optional — match how
other section labels look, e.g. `MM.type.micro`/`caption`).

**3d.** New string keys (base + all 28 locales):
```xml
<string name="wallet_sync_apps_suggested">Suggested</string>
<string name="wallet_sync_apps_all">All apps</string>
```
de: "Vorgeschlagen"/"Alle Apps"; es: "Sugeridas"/"Todas las apps"; it:
"Suggerite"/"Tutte le app"; rest machine-assisted but correct.

---

## Fix 4 — "%d" literal in selected-count / pending text

The resources `wallet_sync_apps_selected` (`%d selected`) and
`wallet_sync_review_pending` (`Review %d suggestions`) render the literal `%d`.
Switch both to **positional** `%1$d` in **all 29** locale files (compose-resources
formats positional args reliably). Examples:
- base: `%1$d selected`, `Review %1$d suggestions`
- de: `%1$d ausgewählt`, `%1$d Vorschläge prüfen`
Call sites already pass the count (`WalletSyncHomeScreen.kt:176` and `:190`) — no
Kotlin change needed there.

Translate the surrounding words for every locale (do not leave English). Keep
exactly one `%1$d` per string.

---

## Fix 5 — Loading uses a spinner, not inline text

In `AppPickerSheet`, the `if (state.appsLoading)` branch (lines ~240-246)
currently shows a "Loading apps…" `Text`. Replace it with a centered spinner in a
min-height area:
```kotlin
Box(
    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
    contentAlignment = Alignment.Center,
) {
    MmLoadingSpinner()
}
```
Use `com.dv.moneym.core.ui.MmLoadingSpinner` (the spinner used inside
`MmLoadingOverlay`). If `MmLoadingSpinner` is not accessible/public from this
module, instead use `MmLoadingOverlay(visible = true)` inside a sized
`Box(Modifier.fillMaxWidth().height(120.dp))`. Imports: `heightIn`/`height`,
`dp`, `Alignment`, `Box`. The `wallet_sync_apps_loading` key may become unused —
leave it in place (do not delete).

---

## Verify
```bash
./gradlew :core:platform:compileDebugKotlinAndroid \
          :feature:walletsync:compileDebugKotlinAndroid
```
Also confirm iOS still links (the InstalledApp icon field must compile for
Native):
```bash
./gradlew :core:platform:compileKotlinIosSimulatorArm64
```
All must pass. Confirm every locale file has `wallet_sync_apps_suggested`,
`wallet_sync_apps_all`, and `%1$d` (not `%d`) in the two count strings.

Report files changed, compile results, and the suggested-package list you used.
Do NOT commit — the main thread commits.
