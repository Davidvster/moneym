package com.dv.moneym.core.oauth

import com.dv.moneym.core.common.DispatcherProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFRelease
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureTokenStore(
    private val dispatchers: DispatcherProvider,
    private val json: Json = DEFAULT_JSON,
) : SecureTokenStore {

    override suspend fun read(): StoredTokens? = withContext(dispatchers.io) {
        val data = readBytes() ?: return@withContext null
        runCatching { json.decodeFromString(StoredTokens.serializer(), data.decodeToString()) }.getOrNull()
    }

    override suspend fun write(tokens: StoredTokens): Unit = withContext(dispatchers.io) {
        val raw = json.encodeToString(StoredTokens.serializer(), tokens).encodeToByteArray()
        writeBytes(raw)
    }

    override suspend fun clear(): Unit = withContext(dispatchers.io) {
        val q = baseQuery()
        try {
            SecItemDelete(q)
        } finally {
            CFRelease(q)
        }
    }

    private fun readBytes(): ByteArray? = memScoped {
        val keys = listOf<CFTypeRef?>(
            kSecClass,
            kSecAttrService,
            kSecAttrAccount,
            kSecMatchLimit,
            kSecReturnData,
        )
        val values = listOf<CFTypeRef?>(
            kSecClassGenericPassword,
            CFBridgingRetain(SERVICE),
            CFBridgingRetain(ACCOUNT),
            kSecMatchLimitOne,
            CFBridgingRetain(true),
        )
        val query = cfDictionary(keys, values) ?: return null
        try {
            val resultVar = alloc<CFTypeRefVar>()
            val status: OSStatus = SecItemCopyMatching(query, resultVar.ptr)
            if (status == errSecItemNotFound) return null
            if (status != errSecSuccess) return null
            val data = resultVar.value ?: return null
            val nsData = CFBridgingRelease(data) as? NSData
            val out = if (nsData == null || nsData.length.toInt() == 0) ByteArray(0)
            else ByteArray(nsData.length.toInt()).also { arr ->
                arr.usePinned { pinned ->
                    platform.posix.memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
                }
            }
            out
        } finally {
            CFRelease(query)
            CFRelease(values[1])
            CFRelease(values[2])
            CFRelease(values[4])
        }
    }

    private fun writeBytes(bytes: ByteArray) {
        val nsData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.convert())
        }
        val deleteQuery = baseQuery()
        try {
            SecItemDelete(deleteQuery)
        } finally {
            CFRelease(deleteQuery)
        }
        val keys = listOf<CFTypeRef?>(
            kSecClass,
            kSecAttrService,
            kSecAttrAccount,
            kSecAttrAccessible,
            kSecValueData,
        )
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(ACCOUNT)
        val dataRef = CFBridgingRetain(nsData)
        val values = listOf<CFTypeRef?>(
            kSecClassGenericPassword,
            service,
            account,
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            dataRef,
        )
        val attrs = cfDictionary(keys, values) ?: return
        try {
            SecItemAdd(attrs, null)
        } finally {
            CFRelease(attrs)
            CFRelease(service)
            CFRelease(account)
            CFRelease(dataRef)
        }
    }

    private fun baseQuery(): CFDictionaryRef? {
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(ACCOUNT)
        val keys = listOf<CFTypeRef?>(kSecClass, kSecAttrService, kSecAttrAccount)
        val values = listOf<CFTypeRef?>(kSecClassGenericPassword, service, account)
        val dict = cfDictionary(keys, values)
        // intentional leak of `service`/`account` retain — CFDictionary retains them; we'll
        // release the dictionary instead. The Foundation CFBridgingRetain bumps refcount by
        // one, and CFDictionary takes its own retain, so release these here.
        CFRelease(service)
        CFRelease(account)
        return dict
    }

    private fun cfDictionary(keys: List<CFTypeRef?>, values: List<CFTypeRef?>): CFDictionaryRef? =
        memScoped {
            require(keys.size == values.size)
            val keysArr = allocArrayOf(keys)
            val valuesArr = allocArrayOf(values)
            CFDictionaryCreate(
                kCFAllocatorDefault,
                keysArr.reinterpret(),
                valuesArr.reinterpret(),
                keys.size.convert(),
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
        }

    companion object {
        private const val SERVICE = "com.dv.moneym.oauth"
        private const val ACCOUNT = "google"
        private val DEFAULT_JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
