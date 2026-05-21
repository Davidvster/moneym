package com.dv.moneym.data.transactions.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "TransactionEntry",
    indices = [
        Index(value = ["occurred_on"],  name = "idx_te_date"),
        Index(value = ["category_id"],  name = "idx_te_category"),
        Index(value = ["account_id"],   name = "idx_te_account"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_minor")    val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "occurred_on")     val occurredOn: String,     // yyyy-MM-dd
    val note: String? = null,
    @ColumnInfo(name = "category_id")     val categoryId: Long,
    @ColumnInfo(name = "account_id")      val accountId: Long,
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String? = null,
    @ColumnInfo(name = "created_at")      val createdAt: Long,
    @ColumnInfo(name = "updated_at")      val updatedAt: Long,
    @ColumnInfo(name = "payment_mode_id") val paymentModeId: Long? = null,
)
