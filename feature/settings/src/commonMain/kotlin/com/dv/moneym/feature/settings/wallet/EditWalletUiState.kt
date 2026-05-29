package com.dv.moneym.feature.settings.wallet

import kotlinx.serialization.Serializable

@Serializable
internal data class EditWalletUiState(
    val name: String = "",
    val colorHex: String? = null,
    val loaded: Boolean = false,
)

internal sealed interface EditWalletIntent {
    data class NameChanged(val value: String) : EditWalletIntent
    data class ColorChanged(val hex: String?) : EditWalletIntent
    data object Save : EditWalletIntent
}

internal sealed interface EditWalletEffect {
    data object Done : EditWalletEffect
}
