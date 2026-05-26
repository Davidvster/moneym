package com.dv.moneym.core.oauth

import com.dv.moneym.core.common.DispatcherProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
class IosSecureTokenStore(
    private val dispatchers: DispatcherProvider,
    private val json: Json = DEFAULT_JSON,
) : SecureTokenStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(): StoredTokens? = withContext(dispatchers.io) {
        val data = defaults.dataForKey(KEY) ?: return@withContext null
        val length = data.length.toInt()
        if (length == 0) return@withContext null
        val bytes = ByteArray(length).also { result ->
            result.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
        runCatching { json.decodeFromString(StoredTokens.serializer(), bytes.decodeToString()) }.getOrNull()
    }

    override suspend fun write(tokens: StoredTokens): Unit = withContext(dispatchers.io) {
        val raw = json.encodeToString(StoredTokens.serializer(), tokens).encodeToByteArray()
        val data = raw.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), raw.size.convert())
        }
        defaults.setObject(data, KEY)
        defaults.synchronize()
    }

    override suspend fun clear(): Unit = withContext(dispatchers.io) {
        defaults.removeObjectForKey(KEY)
        defaults.synchronize()
    }

    companion object {
        private const val KEY = "moneym_oauth_google"
        private val DEFAULT_JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
