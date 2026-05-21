package com.dv.moneym.data.categories

import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import com.dv.moneym.data.categories.internal.CategoryRepositoryImpl
import com.dv.moneym.data.categories.internal.SqlDelightCategoryDataSource

fun createCategoryRepository(
    db: CategoriesRoomDatabase,
): CategoryRepository = CategoryRepositoryImpl(
    SqlDelightCategoryDataSource(db)
)
