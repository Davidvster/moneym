package com.dv.moneym.core.security

import kotlinx.serialization.json.Json

object BackupEnvelopeJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(envelope: EncryptedBackup): String = json.encodeToString(EncryptedBackup.serializer(), envelope)
    fun decode(text: String): EncryptedBackup = json.decodeFromString(EncryptedBackup.serializer(), text)
    fun encodeBytes(envelope: EncryptedBackup): ByteArray = encode(envelope).encodeToByteArray()
    fun decodeBytes(bytes: ByteArray): EncryptedBackup = decode(bytes.decodeToString())
}
