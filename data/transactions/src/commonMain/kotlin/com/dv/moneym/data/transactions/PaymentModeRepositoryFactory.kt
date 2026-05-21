package com.dv.moneym.data.transactions

import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import com.dv.moneym.data.transactions.internal.SqlDelightPaymentModeDataSource

fun createPaymentModeRepository(
    db: TransactionsRoomDatabase,
): PaymentModeRepository = SqlDelightPaymentModeDataSource(db)
