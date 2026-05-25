package com.dv.moneym.core.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newStringUuid(): String = Uuid.random().toString()

interface SingleUiEvent {
    val id: String
}

class DefaultSingleUiEvent(override val id: String = newStringUuid()) : SingleUiEvent
