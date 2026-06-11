package com.dv.moneym.data.banksync.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "BankSuggestion",
    indices = [
        Index(value = ["external_id"], name = "idx_bs_external", unique = true),
        Index(value = ["status"],      name = "idx_bs_status"),
    ],
)
data class BankSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "external_id")      val externalId: String,
    @ColumnInfo(name = "bank_account_uid") val bankAccountUid: String,
    @ColumnInfo(name = "amount_minor")     val amountMinor: Long,
    val currency: String,
    val direction: String,
    @ColumnInfo(name = "booking_date")     val bookingDate: String,
    @ColumnInfo(name = "value_date")       val valueDate: String? = null,
    val description: String? = null,
    val counterparty: String? = null,
    @ColumnInfo(name = "status", defaultValue = "PENDING") val status: String = "PENDING",
    @ColumnInfo(name = "created_transaction_id") val createdTransactionId: Long? = null,
    @ColumnInfo(name = "fetched_at")       val fetchedAt: Long,
    @ColumnInfo(name = "decided_at")       val decidedAt: Long? = null,
)
