package com.dv.moneym.core.security

data class HashedPin(
    val algorithm: String,
    val iterations: Int,
    val salt: ByteArray,
    val digest: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HashedPin) return false
        return algorithm == other.algorithm &&
            iterations == other.iterations &&
            salt.contentEquals(other.salt) &&
            digest.contentEquals(other.digest)
    }
    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + iterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + digest.contentHashCode()
        return result
    }
}

expect class PinHasher() {
    fun hash(pin: String): HashedPin
    fun verify(pin: String, hashed: HashedPin): Boolean
}
