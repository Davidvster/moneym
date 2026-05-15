package com.dv.moneym.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

val categoryIconRegistry: Map<String, ImageVector> = mapOf(
    "cart"         to Icons.Filled.ShoppingCart,
    "restaurant"   to Icons.Filled.Restaurant,
    "home"         to Icons.Filled.Home,
    "car"          to Icons.Filled.DirectionsCar,
    "bolt"         to Icons.Filled.Bolt,
    "heart_pulse"  to Icons.Filled.Favorite,
    "health"       to Icons.Filled.LocalHospital,
    "play_circle"  to Icons.Filled.Movie,
    "bag"          to Icons.Filled.ShoppingBag,
    "wallet"       to Icons.Filled.Payments,
    "bank"         to Icons.Filled.AccountBalance,
    "dots"         to Icons.Filled.MoreHoriz,
)

fun iconForKey(key: String): ImageVector =
    categoryIconRegistry[key] ?: Icons.Filled.MoreHoriz
