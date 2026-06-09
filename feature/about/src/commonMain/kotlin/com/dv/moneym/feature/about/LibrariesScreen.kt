package com.dv.moneym.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.about.generated.resources.Res
import moneym.feature.about.generated.resources.libraries_title
import org.jetbrains.compose.resources.stringResource

@Serializable
data object LibrariesKey : NavKey

private data class LibraryInfo(
    val name: String,
    val coordinate: String,
    val version: String,
)

private val LIBRARIES: List<LibraryInfo> = listOf(
    LibraryInfo("Kotlin", "org.jetbrains.kotlin", "2.3.21"),
    LibraryInfo("Compose Multiplatform", "org.jetbrains.compose", "1.10.3"),
    LibraryInfo("Compose Material 3", "org.jetbrains.compose.material3", "1.10.0-alpha05"),
    LibraryInfo("Compose Material Icons", "org.jetbrains.compose.material", "1.7.3"),
    LibraryInfo("Kotlinx Coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core", "1.10.1"),
    LibraryInfo("Kotlinx Datetime", "org.jetbrains.kotlinx:kotlinx-datetime", "0.7.0"),
    LibraryInfo("Kotlinx Serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json", "1.8.0"),
    LibraryInfo("Koin", "io.insert-koin", "4.1.0"),
    LibraryInfo("Room", "androidx.room", "2.7.2"),
    LibraryInfo("AndroidX SQLite", "androidx.sqlite:sqlite-bundled", "2.5.0"),
    LibraryInfo("Multiplatform Settings", "com.russhwolf:multiplatform-settings", "1.2.0"),
    LibraryInfo("Navigation 3", "androidx.navigation3", "1.1.1"),
    LibraryInfo("Navigation 3 UI (Multiplatform)", "org.jetbrains.androidx.navigation3", "1.0.0-alpha05"),
    LibraryInfo("AndroidX Lifecycle (Multiplatform)", "org.jetbrains.androidx.lifecycle", "2.10.0"),
    LibraryInfo("Compose Charts", "io.github.ehsannarmani:compose-charts", "0.1.4"),
    LibraryInfo("Reorderable", "sh.calvin.reorderable:reorderable", "2.4.3"),
    LibraryInfo("Cryptography Kotlin", "dev.whyoleg.cryptography", "0.5.0"),
    LibraryInfo("Ktor", "io.ktor", "3.0.3"),
    LibraryInfo("Kermit", "co.touchlab:kermit", "2.0.5"),
    LibraryInfo("ML Kit GenAI Prompt", "com.google.mlkit:genai-prompt", "1.0.0-beta2"),
    LibraryInfo("LiteRT-LM", "com.google.ai.edge.litertlm:litertlm-android", "0.13.0"),
    LibraryInfo("AndroidX Core KTX", "androidx.core:core-ktx", "1.18.0"),
    LibraryInfo("AndroidX Activity Compose", "androidx.activity:activity-compose", "1.13.0"),
    LibraryInfo("AndroidX AppCompat", "androidx.appcompat:appcompat", "1.7.1"),
    LibraryInfo("AndroidX Biometric", "androidx.biometric:biometric", "1.2.0-alpha05"),
    LibraryInfo("AndroidX Security Crypto", "androidx.security:security-crypto", "1.1.0-alpha06"),
    LibraryInfo("AndroidX Credentials", "androidx.credentials:credentials", "1.3.0"),
    LibraryInfo("AndroidX DocumentFile", "androidx.documentfile:documentfile", "1.0.1"),
    LibraryInfo("Google Identity", "com.google.android.libraries.identity.googleid:googleid", "1.1.1"),
    LibraryInfo("Play Services Auth", "com.google.android.gms:play-services-auth", "21.3.0"),
)

fun EntryProviderScope<NavKey>.librariesEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<LibrariesKey>(metadata = metadata) {
    LibrariesScreen(onBack = onBack)
}

@Composable
private fun LibrariesScreen(onBack: () -> Unit) {
    val space = MM.dimen

    Column(modifier = Modifier.fillMaxSize().background(MM.colors.bg)) {
        ScreenHeader(
            title = stringResource(Res.string.libraries_title),
            onBack = onBack,
        )
        LazyColumn {
            item(key = "libraries_card") {
                MmCard(modifier = Modifier.padding(horizontal = space.padding_2x)) {
                    LIBRARIES.forEachIndexed { index, library ->
                        MmSettingsRow(
                            title = library.name,
                            subtitle = "${library.coordinate} · ${library.version}",
                            leadingIcon = Icon.Info.imageVector,
                            trailing = null,
                            divider = index != LIBRARIES.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LibrariesScreenPreview() {
    MoneyMTheme {
        LibrariesScreen(onBack = {})
    }
}
