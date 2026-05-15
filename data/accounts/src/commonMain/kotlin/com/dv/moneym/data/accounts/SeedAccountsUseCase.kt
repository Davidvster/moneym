package com.dv.moneym.data.accounts

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import kotlin.time.Instant

class SeedAccountsUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke() {
        if (repository.count() == 0L) {
            val epoch = Instant.fromEpochMilliseconds(0)
            repository.insert(
                Account(
                    id = AccountId(0),
                    name = "Main",
                    type = AccountType.CASH,
                    // TODO Phase 5: replace with user-selected currency from onboarding
                    currency = CurrencyCode("EUR"),
                    isDefault = true,
                    archived = false,
                    createdAt = epoch,
                    updatedAt = epoch,
                )
            )
        }
    }
}
