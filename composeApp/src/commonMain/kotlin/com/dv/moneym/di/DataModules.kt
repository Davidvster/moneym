package com.dv.moneym.di

import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.accounts.createAccountRepository
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.budgets.createBudgetRepository
import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.data.categories.createCategoryRepository
import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.SeedPaymentModesUseCase
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.createPaymentModeRepository
import com.dv.moneym.data.transactions.createTransactionRepository
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import org.koin.dsl.module

val dataCategoriesModule = module {
    single<CategoryRepository> { createCategoryRepository(get<CategoriesRoomDatabase>()) }
    single { SeedCategoriesUseCase(get()) }
}

val dataAccountsModule = module {
    single<AccountRepository> { createAccountRepository(get<AccountsRoomDatabase>()) }
    single { SeedAccountsUseCase(get(), get()) }
}

val dataTransactionsModule = module {
    single<TransactionRepository> { createTransactionRepository(get<TransactionsRoomDatabase>()) }
    single<PaymentModeRepository> { createPaymentModeRepository(get<TransactionsRoomDatabase>()) }
    single { SeedPaymentModesUseCase(get()) }
}

val dataBudgetsModule = module {
    single<BudgetRepository> { createBudgetRepository(get<BudgetsRoomDatabase>()) }
}
