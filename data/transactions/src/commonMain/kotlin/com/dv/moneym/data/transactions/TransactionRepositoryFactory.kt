package com.dv.moneym.data.transactions

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.transactions.db.TransactionsDatabase
import com.dv.moneym.data.transactions.internal.SqlDelightTransactionDataSource
import com.dv.moneym.data.transactions.internal.TransactionRepositoryImpl

fun createTransactionRepository(
    db: TransactionsDatabase,
    dispatchers: DispatcherProvider,
): TransactionRepository = TransactionRepositoryImpl(
    SqlDelightTransactionDataSource(db, dispatchers)
)
