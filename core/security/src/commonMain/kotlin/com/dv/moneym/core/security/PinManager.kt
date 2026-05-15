package com.dv.moneym.core.security

import com.dv.moneym.core.datastore.AppSettings
import kotlin.time.Clock

class PinManager(
    private val secureStore: SecureStore,
    private val pinHasher: PinHasher,
    private val settings: AppSettings,
) {
    suspend fun setPin(pin: String) {
        val hashed = pinHasher.hash(pin)
        secureStore.put(SecurityKeys.PIN_HASH, hashedPinToBytes(hashed))
        settings.putBoolean(SecurityPrefs.PIN_ENABLED, true)
        resetAttempts()
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = secureStore.get(SecurityKeys.PIN_HASH) ?: return false
        val hashed = hashedPinFromBytes(stored) ?: return false
        return pinHasher.verify(pin, hashed)
    }

    suspend fun clearPin() {
        secureStore.remove(SecurityKeys.PIN_HASH)
        settings.putBoolean(SecurityPrefs.PIN_ENABLED, false)
        settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, false)
        resetAttempts()
    }

    suspend fun isPinSet(): Boolean = secureStore.get(SecurityKeys.PIN_HASH) != null

    fun recordFailedAttempt() {
        val current = settings.getInt(SecurityPrefs.FAILED_PIN_ATTEMPTS)
        settings.putInt(SecurityPrefs.FAILED_PIN_ATTEMPTS, current + 1)
        settings.putLong(SecurityPrefs.LAST_FAILED_ATTEMPT_AT, Clock.System.now().toEpochMilliseconds())
    }

    fun resetAttempts() {
        settings.putInt(SecurityPrefs.FAILED_PIN_ATTEMPTS, 0)
        settings.putLong(SecurityPrefs.LAST_FAILED_ATTEMPT_AT, 0L)
    }

    fun failedAttempts(): Int = settings.getInt(SecurityPrefs.FAILED_PIN_ATTEMPTS)

    fun backoffRemainingMs(): Long {
        val attempts = failedAttempts()
        if (attempts < 3) return 0L
        val backoffMs = backoffForAttempts(attempts)
        val lastFailed = settings.getLong(SecurityPrefs.LAST_FAILED_ATTEMPT_AT)
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastFailed
        return maxOf(0L, backoffMs - elapsed)
    }

    private fun backoffForAttempts(attempts: Int): Long = when {
        attempts < 3 -> 0L
        attempts == 3 -> 5_000L
        attempts == 4 -> 30_000L
        else -> 300_000L
    }
}

// Simple manual binary serialization for HashedPin
internal fun hashedPinToBytes(pin: HashedPin): ByteArray {
    val algBytes = pin.algorithm.encodeToByteArray()
    val buf = ByteArray(1 + algBytes.size + 4 + 1 + pin.salt.size + 1 + pin.digest.size)
    var pos = 0
    buf[pos++] = algBytes.size.toByte()
    algBytes.copyInto(buf, pos); pos += algBytes.size
    buf[pos++] = (pin.iterations shr 24).toByte()
    buf[pos++] = (pin.iterations shr 16).toByte()
    buf[pos++] = (pin.iterations shr 8).toByte()
    buf[pos++] = pin.iterations.toByte()
    buf[pos++] = pin.salt.size.toByte()
    pin.salt.copyInto(buf, pos); pos += pin.salt.size
    buf[pos++] = pin.digest.size.toByte()
    pin.digest.copyInto(buf, pos)
    return buf
}

internal fun hashedPinFromBytes(bytes: ByteArray): HashedPin? = try {
    var pos = 0
    val algLen = bytes[pos++].toInt() and 0xFF
    val alg = bytes.copyOfRange(pos, pos + algLen).decodeToString(); pos += algLen
    val iter = ((bytes[pos].toInt() and 0xFF) shl 24) or
        ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
        ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
        (bytes[pos + 3].toInt() and 0xFF); pos += 4
    val saltLen = bytes[pos++].toInt() and 0xFF
    val salt = bytes.copyOfRange(pos, pos + saltLen); pos += saltLen
    val digestLen = bytes[pos++].toInt() and 0xFF
    val digest = bytes.copyOfRange(pos, pos + digestLen)
    HashedPin(alg, iter, salt, digest)
} catch (_: Exception) { null }
