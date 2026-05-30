package com.dv.moneym.data.categories

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.TransactionType
import kotlin.time.Instant

private val epoch = Instant.fromEpochMilliseconds(0)

data class DefaultCategorySpec(
    val icon: Icon,
    val color: String,
    val type: TransactionType,
)

fun DefaultCategorySpec.toCategory(name: String): Category = Category(
    CategoryId(0), name, icon.key, color, isUserCreated = false, archived = false, epoch, epoch, type
)

val defaultCategorySpecs: List<DefaultCategorySpec> = listOf(
    DefaultCategorySpec(Icon.Basket,   "#7E9C8C", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Utensils, "#C97B57", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Home,     "#5F6F8A", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Car,      "#3B7080", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Bolt,     "#B89A4B", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Heart,    "#9B5C7D", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Film,     "#7C5C9B", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Bag,      "#6D6D6D", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Tag,      "#8A8A8A", TransactionType.EXPENSE),
    DefaultCategorySpec(Icon.Wallet,   "#4A7A56", TransactionType.INCOME),
    DefaultCategorySpec(Icon.Banknote, "#5A8A66", TransactionType.INCOME),
    DefaultCategorySpec(Icon.Gift,     "#9B7A4B", TransactionType.INCOME),
    DefaultCategorySpec(Icon.Tag,      "#4A7A56", TransactionType.INCOME),
)

private val englishNames = listOf(
    "Groceries", "Eating out", "Rent", "Transport", "Utilities",
    "Health", "Entertainment", "Shopping", "Other",
    "Salary", "Payment", "Gift", "Other",
)

val defaultCategories: List<Category> =
    defaultCategorySpecs.zip(englishNames) { spec, name -> spec.toCategory(name) }
