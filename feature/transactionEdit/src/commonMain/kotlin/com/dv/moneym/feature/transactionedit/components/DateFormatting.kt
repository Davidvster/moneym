package com.dv.moneym.feature.transactionedit.components

import com.dv.moneym.core.common.DateStyle
import com.dv.moneym.core.common.formatDate
import kotlinx.datetime.LocalDate

internal fun LocalDate.toFriendlyString(today: LocalDate): String {
    return formatDate(this, DateStyle.Medium)
}
