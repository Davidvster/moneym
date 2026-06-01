package com.dv.moneym.data.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncEntityType { ACCOUNT, CATEGORY, PAYMENT_MODE, TRANSACTION, RECURRING, BUDGET }
