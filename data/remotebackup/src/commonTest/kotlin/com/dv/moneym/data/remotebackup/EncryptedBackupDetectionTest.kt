package com.dv.moneym.data.remotebackup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptedBackupDetectionTest {

    private val zipBytes = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)
    private val jsonBytes = "{\"version\":1}".encodeToByteArray()

    @Test
    fun appPropertyEncryptedTrueWins() {
        assertTrue(
            isEncryptedRemoteBackup(mapOf("encrypted" to "true"), "moneym-backup-1.zip", zipBytes),
        )
    }

    @Test
    fun appPropertyEncryptedFalseWins() {
        assertFalse(
            isEncryptedRemoteBackup(mapOf("encrypted" to "false"), "moneym-backup-1.bin", jsonBytes),
        )
    }

    @Test
    fun binSuffixIsEncryptedWhenNoAppProperty() {
        assertTrue(isEncryptedRemoteBackup(emptyMap(), "moneym-backup-1.bin", zipBytes))
    }

    @Test
    fun zipSuffixIsPlaintextWhenNoAppProperty() {
        assertFalse(isEncryptedRemoteBackup(emptyMap(), "moneym-backup-1.zip", jsonBytes))
    }

    @Test
    fun foreignJsonWithoutSuffixOrPropertyFallsBackToByteSniff() {
        // Legacy/unknown name: a JSON-looking blob trips the sniff, but a zip does not.
        assertTrue(isEncryptedRemoteBackup(emptyMap(), "unknown", jsonBytes))
        assertFalse(isEncryptedRemoteBackup(emptyMap(), "unknown", zipBytes))
    }
}
