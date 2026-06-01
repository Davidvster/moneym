package com.dv.moneym.data.sync

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SyncSnapshotCodecTest {

    private fun codec() = SyncSnapshotCodec(
        crypto = FakeBackupCrypto(),
        appVersion = "1.0",
        nowMs = { 1000L },
    )

    private fun sampleSnapshot() = SyncSnapshot(
        formatVersion = 1,
        generatedAtMs = 42L,
        originDeviceId = "dev-1",
        accounts = listOf(
            SyncAccount(
                syncId = "a1",
                name = "Main",
                type = "cash",
                currency = "EUR",
                isDefault = true,
                archived = false,
                colorHex = "#FF0000",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        ),
        transactions = listOf(
            SyncTransaction(
                syncId = "t1",
                type = "expense",
                amountMinor = 500L,
                currency = "EUR",
                occurredOn = "2026-01-01",
                categorySyncId = "c1",
                accountSyncId = "a1",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        ),
    )

    @Test
    fun plaintextRoundTrip() = runTest {
        val codec = codec()
        val snapshot = sampleSnapshot()
        val sealed = codec.seal(snapshot, passphrase = null)
        val opened = codec.open(sealed, passphrase = null)
        assertEquals(snapshot, opened)
    }

    @Test
    fun encryptedRoundTrip() = runTest {
        val codec = codec()
        val snapshot = sampleSnapshot()
        val plaintext = codec.seal(snapshot, passphrase = null)
        val sealed = codec.seal(snapshot, passphrase = "pw".toCharArray())
        assertNotEquals(plaintext.toList(), sealed.toList())
        val opened = codec.open(sealed, passphrase = "pw".toCharArray())
        assertEquals(snapshot, opened)
    }

    @Test
    fun unknownFieldsTolerated() = runTest {
        val codec = codec()
        val withExtra = """{"formatVersion":1,"generatedAtMs":42,"originDeviceId":"dev-1","futureField":true}"""
        val opened = codec.open(withExtra.encodeToByteArray(), passphrase = null)
        assertEquals("dev-1", opened.originDeviceId)
        assertEquals(42L, opened.generatedAtMs)
    }

    @Test
    fun formatVersionPreserved() = runTest {
        val codec = codec()
        val snapshot = sampleSnapshot().copy(formatVersion = 7)
        val opened = codec.open(codec.seal(snapshot, null), null)
        assertEquals(7, opened.formatVersion)
    }

    @Test
    fun plaintextEncodesAsJson() = runTest {
        val codec = codec()
        val sealed = codec.seal(sampleSnapshot(), passphrase = null)
        assertTrue(sealed.decodeToString().startsWith("{"))
    }
}
