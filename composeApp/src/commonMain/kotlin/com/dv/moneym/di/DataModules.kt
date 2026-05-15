package com.dv.moneym.di

import com.dv.moneym.core.database.SqlDriverFactory
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.accounts.createAccountRepository
import com.dv.moneym.data.accounts.db.AccountsDatabase
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.data.categories.createCategoryRepository
import com.dv.moneym.data.categories.db.CategoriesDatabase
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.createTransactionRepository
import com.dv.moneym.data.transactions.db.TransactionsDatabase
import org.koin.dsl.module

val dataCategoriesModule = module {
    single {
        CategoriesDatabase(get<SqlDriverFactory>().create(CategoriesDatabase.Schema, "moneym_categories.db"))
    }
    single<CategoryRepository> { createCategoryRepository(get(), get()) }
    single { SeedCategoriesUseCase(get()) }
}

val dataAccountsModule = module {
    single {
        AccountsDatabase(get<SqlDriverFactory>().create(AccountsDatabase.Schema, "moneym_accounts.db"))
    }
    single<AccountRepository> { createAccountRepository(get(), get()) }
    single { SeedAccountsUseCase(get(), get()) }
}

val dataTransactionsModule = module {
    single {
        TransactionsDatabase(get<SqlDriverFactory>().create(TransactionsDatabase.Schema, "moneym_transactions.db"))
    }
    single<TransactionRepository> { createTransactionRepository(get(), get()) }
}
