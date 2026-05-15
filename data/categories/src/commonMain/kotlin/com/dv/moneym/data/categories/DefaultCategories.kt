package com.dv.moneym.data.categories

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import kotlin.time.Instant

// Epoch 0 used as placeholder; actual timestamps written by the repo on insert.
private val epoch = Instant.fromEpochMilliseconds(0)
private fun seed(name: String, icon: String, color: String) =
    Category(CategoryId(0), name, icon, color, isUserCreated = false, archived = false, epoch, epoch)

val defaultCategories: List<Category> = listOf(
    seed("Groceries",      "cart",        "#7E9C8C"),
    seed("Eating out",     "restaurant",  "#C97B57"),
    seed("Rent",           "home",        "#5F6F8A"),
    seed("Transport",      "car",         "#3B7080"),
    seed("Utilities",      "bolt",        "#B89A4B"),
    seed("Health",         "heart_pulse", "#9B5C7D"),
    seed("Entertainment",  "play_circle", "#7C5C9B"),
    seed("Shopping",       "bag",         "#6D6D6D"),
    seed("Salary",         "wallet",      "#4A7A56"),
    seed("Other (expense)","dots",        "#8A8A8A"),
    seed("Other (income)", "dots",        "#4A7A56"),
)
