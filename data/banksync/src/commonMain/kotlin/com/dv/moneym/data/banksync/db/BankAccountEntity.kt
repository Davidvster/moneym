package com.dv.moneym.data.banksync.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "BankAccount")
data class BankAccountEntity(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "bank_name")        val bankName: String,
    @ColumnInfo(name = "display_name")     val displayName: String? = null,
    val iban: String? = null,
    val currency: String,
    @ColumnInfo(name = "local_account_id") val localAccountId: Long? = null,
    @ColumnInfo(name = "enabled", defaultValue = "1") val enabled: Boolean = true,
    @ColumnInfo(name = "last_synced_date") val lastSyncedDate: String? = null,
    @ColumnInfo(name = "last_synced_at")   val lastSyncedAt: Long? = null,
)
