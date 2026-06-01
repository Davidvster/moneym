package com.dv.moneym.core.ai

sealed interface Grounding {
    data class Snapshot(val text: String) : Grounding
    data class Tools(val tools: List<AiTool>) : Grounding
}
