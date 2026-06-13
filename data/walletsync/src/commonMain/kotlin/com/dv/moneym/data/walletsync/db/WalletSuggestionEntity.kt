package com.dv.moneym.data.walletsync.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "WalletSuggestion",
    indices = [
        Index(value = ["external_id"], name = "idx_ws_external", unique = true),
        Index(value = ["status"],      name = "idx_ws_status"),
    ],
)
data class WalletSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "external_id")    val externalId: String,
    @ColumnInfo(name = "amount_minor")   val amountMinor: Long,
    val currency: String,
    val direction: String,
    val date: String,
    val description: String? = null,
    val counterparty: String? = null,
    @ColumnInfo(name = "source_package")   val sourcePackage: String,
    @ColumnInfo(name = "source_app_label") val sourceAppLabel: String? = null,
    @ColumnInfo(name = "status", defaultValue = "PENDING") val status: String = "PENDING",
    @ColumnInfo(name = "created_transaction_id") val createdTransactionId: Long? = null,
    @ColumnInfo(name = "captured_at")    val capturedAt: Long,
    @ColumnInfo(name = "decided_at")     val decidedAt: Long? = null,
)
