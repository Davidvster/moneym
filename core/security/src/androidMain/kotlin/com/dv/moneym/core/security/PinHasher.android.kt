package com.dv.moneym.core.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

actual class PinHasher actual constructor() {

    actual fun hash(pin: String): HashedPin {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = pbkdf2(pin, salt, ITERATIONS)
        return HashedPin(ALGORITHM, ITERATIONS, salt, digest)
    }

    actual fun verify(pin: String, hashed: HashedPin): Boolean {
        val candidate = pbkdf2(pin, hashed.salt, hashed.iterations)
        return constantTimeEquals(candidate, hashed.digest)
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, DIGEST_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
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
