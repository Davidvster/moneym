package com.dv.moneym.core.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

// Secrets (PIN hash, backup sync passphrase) live in the iOS Keychain. It encrypts items and
// binds them to the device (ThisDeviceOnly = never migrated to a new device or restored from
// an iTunes/iCloud backup), unlike a plaintext NSUserDefaults .plist.
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureStore : SecureStore {

    private companion object {
        const val SERVICE = "com.dv.moneym.securestore"
    }

    override suspend fun put(key: String, value: ByteArray, requireBiometric: Boolean) {
        // Delete-then-add keeps the code single-path; SecItemUpdate would need a second dict.
        deleteItem(key)

        val data = value.toNSData()
        val cfData = CFBridgingRetain(data)
        val cfAccount = CFBridgingRetain(key)
        val cfService = CFBridgingRetain(SERVICE)
        try {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to cfService,
                kSecAttrAccount to cfAccount,
                kSecValueData to cfData,
                kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            )
            try {
                val status = SecItemAdd(query, null)
                check(status == errSecSuccess) { "Keychain write failed for $key: $status" }
            } finally {
                CFRelease(query)
            }
        } finally {
            CFRelease(cfData)
            CFRelease(cfAccount)
            CFRelease(cfService)
        }
    }

    override suspend fun get(key: String): ByteArray? {
        val cfAccount = CFBridgingRetain(key)
        val cfService = CFBridgingRetain(SERVICE)
        try {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to cfService,
                kSecAttrAccount to cfAccount,
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne,
            )
            return try {
                memScoped {
                    val result = alloc<CFTypeRefVar>()
                    val status = SecItemCopyMatching(query, result.ptr)
                    when (status) {
                        errSecSuccess -> (CFBridgingRelease(result.value) as? NSData)?.toByteArray()
                        errSecItemNotFound -> null
                        else -> error("Keychain read failed for $key: $status")
                    }
                }
            } finally {
                CFRelease(query)
            }
        } finally {
            CFRelease(cfAccount)
            CFRelease(cfService)
        }
    }

    override suspend fun remove(key: String) {
        deleteItem(key)
    }

    private fun deleteItem(key: String) {
        val cfAccount = CFBridgingRetain(key)
        val cfService = CFBridgingRetain(SERVICE)
        try {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to cfService,
                kSecAttrAccount to cfAccount,
            )
            try {
                SecItemDelete(query)
            } finally {
                CFRelease(query)
            }
        } finally {
            CFRelease(cfAccount)
            CFRelease(cfService)
        }
    }

    private fun cfDictionaryOf(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef {
        val dict = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            pairs.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        pairs.forEach { (k, v) -> CFDictionaryAddValue(dict, k, v) }
        return dict!!
    }

    private fun ByteArray.toNSData(): NSData =
        if (isEmpty()) {
            NSData()
        } else {
            usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = size.convert())
            }
        }

    private fun NSData.toByteArray(): ByteArray {
        val length = length.toInt()
        if (length == 0) return ByteArray(0)
        return ByteArray(length).also { out ->
            out.usePinned { pinned ->
                memcpy(pinned.addressOf(0), bytes, this.length)
            }
        }
    }
}
