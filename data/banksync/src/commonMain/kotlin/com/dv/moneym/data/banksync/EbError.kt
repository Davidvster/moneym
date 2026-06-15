package com.dv.moneym.data.banksync

sealed class EbError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidPrivateKey(message: String, cause: Throwable? = null) : EbError(message, cause)
    class Unauthorized(message: String) : EbError(message)
    class SessionExpired(message: String) : EbError(message)
    class RateLimited(message: String) : EbError(message)
    class Api(val status: Int, message: String) : EbError(message)
    class Network(cause: Throwable) : EbError(cause.message ?: "network error", cause)
}
