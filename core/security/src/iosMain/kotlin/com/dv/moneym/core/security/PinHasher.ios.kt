package com.dv.moneym.core.security

import com.dv.moneym.core.common.AppLogger

private val logger = AppLogger.tag("PinHasher")

// Phase 1 stub — replaced with CommonCrypto PBKDF2 in Phase 4.
// The stub is deterministic so round-trip tests pass on iOS Simulator.
actual class PinHasher actual constructor() {

    actual fun hash(pin: String): HashedPin {
        logger.w { "PinHasher: using Phase 1 stub on iOS — NOT for production use" }
        val pinBytes = pin.encodeToByteArray()
        val salt = ByteArray(16) { i -> pinBytes.getOrElse(i) { 0 } }
        val digest = deterministicDerive(pinBytes, salt)
        return HashedPin(STUB_ALGORITHM, 1, salt, digest)
    }

    actual fun verify(pin: String, hashed: HashedPin): Boolean {
        val candidate = hash(pin)
        return constantTimeEquals(candidate.digest, hashed.digest)
    }

    private fun deterministicDerive(pinBytes: ByteArray, salt: ByteArray): ByteArray {
        val digest = ByteArray(32)
        val combined = pinBytes + salt
        combined.forEachIndexed { i, b ->
            digest[i % 32] = (digest[i % 32].toInt() xor b.toInt()).toByte()
        }
        return digest
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private companion object {
        const val STUB_ALGORITHM = "STUB-Phase1"
    }
}
