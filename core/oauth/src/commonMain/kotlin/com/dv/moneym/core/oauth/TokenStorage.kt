package com.dv.moneym.core.oauth

import kotlinx.serialization.Serializable

@Serializable
data class StoredTokens(
    val refreshToken: String,
    val accessToken: String?,
    val expiresAtMs: Long,
    val email: String?,
)

interface SecureTokenStore {
    suspend fun read(): StoredTokens?
    suspend fun write(tokens: StoredTokens)
    suspend fun clear()
}
