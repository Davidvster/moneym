package com.dv.moneym.data.transactions.internal

import androidx.room.InvalidationTracker
import com.dv.moneym.data.transactions.db.PaymentModeDao
import com.dv.moneym.data.transactions.db.RecurringTransactionDao
import com.dv.moneym.data.transactions.db.TransactionDao
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase

internal class FakeTransactionsRoomDatabase(
    private val paymentModeDao: PaymentModeDao = FakePaymentModeDao(),
) : TransactionsRoomDatabase() {

    override fun transactionDao(): TransactionDao =
        throw UnsupportedOperationException("not needed for SeedPaymentModesUseCase")

    override fun paymentModeDao(): PaymentModeDao = paymentModeDao

    override fun recurringTransactionDao(): RecurringTransactionDao =
        throw UnsupportedOperationException("not needed for SeedPaymentModesUseCase")

    override fun createInvalidationTracker(): InvalidationTracker =
        throw UnsupportedOperationException("not needed for SeedPaymentModesUseCase")

    override fun clearAllTables(): Unit =
        throw UnsupportedOperationException("not needed for SeedPaymentModesUseCase")
}
