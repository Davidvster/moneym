package com.dv.moneym.core.model

enum class IndicatorStyle { IconTile, SoftIcon, Bar, Dot, Minimal }

enum class Density { Compact, Normal, Comfortable }

data class TxDisplayPrefs(
    val indicatorStyle: IndicatorStyle = IndicatorStyle.IconTile,
    val showCategoryName: Boolean = true,
    val showNote: Boolean = true,
    val density: Density = Density.Comfortable,
)
