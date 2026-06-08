package com.dv.moneym.data.llmmodels

object LlmModelCatalog {

    private const val FORMAT_LITERTLM = "litertlm"

    val models: List<LlmModel> = listOf(
        LlmModel(
            id = "smollm2-135m-it",
            displayNameKey = "ai_model_smollm2_135m",
            fileName = "SmolLM2_135M_Instruct.litertlm",
            url = "https://huggingface.co/litert-community/SmolLM2-135M-Instruct/resolve/main/" +
                "SmolLM2_135M_Instruct.litertlm",
            sizeBytes = 142819328L,
            sha256 = "ccdc5c85735743f081b7d44ca309cab569f76c0f2f0e8e163449a63721969c37",
            format = FORMAT_LITERTLM,
        ),
        LlmModel(
            id = "qwen2.5-1.5b-it",
            displayNameKey = "ai_model_qwen25_1_5b",
            fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/" +
                "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeBytes = 1597931520L,
            sha256 = "faa60663b333290c1496c499828b21d3e3254a788cacd8cce917ce0f761a2dc9",
            format = FORMAT_LITERTLM,
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
        ),
    )

    fun byId(id: String): LlmModel? = models.firstOrNull { it.id == id }
}
