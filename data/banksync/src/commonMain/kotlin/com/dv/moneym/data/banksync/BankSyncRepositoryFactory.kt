package com.dv.moneym.data.banksync

import com.dv.moneym.data.banksync.db.BankSyncRoomDatabase
import com.dv.moneym.data.banksync.internal.BankSyncRepositoryImpl

fun createBankSyncRepository(db: BankSyncRoomDatabase): BankSyncRepository =
    BankSyncRepositoryImpl(db)
