package com.dv.moneym.data.accounts

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import kotlin.time.Instant

class SeedAccountsUseCase(
    private val repository: AccountRepository,
    private val settings: AppSettings,
) {
    suspend operator fun invoke() {
        if (repository.count() > 0L) return
        if (!settings.getBoolean(PrefKeys.ONBOARDING_COMPLETED)) return
        val epoch = Instant.fromEpochMilliseconds(0)
        repository.insert(
            Account(
                id = AccountId(0),
                name = "Main",
                type = AccountType.CASH,
                currency = CurrencyCode("USD"),
                isDefault = true,
                archived = false,
                createdAt = epoch,
                updatedAt = epoch,
            )
        )
    }
}
