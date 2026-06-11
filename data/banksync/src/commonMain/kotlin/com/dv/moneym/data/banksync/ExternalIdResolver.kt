package com.dv.moneym.data.banksync

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import com.dv.moneym.data.banksync.internal.platformCryptographyProvider
import kotlinx.datetime.LocalDate

class ExternalIdResolver(
    provider: CryptographyProvider = platformCryptographyProvider(),
) {
    private val hasher = provider.get(SHA256).hasher()

    suspend fun resolve(
        accountUid: String,
        entryReference: String?,
        bookingDate: LocalDate,
        amountMinor: Long,
        currency: String,
        description: String?,
    ): String {
        if (!entryReference.isNullOrBlank()) return "eb:$accountUid:$entryReference"
        val seed = "$accountUid|$bookingDate|$amountMinor|$currency|${description.orEmpty()}"
        val digest = hasher.hash(seed.encodeToByteArray())
        return "ebh:" + digest.toHexString()
    }

    fun disambiguate(ids: List<String>): List<String> {
        val seen = mutableMapOf<String, Int>()
        return ids.map { id ->
            val count = seen.getOrElse(id) { 0 } + 1
            seen[id] = count
            if (count == 1) id else "$id-$count"
        }
    }
}

private fun ByteArray.toHexString(): String =
    joinToString("") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
