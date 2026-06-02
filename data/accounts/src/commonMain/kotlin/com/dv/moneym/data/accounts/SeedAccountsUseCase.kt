package com.dv.moneym.data.accounts

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.AccountType

class SeedAccountsUseCase(
    private val repository: AccountRepository,
    private val settings: AppSettings,
    private val clock: AppClock,
    private val defaultName: suspend () -> String,
) {
    suspend operator fun invoke() {
        if (repository.count() > 0L) return
        val now = clock.now().toEpochMilliseconds()
        val currency = settings.getString(PrefKeys.DEFAULT_CURRENCY, "EUR") ?: "EUR"
        repository.upsertFromSync(
            AccountSyncRow(
                id = 0,
                syncId = "seed-account-default",
                name = defaultName(),
                type = AccountType.CASH.name,
                currency = currency,
                isDefault = true,
                archived = false,
                colorHex = null,
                deleted = false,
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}
