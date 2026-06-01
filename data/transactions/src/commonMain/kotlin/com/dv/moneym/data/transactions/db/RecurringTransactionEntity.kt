package com.dv.moneym.data.transactions.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "RecurringTransactionEntry",
    indices = [
        Index(value = ["category_id"], name = "idx_rte_category"),
        Index(value = ["account_id"],  name = "idx_rte_account"),
    ],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_minor")     val amountMinor: Long,
    val currency: String,
    val note: String? = null,
    @ColumnInfo(name = "category_id")      val categoryId: Long,
    @ColumnInfo(name = "account_id")       val accountId: Long,
    @ColumnInfo(name = "payment_mode_id")  val paymentModeId: Long? = null,
    @ColumnInfo(name = "start_date")       val startDate: String,
    @ColumnInfo(name = "freq_unit")        val freqUnit: String,
    @ColumnInfo(name = "freq_interval")    val freqInterval: Int,
    @ColumnInfo(name = "day_of_week")      val dayOfWeek: Int? = null,
    @ColumnInfo(name = "day_of_month")     val dayOfMonth: Int? = null,
    @ColumnInfo(name = "use_last_day")     val useLastDay: Boolean = false,
    @ColumnInfo(name = "end_kind")         val endKind: String,
    @ColumnInfo(name = "end_count")        val endCount: Int? = null,
    @ColumnInfo(name = "end_date")         val endDate: String? = null,
    @ColumnInfo(name = "last_materialized") val lastMaterializedDate: String? = null,
    @ColumnInfo(name = "created_at")       val createdAt: Long,
    @ColumnInfo(name = "updated_at")       val updatedAt: Long,
    @ColumnInfo(name = "sync_id")          val syncId: String? = null,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
)
