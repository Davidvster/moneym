package com.dv.moneym.data.transactions

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.transactions.db.TransactionsDatabase
import com.dv.moneym.data.transactions.internal.SqlDelightPaymentModeDataSource

fun createPaymentModeRepository(
    db: TransactionsDatabase,
    dispatchers: DispatcherProvider,
): PaymentModeRepository = SqlDelightPaymentModeDataSource(db, dispatchers)
