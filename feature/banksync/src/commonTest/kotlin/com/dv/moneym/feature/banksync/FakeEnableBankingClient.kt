package com.dv.moneym.feature.banksync

import com.dv.moneym.data.banksync.EbAccountInfo
import com.dv.moneym.data.banksync.EbAuthStart
import com.dv.moneym.data.banksync.EbBank
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.data.banksync.EbSessionInfo
import com.dv.moneym.data.banksync.EbSessionStatus
import com.dv.moneym.data.banksync.EbTransactionsPage
import com.dv.moneym.data.banksync.EnableBankingClient
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal class FakeEnableBankingClient : EnableBankingClient {
    var validateResult: Result<Unit> = Result.success(Unit)
    var banks: List<EbBank> = emptyList()
    var authUrl: String = "https://bank.example/auth"
    var sessionInfo: EbSessionInfo = EbSessionInfo(
        sessionId = "sess-1",
        accounts = listOf(EbAccountInfo(uid = "acc-1", iban = "SK00", name = "Main", currency = "EUR")),
        validUntil = Instant.parse("2026-12-01T00:00:00Z"),
    )
    var createSessionError: EbError? = null
    var startAuthError: EbError? = null
    var deletedSessionId: String? = null

    override suspend fun validateCredentials(credentials: EbCredentials) = validateResult

    override suspend fun listBanks(country: String) = Result.success(banks)

    override suspend fun startAuth(bank: EbBank, redirectUrl: String, validUntil: Instant, state: String) =
        startAuthError?.let { Result.failure(it) } ?: Result.success(EbAuthStart(authUrl))

    override suspend fun createSession(code: String): Result<EbSessionInfo> =
        createSessionError?.let { Result.failure(it) } ?: Result.success(sessionInfo)

    override suspend fun getSessionStatus(sessionId: String) =
        Result.success(EbSessionStatus("AUTHORIZED", sessionInfo.validUntil))

    override suspend fun fetchTransactions(accountUid: String, dateFrom: LocalDate, continuationKey: String?) =
        Result.success(EbTransactionsPage(emptyList()))

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        deletedSessionId = sessionId
        return Result.success(Unit)
    }
}
