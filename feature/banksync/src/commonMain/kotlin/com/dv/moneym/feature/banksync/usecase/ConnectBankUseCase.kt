package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.data.banksync.EbAuthStart
import com.dv.moneym.data.banksync.EbBank
import com.dv.moneym.data.banksync.EnableBankingClient
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ConnectBankUseCase(
    private val client: EnableBankingClient,
    private val clock: AppClock,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(bankName: String, country: String): Result<EbAuthStart> =
        client.startAuth(
            bank = EbBank(name = bankName, country = country),
            redirectUrl = REDIRECT_URL,
            validUntil = clock.now() + SESSION_VALIDITY_DAYS.days,
            state = Uuid.random().toString(),
        )

    companion object {
        const val REDIRECT_URL = "https://davidvster.github.io/moneym.github.io/bank-callback.html"
        const val SESSION_VALIDITY_DAYS = 180
    }
}
