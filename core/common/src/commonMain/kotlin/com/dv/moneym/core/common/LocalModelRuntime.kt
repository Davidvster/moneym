package com.dv.moneym.core.common

/**
 * Seam for releasing on-device LLM resources. Lives in core/common so the data layer (which owns
 * model files) and core/ai (which owns the native engine + its compiled-model cache) can share it
 * without depending on each other.
 *
 * The native runtime keeps the model loaded (an open file handle pins the on-disk blocks even after
 * the file is unlinked) and writes a compiled copy of the model into a cache directory. Deleting the
 * downloaded blob alone therefore frees nothing until the runtime releases both.
 */
interface LocalModelRuntime {
    suspend fun releaseAndClearCache()
}

/** No-op runtime for tests and platforms without a local LLM engine. */
object NoopLocalModelRuntime : LocalModelRuntime {
    override suspend fun releaseAndClearCache() = Unit
}
