# Phase 6: Info Page Feature Module

## Context
Add an info button (top-right) to the Backup & Restore screen that opens a static info page. The info page lives in its own feature module so any screen can show an info page. Content supports basic HTML (rendered as styled AnnotatedString — no WebView needed). Initial content: backup/restore explanation.

## New Module: feature/infopage

### Module structure
```
feature/infopage/
├── build.gradle.kts
└── src/commonMain/kotlin/com/dv/moneym/feature/infopage/
    ├── InfoPageKey.kt          # NavKey + entryProvider
    ├── InfoPageScreen.kt       # Composable
    └── HtmlText.kt             # HTML → AnnotatedString renderer
```
Plus:
```
feature/infopage/src/commonMain/composeResources/values/strings.xml
feature/infopage/src/commonMain/composeResources/values-de/strings.xml
feature/infopage/src/commonMain/composeResources/values-es/strings.xml
feature/infopage/src/commonMain/composeResources/values-it/strings.xml
```

### build.gradle.kts
Copy structure from `feature/settings/build.gradle.kts`:
- Same plugins (KMP, Android Library, Compose, Serialization)
- Same Compose deps
- `namespace = "com.dv.moneym.feature.infopage"`
- `baseName = "FeatureInfoPage"`
- Dependencies: `core.designsystem`, `core.ui`, `core.model`, `core.navigation`, `core.common`
- No data layer deps needed (static content)

### settings.gradle.kts
Add: `include(":feature:infopage")`

### InfoPageKey.kt
```kotlin
@Serializable
data class InfoPageKey(val pageId: String) : ModalKey

fun EntryProviderScope<NavKey>.infoPageEntry(onBack: () -> Unit) =
    entry<InfoPageKey> { key ->
        InfoPageScreen(pageId = key.pageId, onBack = onBack)
    }
```

### InfoPageScreen.kt
```kotlin
@Composable
fun InfoPageScreen(pageId: String, onBack: () -> Unit) {
    val title: String
    val htmlContent: String
    when (pageId) {
        "backup" -> {
            title = stringResource(Res.string.info_backup_title)
            htmlContent = stringResource(Res.string.info_backup_content)
        }
        else -> {
            title = pageId
            htmlContent = ""
        }
    }
    Column(Modifier.fillMaxSize().background(MM.colors.bg)) {
        ScreenHeader(title = title, onBack = onBack)
        LazyColumn(Modifier.padding(horizontal = MM.dimen.padding_2x)) {
            item { HtmlText(html = htmlContent) }
        }
    }
}
```

### HtmlText.kt
A pure Compose composable that converts basic HTML to styled `AnnotatedString`:
```kotlin
@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val annotatedString = remember(html) { parseHtml(html) }
    Text(text = annotatedString, style = MM.type.body, color = MM.colors.text, modifier = modifier)
}
```
`parseHtml()` handles: `<b>...</b>`, `<br>` / `<br/>`, `<p>...</p>`, `<h2>...</h2>`, `<ul>/<li>` (bullet points).

Implementation: Simple state-machine parser that builds `AnnotatedString.Builder`. No external library needed. Support tags:
- `<h2>` → body_bold style + extra top padding (via `\n\n`)
- `<b>` → bold span
- `<br>` → newline
- `<p>` → paragraph break (`\n\n`)
- `<li>` → `\n• ` prefix
- Strip unknown tags

### strings.xml — content for backup info page
```xml
<string name="info_backup_title">Backup &amp; Restore</string>
<string name="info_backup_content"><![CDATA[
<h2>Local Backup</h2>
<p><b>Backup to file</b> exports your complete database as a ZIP file you can save anywhere.</p>
<p><b>Auto backup</b> automatically saves a backup to a folder you choose whenever the app data changes.</p>
<p><b>Restore from file</b> imports a previously exported ZIP file. This replaces all current data.</p>

<h2>Cloud Backup (Google Drive)</h2>
<p>Cloud backup stores an encrypted copy of your data in your Google Drive. Only you can decrypt it with your passphrase.</p>
<p><b>Auto backup</b> uploads a new backup automatically after each change (with a short delay).</p>
<p><b>Back up now</b> triggers an immediate upload regardless of the auto backup setting.</p>
<p><b>Passphrase</b> is used to encrypt your backup. It is never stored on the device. If you lose it, you cannot restore the cloud backup.</p>
<p>Up to 5 backup versions are kept. Older ones are deleted automatically.</p>

<h2>Restore</h2>
<p>Restoring a backup <b>replaces all current data</b>. The app will restart after a successful restore.</p>
<p>For cloud restore, you need the passphrase that was used when the backup was created.</p>
]]></string>
```
Add de/es/it translations.

## Wiring into existing app

### feature/settings/build.gradle.kts
Add: `implementation(projects.feature.infopage)`

### BackupRestoreScreen.kt
- Add `onNavigateToInfo: () -> Unit` param to `BackupRestoreScreen`
- Add info button to `ScreenHeader` trailing slot:
  ```kotlin
  ScreenHeader(
      title = stringResource(Res.string.settings_backup_restore),
      onBack = onBack,
      trailingContent = {
          MmIconButton(
              icon = Icon.Info.imageVector,
              onClick = onNavigateToInfo,
              contentDescription = "Info",
          )
      }
  )
  ```

### MainNav.kt (or wherever BackupRestoreScreen navigation is defined)
- Add `infoPageEntry(onBack = { backStack.removeLastOrNull() })` to the navigation graph
- Pass `onNavigateToInfo = { backStack.add(InfoPageKey("backup")) }` to `BackupRestoreScreen`

### FeatureModules.kt
No ViewModel → no Koin registration needed for this module.

### composeApp/build.gradle.kts
Add: `implementation(projects.feature.infopage)`

## Icon
Use `Icon.Info` if it exists in `core/model/Icon.kt`. If not, add it (mapped to `androidx.compose.material.icons.Icons.Outlined.Info` or similar).

## Verification
1. Open Backup & Restore screen → see info icon in top-right
2. Tap info → opens info page with formatted backup explanation
3. Content shows headers, bold text, paragraphs correctly
4. Back button returns to backup screen
