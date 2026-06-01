package com.dv.moneym.feature.settings.overview.locale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.settings.overview.LanguagePickerKey
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_language_all_languages
import moneym.feature.settings.generated.resources.settings_language_picker_title
import moneym.feature.settings.generated.resources.settings_language_restart_note
import moneym.feature.settings.generated.resources.settings_language_use_device
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.languagePickerEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<LanguagePickerKey>(metadata = metadata) {
    LanguagePickerScreen(onBack = onBack)
}

data class LanguageInfo(val code: String, val nativeName: String, val englishName: String)

private val supportedLanguages = listOf(
    LanguageInfo("en", "English", "English"),
    LanguageInfo("de", "Deutsch", "German"),
    LanguageInfo("es", "Español", "Spanish"),
    LanguageInfo("it", "Italiano", "Italian"),
    LanguageInfo("fr", "Français", "French"),
    LanguageInfo("pt", "Português", "Portuguese"),
    LanguageInfo("ar", "العربية", "Arabic"),
    LanguageInfo("hi", "हिन्दी", "Hindi"),
    LanguageInfo("zh", "中文", "Chinese"),
)

@Composable
private fun LanguagePickerScreen(
    onBack: () -> Unit,
    viewModel: LanguagePickerViewModel = koinViewModel(),
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    // "" means use device language
    var useDeviceLanguage by remember(selectedLanguage) {
        mutableStateOf(selectedLanguage.isBlank())
    }

    LanguagePickerContent(
        selectedLanguage = selectedLanguage,
        useDeviceLanguage = useDeviceLanguage,
        onUseDeviceChanged = { use ->
            useDeviceLanguage = use
            if (use) {
                viewModel.onIntent(LanguagePickerIntent.SetLanguage(""))
            }
        },
        onLanguageSelected = { code ->
            viewModel.onIntent(LanguagePickerIntent.SetLanguage(code))
            onBack()
        },
        onBack = onBack,
    )
}

@Composable
private fun LanguagePickerContent(
    selectedLanguage: String,
    useDeviceLanguage: Boolean,
    onUseDeviceChanged: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen
    val space = MM.dimen

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_language_picker_title), onBack = onBack)

        Text(
            text = stringResource(Res.string.settings_language_restart_note),
            style = type.caption.copy(color = colors.text2),
            modifier = Modifier.padding(space.padding_3x),
        )

        Box(Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_1x)) {
            Box(
                modifier = Modifier
                    .clip(radius.radius_1_5x)
                    .border(1.dp, colors.border, radius.radius_1_5x)
                    .background(colors.surface),
            ) {
                MmRow(divider = false) {
                    Icon(
                        imageVector = Icon.Globe.imageVector,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(MM.dimen.icon_1x),
                    )
                    Text(
                        stringResource(Res.string.settings_language_use_device),
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    MmToggle(
                        checked = useDeviceLanguage,
                        onCheckedChange = { onUseDeviceChanged(it) },
                    )
                }
            }
        }

        SectionLabel(
            text = stringResource(Res.string.settings_language_all_languages),
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = space.padding_0_5x
            ),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(supportedLanguages, key = { it.code }) { lang ->
                val isLast = lang == supportedLanguages.last()
                MmRow(
                    onClick = { if (!useDeviceLanguage) onLanguageSelected(lang.code) },
                    divider = !isLast,
                    modifier = if (useDeviceLanguage) Modifier.alpha(0.45f) else Modifier,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(radius.radius_1x)
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = lang.code.uppercase(),
                            style = type.captionMono.copy(color = colors.text2),
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text(lang.nativeName, style = type.body, color = colors.text)
                        Text(
                            lang.englishName,
                            style = type.caption.copy(color = colors.text2),
                        )
                    }

                    if (lang.code == selectedLanguage && !useDeviceLanguage) {
                        Icon(
                            imageVector = Icon.Check.imageVector,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(MM.dimen.padding_2x),
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun LanguagePickerContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        LanguagePickerContent(
            selectedLanguage = "en",
            useDeviceLanguage = false,
            onUseDeviceChanged = {},
            onLanguageSelected = {},
            onBack = {},
        )
    }
}
