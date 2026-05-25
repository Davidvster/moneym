package com.dv.moneym.core.model

import kotlinx.serialization.Serializable

// Using data classes rather than value classes for cross-platform compatibility.
// Value class (@JvmInline required on JVM, unavailable on K/N) can be revisited
// when Kotlin/KMP provides a clean multiplatform solution.
@Serializable data class TransactionId(val value: Long)
@Serializable data class CategoryId(val value: Long)
@Serializable data class AccountId(val value: Long)
@Serializable data class BudgetId(val value: Long)
@Serializable data class RecurringTransactionId(val value: Long)
