package com.dv.moneym.data.llmmodels

object LlmModelCatalog {

    private const val FORMAT_LITERTLM = "litertlm"

    val models: List<LlmModel> = listOf(
        LlmModel(
            id = "gemma3-1b-it",
            displayNameKey = "ai_model_gemma3_1b_it",
            fileName = "gemma3-1b-it-int4.litertlm",
            url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/" +
                "gemma3-1b-it-int4.litertlm",
            sizeBytes = 584417280L,
            sha256 = "****************************************************************",
            format = FORMAT_LITERTLM,
            requiresToken = false,
        ),
        LlmModel(
            id = "gemma4-e2b-it",
            displayNameKey = "ai_model_gemma4_e2b_it",
            fileName = "gemma-4-E2B-it.litertlm",
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/" +
                "gemma-4-E2B-it.litertlm",
            sizeBytes = 2588147712L,
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            format = FORMAT_LITERTLM,
            requiresToken = false,
        ),
    )

    fun byId(id: String): LlmModel? = models.firstOrNull { it.id == id }
}
