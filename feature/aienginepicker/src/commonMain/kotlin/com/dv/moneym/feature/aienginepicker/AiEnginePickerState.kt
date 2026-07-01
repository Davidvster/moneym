package com.dv.moneym.feature.aienginepicker

import com.dv.moneym.core.ai.AiEngineId
import kotlinx.serialization.Serializable

@Serializable
data class AiEnginePickerState(
    val engines: List<AiEngineOption> = emptyList(),
    val selectedEngine: AiEngineId? = null,
    val selectedAvailable: Boolean = false,
    val selectedNeedsDownload: Boolean = false,
    val localModelNameKey: String? = null,
)

@Serializable
data class AiEngineOption(
    val id: AiEngineId,
    val available: Boolean,
    val needsDownload: Boolean,
)
