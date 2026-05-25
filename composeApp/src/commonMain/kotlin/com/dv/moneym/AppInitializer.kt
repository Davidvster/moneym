package com.dv.moneym

import com.dv.moneym.core.common.AppLogger
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.data.transactions.MaterializeRecurringTransactionsUseCase
import com.dv.moneym.data.transactions.SeedPaymentModesUseCase

private val logger = AppLogger.tag("App")

class AppInitializer(
    private val seedCategories: SeedCategoriesUseCase,
    private val seedAccounts: SeedAccountsUseCase,
    private val lockController: AppLockController,
    private val seedPaymentModes: SeedPaymentModesUseCase,
    private val materializeRecurringTransactions: MaterializeRecurringTransactionsUseCase,
) {
    suspend fun initialize() {
        seedCategories()
        seedAccounts()
        seedPaymentModes()
        materializeRecurringTransactions()
        lockController.init()
        logger.d { "App bootstrap complete" }
    }
}
