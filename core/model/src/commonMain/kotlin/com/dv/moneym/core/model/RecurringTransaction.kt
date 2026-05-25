package com.dv.moneym.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class RecurringTransaction(
    val id: RecurringTransactionId,
    val type: TransactionType,
    val amount: Money,
    val note: String?,
    val categoryId: CategoryId,
    val accountId: AccountId,
    val paymentModeId: PaymentModeId?,
    val startDate: LocalDate,
    val rule: RecurrenceRule,
    val endCondition: EndCondition,
    val lastMaterializedDate: LocalDate?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

val UNSAVED_RECURRING_ID = RecurringTransactionId(0L)

@Serializable
sealed interface RecurrenceRule {
    val interval: Int

    @Serializable
    data class Daily(override val interval: Int) : RecurrenceRule

    @Serializable
    data class Weekly(
        override val interval: Int,
        val dayOfWeek: Int,
    ) : RecurrenceRule

    @Serializable
    data class Monthly(
        override val interval: Int,
        val dayKind: MonthlyDayKind,
    ) : RecurrenceRule
}

@Serializable
sealed interface MonthlyDayKind {
    @Serializable data class OnDay(val day: Int) : MonthlyDayKind
    @Serializable data object LastDay : MonthlyDayKind
}

@Serializable
sealed interface EndCondition {
    @Serializable data object Unlimited : EndCondition
    @Serializable data class Count(val occurrences: Int) : EndCondition
    @Serializable data class Until(val date: LocalDate) : EndCondition
}
