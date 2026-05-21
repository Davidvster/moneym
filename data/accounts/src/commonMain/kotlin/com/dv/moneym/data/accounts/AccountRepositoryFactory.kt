package com.dv.moneym.data.accounts

import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.accounts.internal.AccountRepositoryImpl
import com.dv.moneym.data.accounts.internal.SqlDelightAccountDataSource

fun createAccountRepository(
    db: AccountsRoomDatabase,
): AccountRepository = AccountRepositoryImpl(
    SqlDelightAccountDataSource(db)
)
