package com.dv.moneym.core.security

import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
class IosSecureStore : SecureStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) {
        val data = value.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), value.size.convert())
        }
        defaults.setObject(data, "moneym_secure_$key")
        defaults.synchronize()
    }

    override suspend fun get(key: String): ByteArray? {
        val data = defaults.dataForKey("moneym_secure_$key") ?: return null
        val length = data.length.toInt()
        if (length == 0) return ByteArray(0)
        return ByteArray(length).also { result ->
            result.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
    }

    override suspend fun remove(key: String) {
        defaults.removeObjectForKey("moneym_secure_$key")
        defaults.synchronize()
    }
}
