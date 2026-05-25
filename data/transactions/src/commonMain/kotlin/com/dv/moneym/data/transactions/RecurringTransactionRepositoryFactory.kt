package com.dv.moneym.data.transactions

import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import com.dv.moneym.data.transactions.internal.RecurringTransactionRepositoryImpl

fun createRecurringTransactionRepository(
    db: TransactionsRoomDatabase,
): RecurringTransactionRepository = RecurringTransactionRepositoryImpl(db.recurringTransactionDao())
