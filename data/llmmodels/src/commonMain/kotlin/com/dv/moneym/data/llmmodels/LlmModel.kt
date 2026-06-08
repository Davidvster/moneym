package com.dv.moneym.data.llmmodels

data class LlmModel(
    val id: String,
    val displayNameKey: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
    val format: String,
)
