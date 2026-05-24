package com.dv.moneym.feature.settings.overview.currencypicker

sealed interface CurrencyPickerIntent {
    data class SetDefaultCurrency(val code: String) : CurrencyPickerIntent
}
