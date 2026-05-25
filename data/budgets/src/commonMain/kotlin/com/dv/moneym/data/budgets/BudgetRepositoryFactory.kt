package com.dv.moneym.data.budgets

import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import com.dv.moneym.data.budgets.internal.BudgetRepositoryImpl
import com.dv.moneym.data.budgets.internal.RoomBudgetDataSource

fun createBudgetRepository(
    db: BudgetsRoomDatabase,
): BudgetRepository = BudgetRepositoryImpl(
    RoomBudgetDataSource(db)
)
