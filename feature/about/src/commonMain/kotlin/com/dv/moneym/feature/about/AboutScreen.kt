package com.dv.moneym.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.about.generated.resources.Res
import moneym.feature.about.generated.resources.about_libraries
import moneym.feature.about.generated.resources.about_privacy
import moneym.feature.about.generated.resources.about_terms
import moneym.feature.about.generated.resources.about_title
import org.jetbrains.compose.resources.stringResource

private const val TERMS_URL = "https://davidvster.github.io/moneym.github.io/terms.html"
private const val PRIVACY_URL = "https://davidvster.github.io/moneym.github.io/privacy.html"

@Serializable
data object AboutKey : NavKey

fun EntryProviderScope<NavKey>.aboutEntry(
    onBack: () -> Unit,
    onNavigateToLibraries: () -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
) = entry<AboutKey>(metadata = metadata) {
    AboutScreen(onBack = onBack, onNavigateToLibraries = onNavigateToLibraries)
}

@Composable
private fun AboutScreen(onBack: () -> Unit, onNavigateToLibraries: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val space = MM.dimen

    Column(modifier = Modifier.fillMaxSize().background(MM.colors.bg)) {
        ScreenHeader(
            title = stringResource(Res.string.about_title),
            onBack = onBack,
        )
        LazyColumn {
            item(key = "about_card") {
                MmCard(modifier = Modifier.padding(horizontal = space.padding_2x)) {
                    MmSettingsRow(
                        title = stringResource(Res.string.about_terms),
                        leadingIcon = Icon.Info.imageVector,
                        onClick = { uriHandler.openUri(TERMS_URL) },
                    )
                    MmSettingsRow(
                        title = stringResource(Res.string.about_privacy),
                        leadingIcon = Icon.Info.imageVector,
                        onClick = { uriHandler.openUri(PRIVACY_URL) },
                    )
                    MmSettingsRow(
                        title = stringResource(Res.string.about_libraries),
                        leadingIcon = Icon.List.imageVector,
                        onClick = onNavigateToLibraries,
                        divider = false,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    MoneyMTheme {
        AboutScreen(onBack = {})
    }
}
