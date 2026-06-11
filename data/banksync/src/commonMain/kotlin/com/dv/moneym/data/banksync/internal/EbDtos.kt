package com.dv.moneym.data.banksync.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EbApplicationDto(
    val name: String? = null,
)

@Serializable
internal data class EbAspspDto(
    val name: String,
    val country: String,
    val logo: String? = null,
)

@Serializable
internal data class EbAspspsResponseDto(
    val aspsps: List<EbAspspDto> = emptyList(),
)

@Serializable
internal data class EbAuthRequestDto(
    val access: EbAccessDto,
    val aspsp: EbAspspRefDto,
    val state: String,
    @SerialName("redirect_url") val redirectUrl: String,
)

@Serializable
internal data class EbAccessDto(
    @SerialName("valid_until") val validUntil: String? = null,
)

@Serializable
internal data class EbAspspRefDto(
    val name: String,
    val country: String,
)

@Serializable
internal data class EbAuthResponseDto(
    val url: String,
)

@Serializable
internal data class EbSessionRequestDto(
    val code: String,
)

@Serializable
internal data class EbSessionResponseDto(
    @SerialName("session_id") val sessionId: String,
    val accounts: List<EbAccountDto> = emptyList(),
    val access: EbAccessDto? = null,
)

@Serializable
internal data class EbAccountDto(
    val uid: String? = null,
    @SerialName("account_id") val accountId: EbAccountIdDto? = null,
    val name: String? = null,
    val currency: String? = null,
)

@Serializable
internal data class EbAccountIdDto(
    val iban: String? = null,
)

@Serializable
internal data class EbSessionStatusResponseDto(
    val status: String? = null,
    val access: EbAccessDto? = null,
)

@Serializable
internal data class EbTransactionsResponseDto(
    val transactions: List<EbTransactionDto> = emptyList(),
    @SerialName("continuation_key") val continuationKey: String? = null,
)

@Serializable
internal data class EbTransactionDto(
    @SerialName("entry_reference") val entryReference: String? = null,
    @SerialName("transaction_amount") val transactionAmount: EbAmountDto,
    @SerialName("credit_debit_indicator") val creditDebitIndicator: String,
    val status: String? = null,
    @SerialName("booking_date") val bookingDate: String? = null,
    @SerialName("value_date") val valueDate: String? = null,
    @SerialName("remittance_information") val remittanceInformation: List<String> = emptyList(),
    val creditor: EbPartyDto? = null,
    val debtor: EbPartyDto? = null,
)

@Serializable
internal data class EbAmountDto(
    val currency: String,
    val amount: String,
)

@Serializable
internal data class EbPartyDto(
    val name: String? = null,
)
