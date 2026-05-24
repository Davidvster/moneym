package com.dv.moneym.feature.settings.paymentmodes

import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import kotlinx.serialization.Serializable

@Serializable
internal data class PaymentModeListUiState(
    val modes: List<PaymentMode> = emptyList(),
    val isLoading: Boolean = true,
    val dialogState: PaymentModeDialogState = PaymentModeDialogState.None,
)

@Serializable
internal sealed interface PaymentModeDialogState {
    @Serializable
    data object None : PaymentModeDialogState

    @Serializable
    data object Add : PaymentModeDialogState

    @Serializable
    data class Rename(val id: PaymentModeId, val currentName: String) : PaymentModeDialogState

    @Serializable
    data class DeleteConfirm(val id: PaymentModeId, val name: String) : PaymentModeDialogState
}

internal sealed interface PaymentModeListIntent {
    data object ShowAdd : PaymentModeListIntent
    data class ShowRename(val id: PaymentModeId, val currentName: String) : PaymentModeListIntent
    data class ShowDelete(val id: PaymentModeId, val name: String) : PaymentModeListIntent
    data object Dismiss : PaymentModeListIntent
    data class Create(val name: String) : PaymentModeListIntent
    data class Rename(val id: PaymentModeId, val name: String) : PaymentModeListIntent
    data class Delete(val id: PaymentModeId) : PaymentModeListIntent
}
