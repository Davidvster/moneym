package com.dv.moneym.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Category(
    val id: CategoryId,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isUserCreated: Boolean,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
