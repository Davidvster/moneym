package com.dv.moneym.data.banksync

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.data.banksync.internal.DefaultEnableBankingClient
import com.dv.moneym.data.banksync.internal.EbJwtSigner
import io.ktor.client.HttpClient

fun createEnableBankingClient(
    httpClient: HttpClient,
    credentialsStore: EnableBankingCredentialsStore,
    clock: AppClock,
): EnableBankingClient = DefaultEnableBankingClient(
    httpClient = httpClient,
    credentialsStore = credentialsStore,
    signer = EbJwtSigner(),
    clock = clock,
)
