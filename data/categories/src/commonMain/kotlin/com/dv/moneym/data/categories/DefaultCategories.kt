package com.dv.moneym.data.categories

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import kotlin.time.Instant

// Epoch 0 used as placeholder; actual timestamps written by the repo on insert.
private val epoch = Instant.fromEpochMilliseconds(0)
private fun seed(name: String, icon: Icon, color: String) =
    Category(CategoryId(0), name, icon.key, color, isUserCreated = false, archived = false, epoch, epoch)

val defaultCategories: List<Category> = listOf(
    seed("Groceries",      Icon.Basket,   "#7E9C8C"),
    seed("Eating out",     Icon.Utensils, "#C97B57"),
    seed("Rent",           Icon.Home,     "#5F6F8A"),
    seed("Transport",      Icon.Car,      "#3B7080"),
    seed("Utilities",      Icon.Bolt,     "#B89A4B"),
    seed("Health",         Icon.Heart,    "#9B5C7D"),
    seed("Entertainment",  Icon.Film,     "#7C5C9B"),
    seed("Shopping",       Icon.Bag,      "#6D6D6D"),
    seed("Salary",         Icon.Wallet,   "#4A7A56"),
    seed("Other (expense)",Icon.Tag,      "#8A8A8A"),
    seed("Other (income)", Icon.Tag,      "#4A7A56"),
)
