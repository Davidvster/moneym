package com.dv.moneym.data.llmmodels

data class LlmModelState(
    val model: LlmModel,
    val downloaded: Boolean,
    val active: Boolean,
    val progress: Float?,
)
