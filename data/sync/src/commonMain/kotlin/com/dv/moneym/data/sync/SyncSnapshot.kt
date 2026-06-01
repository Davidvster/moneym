package com.dv.moneym.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncAccount(
    val syncId: String,
    val name: String,
    val type: String,
    val currency: String,
    val isDefault: Boolean,
    val archived: Boolean,
    val colorHex: String? = null,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SyncCategory(
    val syncId: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isUserCreated: Boolean,
    val archived: Boolean,
    val categoryType: String,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SyncPaymentMode(
    val syncId: String,
    val name: String,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SyncTransaction(
    val syncId: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val note: String? = null,
    val categorySyncId: String,
    val accountSyncId: String,
    val paymentModeSyncId: String? = null,
    val recurringSyncId: String? = null,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SyncRecurring(
    val syncId: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val note: String? = null,
    val categorySyncId: String,
    val accountSyncId: String,
    val paymentModeSyncId: String? = null,
    val startDate: String,
    val freqUnit: String,
    val freqInterval: Int,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val useLastDay: Boolean = false,
    val endKind: String,
    val endCount: Int? = null,
    val endDate: String? = null,
    val lastMaterializedDate: String? = null,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SyncBudget(
    val syncId: String,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val categorySyncId: String? = null,
    val accountSyncId: String,
    val periodType: String,
    val startYearMonth: String,
    val recurringMonths: Int? = null,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SyncSnapshot(
    val formatVersion: Int = 1,
    val generatedAtMs: Long,
    val originDeviceId: String,
    val accounts: List<SyncAccount> = emptyList(),
    val categories: List<SyncCategory> = emptyList(),
    val paymentModes: List<SyncPaymentMode> = emptyList(),
    val recurring: List<SyncRecurring> = emptyList(),
    val budgets: List<SyncBudget> = emptyList(),
    val transactions: List<SyncTransaction> = emptyList(),
)
