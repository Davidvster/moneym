package com.dv.moneym

import com.dv.moneym.core.common.AppLogger
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.categories.SeedCategoriesUseCase

private val logger = AppLogger.tag("App")

class AppInitializer(
    private val seedCategories: SeedCategoriesUseCase,
    private val seedAccounts: SeedAccountsUseCase,
    private val lockController: AppLockController,
) {
    suspend fun initialize() {
        seedCategories()
        seedAccounts()
        lockController.init()
        logger.d { "App bootstrap complete" }
    }
}
