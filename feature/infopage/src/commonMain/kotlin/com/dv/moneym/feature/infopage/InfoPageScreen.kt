package com.dv.moneym.feature.infopage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.ScreenHeader
import moneym.feature.infopage.generated.resources.Res
import moneym.feature.infopage.generated.resources.info_backup_content
import moneym.feature.infopage.generated.resources.info_backup_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InfoPageScreen(pageId: String, onBack: () -> Unit) {
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
        LazyColumn(modifier = Modifier.padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1x)) {
            item {
                HtmlText(html = htmlContent)
            }
        }
    }
}

@Preview
@Composable
private fun InfoPageScreenPreview() {
    MoneyMTheme {
        InfoPageScreen(pageId = "backup", onBack = {})
    }
}
