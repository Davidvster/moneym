package com.dv.moneym.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCValues
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual class PinHasher actual constructor() {

    actual fun hash(pin: String): HashedPin {
        val salt = ByteArray(SALT_BYTES)
        memScoped {
            SecRandomCopyBytes(kSecRandomDefault, SALT_BYTES.toULong(), salt.asUByteArray().toCValues().ptr)
        }
        val digest = pbkdf2(pin, salt, ITERATIONS)
        return HashedPin(ALGORITHM, ITERATIONS, salt, digest)
    }

    actual fun verify(pin: String, hashed: HashedPin): Boolean {
        val candidate = pbkdf2(pin, hashed.salt, hashed.iterations)
        return constantTimeEquals(candidate, hashed.digest)
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val pinBytes = pin.encodeToByteArray()
        val digestBytes = DIGEST_BITS / 8
        return memScoped {
            val output = allocArray<UByteVar>(digestBytes)
            val result = CCKeyDerivationPBKDF(
                algorithm = kCCPBKDF2,
                password = pin,
                passwordLen = pinBytes.size.toULong(),
                salt = salt.asUByteArray().toCValues().ptr,
                saltLen = salt.size.toULong(),
                prf = kCCPRFHmacAlgSHA256,
                rounds = iterations.toUInt(),
                derivedKey = output,
                derivedKeyLen = digestBytes.toULong(),
            )
            check(result == kCCSuccess) { "CCKeyDerivationPBKDF failed: $result" }
            output.readBytes(digestBytes)
        }
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 100_000
        const val SALT_BYTES = 16
        const val DIGEST_BITS = 256
    }
}
