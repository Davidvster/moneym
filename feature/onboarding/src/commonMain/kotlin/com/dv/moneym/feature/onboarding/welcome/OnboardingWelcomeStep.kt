package com.dv.moneym.feature.onboarding.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon as MmIcon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.onboarding_get_started
import moneym.feature.onboarding.generated.resources.onboarding_privacy
import moneym.feature.onboarding.generated.resources.onboarding_terms
import moneym.feature.onboarding.generated.resources.onboarding_welcome
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_ai_desc
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_ai_title
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_insights_desc
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_insights_title
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_privacy_desc
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_privacy_title
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_track_desc
import moneym.feature.onboarding.generated.resources.onboarding_welcome_feature_track_title
import moneym.feature.onboarding.generated.resources.onboarding_welcome_subtitle
import org.jetbrains.compose.resources.stringResource

private const val LEGAL_URL = "https://davidvster.github.io/moneym.github.io/"

@Serializable
data object OnboardingWelcomeKey : NavKey

fun EntryProviderScope<NavKey>.onboardingWelcomeEntry(
    onGetStarted: () -> Unit,
) = entry<OnboardingWelcomeKey> {
    WelcomeStep(onGetStarted = onGetStarted)
}

private data class Capability(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val description: String,
)

@Composable
internal fun WelcomeStep(
    onGetStarted: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val uriHandler = LocalUriHandler.current

    val capabilities = listOf(
        Capability(
            icon = MmIcon.Wallet.imageVector,
            tint = colors.catSalary,
            title = stringResource(Res.string.onboarding_welcome_feature_track_title),
            description = stringResource(Res.string.onboarding_welcome_feature_track_desc),
        ),
        Capability(
            icon = MmIcon.Chart.imageVector,
            tint = colors.catTransport,
            title = stringResource(Res.string.onboarding_welcome_feature_insights_title),
            description = stringResource(Res.string.onboarding_welcome_feature_insights_desc),
        ),
        Capability(
            icon = MmIcon.Sparkles.imageVector,
            tint = colors.catEntertainment,
            title = stringResource(Res.string.onboarding_welcome_feature_ai_title),
            description = stringResource(Res.string.onboarding_welcome_feature_ai_desc),
        ),
        Capability(
            icon = MmIcon.Lock.imageVector,
            tint = colors.accent,
            title = stringResource(Res.string.onboarding_welcome_feature_privacy_title),
            description = stringResource(Res.string.onboarding_welcome_feature_privacy_desc),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MM.dimen.padding_2_5x)
                .statusBarsPadding(),
        ) {
            Spacer(Modifier.height(MM.dimen.padding_4x))
            Text(
                text = stringResource(Res.string.onboarding_welcome),
                style = type.title1.copy(fontWeight = FontWeight.Bold),
                color = colors.text,
            )
            Spacer(Modifier.height(MM.dimen.padding_1x))
            Text(
                text = stringResource(Res.string.onboarding_welcome_subtitle),
                style = type.body,
                color = colors.text2,
            )
            Spacer(Modifier.height(MM.dimen.padding_4x))

            capabilities.forEach { capability ->
                CapabilityRow(capability)
                Spacer(Modifier.height(MM.dimen.padding_2_5x))
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = MM.dimen.padding_2x, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x, Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_terms),
                    style = type.caption,
                    color = colors.accent,
                    modifier = Modifier.clickable { uriHandler.openUri(LEGAL_URL) },
                )
                Text(
                    text = stringResource(Res.string.onboarding_privacy),
                    style = type.caption,
                    color = colors.accent,
                    modifier = Modifier.clickable { uriHandler.openUri(LEGAL_URL) },
                )
            }
            Spacer(Modifier.height(MM.dimen.padding_1_5x))
            MmButton(
                text = stringResource(Res.string.onboarding_get_started),
                onClick = onGetStarted,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun CapabilityRow(capability: Capability) {
    val colors = MM.colors
    val type = MM.type

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(capability.tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = capability.icon,
                contentDescription = null,
                tint = capability.tint,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = capability.title,
                style = type.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Text(
                text = capability.description,
                style = type.caption,
                color = colors.text2,
            )
        }
    }
}

@Preview
@Composable
private fun WelcomeStepPreview() {
    MoneyMTheme {
        WelcomeStep(onGetStarted = {})
    }
}
