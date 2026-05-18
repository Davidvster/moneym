package com.dv.moneym.core.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PaymentModeId(val value: Long)

@Serializable
data class PaymentMode(
    val id: PaymentModeId,
    val name: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)
