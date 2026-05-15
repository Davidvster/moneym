package com.dv.moneym.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupDto(
    val moneym: BackupMetaDto,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
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
)

@Serializable
data class BackupSettingsDto(
    val themeMode: String = "system",
    val backgroundLockSeconds: Int = 30,
    val defaultCurrency: String = "EUR",
)
