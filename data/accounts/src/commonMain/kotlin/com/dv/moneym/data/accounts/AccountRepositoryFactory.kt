package com.dv.moneym.data.accounts

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.accounts.db.AccountsDatabase
import com.dv.moneym.data.accounts.internal.AccountRepositoryImpl
import com.dv.moneym.data.accounts.internal.SqlDelightAccountDataSource

fun createAccountRepository(
    db: AccountsDatabase,
    dispatchers: DispatcherProvider,
): AccountRepository = AccountRepositoryImpl(
    SqlDelightAccountDataSource(db, dispatchers)
)
