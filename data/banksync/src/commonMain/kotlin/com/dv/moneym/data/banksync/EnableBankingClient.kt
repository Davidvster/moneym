package com.dv.moneym.data.banksync

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

data class EbBank(
    val name: String,
    val country: String,
    val logoUrl: String? = null,
)

data class EbAuthStart(val url: String)

data class EbAccountInfo(
    val uid: String,
    val iban: String? = null,
    val name: String? = null,
    val currency: String? = null,
)

data class EbSessionInfo(
    val sessionId: String,
    val accounts: List<EbAccountInfo>,
    val validUntil: Instant? = null,
)

data class EbSessionStatus(
    val status: String?,
    val validUntil: Instant? = null,
)

enum class EbDirection { CREDIT, DEBIT }

data class EbTransactionData(
    val entryReference: String?,
    val amountMinor: Long,
    val currency: String,
    val direction: EbDirection,
    val bookingDate: LocalDate,
    val valueDate: LocalDate? = null,
    val description: String? = null,
    val counterparty: String? = null,
    val booked: Boolean = true,
)

data class EbTransactionsPage(
    val transactions: List<EbTransactionData>,
    val continuationKey: String? = null,
)

interface EnableBankingClient {
    suspend fun validateCredentials(credentials: EbCredentials): Result<Unit>
    suspend fun listBanks(country: String): Result<List<EbBank>>
    suspend fun startAuth(
        bank: EbBank,
        redirectUrl: String,
        validUntil: Instant,
        state: String,
    ): Result<EbAuthStart>

    suspend fun createSession(code: String): Result<EbSessionInfo>
    suspend fun getSessionStatus(sessionId: String): Result<EbSessionStatus>
    suspend fun fetchTransactions(
        accountUid: String,
        dateFrom: LocalDate,
        continuationKey: String? = null,
    ): Result<EbTransactionsPage>

    suspend fun deleteSession(sessionId: String): Result<Unit>
}
