package com.dv.moneym.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.about.generated.resources.Res
import moneym.feature.about.generated.resources.about_privacy
import moneym.feature.about.generated.resources.about_terms
import moneym.feature.about.generated.resources.about_title
import org.jetbrains.compose.resources.stringResource

private const val ABOUT_URL = "https://davidvster.github.io/moneym.github.io/"

@Serializable
data object AboutKey : NavKey

fun EntryProviderScope<NavKey>.aboutEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AboutKey>(metadata = metadata) {
    AboutScreen(onBack = onBack)
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
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
                        onClick = { uriHandler.openUri(ABOUT_URL) },
                    )
                    MmSettingsRow(
                        title = stringResource(Res.string.about_privacy),
                        leadingIcon = Icon.Info.imageVector,
                        onClick = { uriHandler.openUri(ABOUT_URL) },
                        divider = false,
                    )
                }
            }
        }
    }
}
