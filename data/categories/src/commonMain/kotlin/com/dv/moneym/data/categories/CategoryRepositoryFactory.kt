package com.dv.moneym.data.categories

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.categories.db.CategoriesDatabase
import com.dv.moneym.data.categories.internal.CategoryRepositoryImpl
import com.dv.moneym.data.categories.internal.SqlDelightCategoryDataSource

fun createCategoryRepository(
    db: CategoriesDatabase,
    dispatchers: DispatcherProvider,
): CategoryRepository = CategoryRepositoryImpl(
    SqlDelightCategoryDataSource(db, dispatchers)
)
