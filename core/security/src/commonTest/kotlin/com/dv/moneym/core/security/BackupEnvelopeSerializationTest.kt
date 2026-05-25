package com.dv.moneym.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupEnvelopeSerializationTest {

    @Test
    fun roundTrip_preservesAllFields() {
        val original = EncryptedBackup(
            schema = 7,
            createdAt = 1_700_000_000_000L,
            appVersion = "1.2.3",
            kdf = KdfParams(iter = 600_000, saltB64 = "AAECAwQFBgcICQoLDA0ODw=="),
            cipher = CipherParams(
                ivB64 = "AAECAwQFBgcICQoL",
                ctB64 = "ZGVhZGJlZWY=",
                tagBits = 128,
            ),
        )
        val text = BackupEnvelopeJson.encode(original)
        val decoded = BackupEnvelopeJson.decode(text)
        assertEquals(original, decoded)
    }

    @Test
    fun rejects_unknownNewerVersion() {
        val json = """{"version":99,"schema":1,"createdAt":0,"appVersion":"x",""" +
            """"kdf":{"name":"PBKDF2-HMAC-SHA256","iter":1,"saltB64":""},""" +
            """"cipher":{"name":"AES-256-GCM","ivB64":"","ctB64":"","tagBits":128}}"""
        val decoded = BackupEnvelopeJson.decode(json)
        assertEquals(99, decoded.version)
        assertTrue(decoded.version > EncryptedBackup.ENVELOPE_VERSION)
    }
}
