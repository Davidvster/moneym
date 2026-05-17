package com.dv.moneym.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class ImportPreview(
    val transactions: EntityCount,
    val categories: EntityCount,
    val accounts: EntityCount,
    val isValid: Boolean = true,
    val errorMessage: String? = null,
)

@Serializable
data class EntityCount(val new: Int, val duplicate: Int) {
    val total: Int get() = new + duplicate
}

enum class ImportMode { MERGE, REPLACE }
