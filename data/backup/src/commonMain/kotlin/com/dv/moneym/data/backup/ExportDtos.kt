package com.dv.moneym.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupDto(
    val moneym: BackupMetaDto,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val recurringTransactions: List<RecurringTransactionDto> = emptyList(),
    val settings: BackupSettingsDto = BackupSettingsDto(),
)

@Serializable
data class BackupMetaDto(
    val version: Int = 1,
    val exportedAt: String,
    val defaultCurrency: String = "EUR",
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isUserCreated: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val type: String,
    val currency: String,
    val isDefault: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val colorHex: String? = null,
)

@Serializable
data class TransactionDto(
    val id: Long,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val note: String? = null,
    val categoryId: Long,
    val accountId: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val recurringId: Long? = null,
)

@Serializable
data class RecurringTransactionDto(
    val id: Long,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val note: String? = null,
    val categoryId: Long,
    val accountId: Long,
    val paymentModeId: Long? = null,
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
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BudgetDto(
    val id: Long,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long?,
    val accountId: Long,
    val periodType: String,
    val startYearMonth: String,
    val recurringMonths: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSettingsDto(
    val themeMode: String = "system",
    val backgroundLockSeconds: Int = 30,
    val defaultCurrency: String = "EUR",
)
