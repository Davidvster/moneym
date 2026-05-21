package com.dv.moneym.data.transactions

import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import com.dv.moneym.data.transactions.internal.SqlDelightTransactionDataSource
import com.dv.moneym.data.transactions.internal.TransactionRepositoryImpl

fun createTransactionRepository(
    db: TransactionsRoomDatabase,
): TransactionRepository = TransactionRepositoryImpl(
    SqlDelightTransactionDataSource(db)
)
