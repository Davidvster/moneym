package com.dv.moneym.data.banksync.internal

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.data.banksync.EbBank
import com.dv.moneym.data.banksync.EbCredentials
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.EbError
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

private class InMemorySecureStore : SecureStore {
    private val map = mutableMapOf<String, ByteArray>()
    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) { map[key] = value }
    override suspend fun get(key: String): ByteArray? = map[key]
    override suspend fun remove(key: String) { map.remove(key) }
}

private class FixedAppClock : AppClock {
    override fun now(): Instant = Instant.fromEpochSeconds(1_700_000_000)
    override fun today(): LocalDate = LocalDate(2026, 6, 1)
}

class DefaultEnableBankingClientTest {

    private suspend fun generatePem(): String {
        val keyPair = platformCryptographyProvider().get(RSA.PKCS1).keyPairGenerator(digest = SHA256).generateKey()
        return keyPair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.PEM).decodeToString()
    }

    private suspend fun client(
        store: EnableBankingCredentialsStore? = null,
        handler: suspend (path: String, query: String) -> Pair<HttpStatusCode, String>,
    ): DefaultEnableBankingClient {
        val credentialsStore = store ?: configuredStore()
        val engine = MockEngine { request ->
            val (status, body) = handler(request.url.encodedPath, request.url.encodedQuery)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }
        return DefaultEnableBankingClient(
            httpClient = httpClient,
            credentialsStore = credentialsStore,
            signer = EbJwtSigner(platformCryptographyProvider()),
            clock = FixedAppClock(),
        )
    }

    private suspend fun configuredStore(): EnableBankingCredentialsStore {
        val store = EnableBankingCredentialsStore(InMemorySecureStore())
        store.saveCredentials(EbCredentials("app-1", generatePem()))
        return store
    }

    @Test
    fun listsBanksForCountry() = runTest {
        val c = client { path, query ->
            assertEquals("/aspsps", path)
            assertTrue(query.contains("country=SK"))
            HttpStatusCode.OK to """{"aspsps":[{"name":"Tatra","country":"SK","logo":"https://x/l.png"},{"name":"VUB","country":"SK"}]}"""
        }
        val banks = c.listBanks("SK").getOrThrow()
        assertEquals(listOf("Tatra", "VUB"), banks.map { it.name })
    }

    @Test
    fun startAuthReturnsUrl() = runTest {
        val c = client { path, _ ->
            assertEquals("/auth", path)
            HttpStatusCode.OK to """{"url":"https://bank.example/authorize?x=1"}"""
        }
        val auth = c.startAuth(
            bank = EbBank("Tatra", "SK"),
            redirectUrl = "moneym://bank-callback",
            validUntil = Instant.fromEpochSeconds(2_000_000_000),
            state = "state-1",
        ).getOrThrow()
        assertEquals("https://bank.example/authorize?x=1", auth.url)
    }

    @Test
    fun createSessionParsesAccounts() = runTest {
        val c = client { path, _ ->
            assertEquals("/sessions", path)
            HttpStatusCode.OK to """
                {"session_id":"sess-1",
                 "accounts":[
                   {"uid":"acc-1","account_id":{"iban":"SK0000"},"name":"Main","currency":"EUR"},
                   {"name":"NoUid"}
                 ],
                 "access":{"valid_until":"2026-12-01T00:00:00Z"}}
            """.trimIndent()
        }
        val session = c.createSession("code-1").getOrThrow()
        assertEquals("sess-1", session.sessionId)
        assertEquals(1, session.accounts.size)
        assertEquals("acc-1", session.accounts.first().uid)
        assertEquals("SK0000", session.accounts.first().iban)
        assertEquals(Instant.parse("2026-12-01T00:00:00Z"), session.validUntil)
    }

    @Test
    fun fetchTransactionsPaginatesAndMaps() = runTest {
        val c = client { path, query ->
            assertEquals("/accounts/acc-1/transactions", path)
            if (!query.contains("continuation_key")) {
                HttpStatusCode.OK to """
                    {"transactions":[
                       {"entry_reference":"r1",
                        "transaction_amount":{"currency":"EUR","amount":"12.50"},
                        "credit_debit_indicator":"DBIT",
                        "status":"BOOK",
                        "booking_date":"2026-05-30",
                        "remittance_information":["COFFEE SHOP"],
                        "creditor":{"name":"Coffee Shop"}}
                     ],
                     "continuation_key":"next-1"}
                """.trimIndent()
            } else {
                HttpStatusCode.OK to """
                    {"transactions":[
                       {"transaction_amount":{"currency":"EUR","amount":"100"},
                        "credit_debit_indicator":"CRDT",
                        "status":"PDNG",
                        "booking_date":"2026-05-31",
                        "debtor":{"name":"Employer"}}
                     ]}
                """.trimIndent()
            }
        }
        val first = c.fetchTransactions("acc-1", LocalDate(2026, 5, 1)).getOrThrow()
        assertEquals("next-1", first.continuationKey)
        val tx = first.transactions.single()
        assertEquals("r1", tx.entryReference)
        assertEquals(1250L, tx.amountMinor)
        assertEquals(EbDirection.DEBIT, tx.direction)
        assertEquals("Coffee Shop", tx.counterparty)
        assertEquals("COFFEE SHOP", tx.description)
        assertTrue(tx.booked)

        val second = c.fetchTransactions("acc-1", LocalDate(2026, 5, 1), first.continuationKey).getOrThrow()
        assertNull(second.continuationKey)
        val pending = second.transactions.single()
        assertEquals(EbDirection.CREDIT, pending.direction)
        assertEquals("Employer", pending.counterparty)
        assertEquals(10_000L, pending.amountMinor)
        assertEquals(false, pending.booked)
    }

    @Test
    fun mapsRateLimitAndAuthErrors() = runTest {
        val limited = client { _, _ -> HttpStatusCode.TooManyRequests to """{"detail":"slow down"}""" }
        assertIs<EbError.RateLimited>(limited.listBanks("SK").exceptionOrNull())

        val unauthorized = client { _, _ -> HttpStatusCode.Unauthorized to """{"detail":"bad jwt"}""" }
        assertIs<EbError.Unauthorized>(unauthorized.listBanks("SK").exceptionOrNull())
    }

    @Test
    fun failsUnauthorizedWhenNoCredentialsStored() = runTest {
        val empty = EnableBankingCredentialsStore(InMemorySecureStore())
        val c = client(store = empty) { _, _ -> HttpStatusCode.OK to "{}" }
        assertIs<EbError.Unauthorized>(c.listBanks("SK").exceptionOrNull())
    }
}
