package com.dv.moneym.core.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.dv.moneym.core.model.Icon

val Icon.imageVector: ImageVector get() = MmIcons.forIcon(this)
