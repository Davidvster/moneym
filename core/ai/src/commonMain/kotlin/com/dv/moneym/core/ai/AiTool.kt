package com.dv.moneym.core.ai

data class AiTool(
    val name: String,
    val description: String,
    val paramsSchema: String,
    val invoke: suspend (Map<String, String>) -> String,
)
