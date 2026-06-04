package com.dv.moneym.data.llmmodels

object LlmModelCatalog {

    private const val FORMAT_LITERTLM = "litertlm"

    val models: List<LlmModel> = listOf(
        LlmModel(
            id = "gemma3-1b-it",
            displayNameKey = "ai_model_gemma3_1b_it",
            fileName = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.litertlm",
            url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/" +
                "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.litertlm",
            sizeBytes = 555_000_000L,
            sha256 = "",
            format = FORMAT_LITERTLM,
            requiresToken = false,
        ),
        LlmModel(
            id = "gemma4-e2b-it",
            displayNameKey = "ai_model_gemma4_e2b_it",
            fileName = "gemma-4-E2B-it.litertlm",
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/" +
                "gemma-4-E2B-it.litertlm",
            sizeBytes = 3_000_000_000L,
            sha256 = "",
            format = FORMAT_LITERTLM,
            requiresToken = false,
        ),
        LlmModel(
            id = "gemma3n-e2b-it",
            displayNameKey = "ai_model_gemma3n_e2b_it",
            fileName = "gemma-3n-E2B-it-int4.litertlm",
            url = "https://huggingface.co/litert-community/gemma-3n-E2B-it-litert-lm/resolve/main/" +
                "gemma-3n-E2B-it-int4.litertlm",
            sizeBytes = 3_000_000_000L,
            sha256 = "",
            format = FORMAT_LITERTLM,
            requiresToken = false,
        ),
    )

    fun byId(id: String): LlmModel? = models.firstOrNull { it.id == id }
}
