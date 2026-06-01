package com.dv.moneym.data.budgets.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Budget")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "amount_minor")      val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "category_id")       val categoryId: Long?,
    @ColumnInfo(name = "account_id")        val accountId: Long,
    @ColumnInfo(name = "period_type")       val periodType: String,
    @ColumnInfo(name = "start_year_month")  val startYearMonth: String,
    @ColumnInfo(name = "recurring_months")  val recurringMonths: Int?,
    @ColumnInfo(name = "created_at")        val createdAt: Long,
    @ColumnInfo(name = "updated_at")        val updatedAt: Long,
    @ColumnInfo(name = "sync_id")           val syncId: String? = null,
    @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
)
