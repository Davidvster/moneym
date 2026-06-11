package com.dv.moneym.data.banksync.internal

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.data.banksync.EbAccountInfo
import com.dv.moneym.data.banksync.EbAuthStart
import com.dv.moneym.data.banksync.EbBank
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.data.banksync.EbSessionInfo
import com.dv.moneym.data.banksync.EbSessionStatus
import com.dv.moneym.data.banksync.EbTransactionData
import com.dv.moneym.data.banksync.EbTransactionsPage
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal class DefaultEnableBankingClient(
    private val httpClient: HttpClient,
    private val credentialsStore: EnableBankingCredentialsStore,
    private val signer: EbJwtSigner,
    private val clock: AppClock,
    private val baseUrl: String = "https://api.enablebanking.com",
) : EnableBankingClient {

    override suspend fun validateCredentials(credentials: EbCredentials): Result<Unit> = runCatching {
        val jwt = signer.sign(credentials.applicationId, credentials.privateKeyPem, clock.now())
        val response = httpClient.get("$baseUrl/application") {
            header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        response.ensureSuccess()
        response.body<EbApplicationDto>()
        Unit
    }.mapNetworkFailure()

    override suspend fun listBanks(country: String): Result<List<EbBank>> = runCatching {
        val response = httpClient.get("$baseUrl/aspsps") {
            authorize()
            parameter("country", country)
        }
        response.ensureSuccess()
        response.body<EbAspspsResponseDto>().aspsps.map {
            EbBank(name = it.name, country = it.country, logoUrl = it.logo)
        }
    }.mapNetworkFailure()

    override suspend fun startAuth(
        bank: EbBank,
        redirectUrl: String,
        validUntil: Instant,
        state: String,
    ): Result<EbAuthStart> = runCatching {
        val response = httpClient.post("$baseUrl/auth") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(
                EbAuthRequestDto(
                    access = EbAccessDto(validUntil = validUntil.toString()),
                    aspsp = EbAspspRefDto(name = bank.name, country = bank.country),
                    state = state,
                    redirectUrl = redirectUrl,
                )
            )
        }
        response.ensureSuccess()
        EbAuthStart(url = response.body<EbAuthResponseDto>().url)
    }.mapNetworkFailure()

    override suspend fun createSession(code: String): Result<EbSessionInfo> = runCatching {
        val response = httpClient.post("$baseUrl/sessions") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(EbSessionRequestDto(code = code))
        }
        response.ensureSuccess()
        val dto = response.body<EbSessionResponseDto>()
        EbSessionInfo(
            sessionId = dto.sessionId,
            accounts = dto.accounts.mapNotNull { account ->
                val uid = account.uid ?: return@mapNotNull null
                EbAccountInfo(
                    uid = uid,
                    iban = account.accountId?.iban,
                    name = account.name,
                    currency = account.currency,
                )
            },
            validUntil = dto.access?.validUntil?.let { Instant.parse(it) },
        )
    }.mapNetworkFailure()

    override suspend fun getSessionStatus(sessionId: String): Result<EbSessionStatus> = runCatching {
        val response = httpClient.get("$baseUrl/sessions/$sessionId") { authorize() }
        response.ensureSuccess()
        val dto = response.body<EbSessionStatusResponseDto>()
        EbSessionStatus(
            status = dto.status,
            validUntil = dto.access?.validUntil?.let { Instant.parse(it) },
        )
    }.mapNetworkFailure()

    override suspend fun fetchTransactions(
        accountUid: String,
        dateFrom: LocalDate,
        continuationKey: String?,
    ): Result<EbTransactionsPage> = runCatching {
        val response = httpClient.get("$baseUrl/accounts/$accountUid/transactions") {
            authorize()
            parameter("date_from", dateFrom.toString())
            continuationKey?.let { parameter("continuation_key", it) }
        }
        response.ensureSuccess()
        val dto = response.body<EbTransactionsResponseDto>()
        EbTransactionsPage(
            transactions = dto.transactions.mapNotNull { it.toData() },
            continuationKey = dto.continuationKey,
        )
    }.mapNetworkFailure()

    override suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        val response = httpClient.delete("$baseUrl/sessions/$sessionId") { authorize() }
        response.ensureSuccess()
        Unit
    }.mapNetworkFailure()

    private suspend fun HttpRequestBuilder.authorize() {
        val credentials = credentialsStore.loadCredentials()
            ?: throw EbError.Unauthorized("Enable Banking credentials are not configured")
        val jwt = signer.sign(credentials.applicationId, credentials.privateKeyPem, clock.now())
        header(HttpHeaders.Authorization, "Bearer $jwt")
    }

    private suspend fun HttpResponse.ensureSuccess() {
        if (status.isSuccess()) return
        val detail = runCatching { bodyAsText() }.getOrDefault("")
        throw when (status.value) {
            401, 403 -> EbError.Unauthorized("HTTP ${status.value}: $detail")
            410, 422 -> EbError.SessionExpired("HTTP ${status.value}: $detail")
            429 -> EbError.RateLimited("HTTP 429: $detail")
            else -> EbError.Api(status.value, "HTTP ${status.value}: $detail")
        }
    }

    private fun <T> Result<T>.mapNetworkFailure(): Result<T> = recoverCatching { t ->
        if (t is EbError) throw t else throw EbError.Network(t)
    }
}

private fun EbTransactionDto.toData(): EbTransactionData? {
    val bookingDate = bookingDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val direction = when (creditDebitIndicator.uppercase()) {
        "CRDT" -> EbDirection.CREDIT
        "DBIT" -> EbDirection.DEBIT
        else -> return null
    }
    return EbTransactionData(
        entryReference = entryReference?.takeIf { it.isNotBlank() },
        amountMinor = parseAmountToMinorUnits(transactionAmount.amount, transactionAmount.currency),
        currency = transactionAmount.currency.uppercase(),
        direction = direction,
        bookingDate = bookingDate,
        valueDate = valueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        description = remittanceInformation.filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
        counterparty = when (direction) {
            EbDirection.DEBIT -> creditor?.name
            EbDirection.CREDIT -> debtor?.name
        },
        booked = status?.uppercase() != "PDNG",
    )
}
