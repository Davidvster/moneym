package com.dv.moneym.feature.infopage

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.navigation.ModalKey
import kotlinx.serialization.Serializable

@Serializable
data class InfoPageKey(val pageId: String) : ModalKey

fun EntryProviderScope<NavKey>.infoPageEntry(
    onBack: () -> Unit,
) = entry<InfoPageKey> { key ->
    InfoPageScreen(pageId = key.pageId, onBack = onBack)
}
