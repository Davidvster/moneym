package com.dv.moneym.data.llmmodels

class InMemoryModelFileStore : ModelFileStore {

    val finals = mutableMapOf<String, ByteArray>()
    val parts = mutableMapOf<String, ByteArray>()

    override fun finalPath(fileName: String): String = "/mem/models/$fileName"

    override suspend fun finalExists(fileName: String): Boolean = finals.containsKey(fileName)

    override suspend fun finalSize(fileName: String): Long = finals[fileName]?.size?.toLong() ?: 0L

    override suspend fun partSize(fileName: String): Long = parts[fileName]?.size?.toLong() ?: 0L

    override suspend fun appendToPart(fileName: String, bytes: ByteArray, offset: Int, length: Int) {
        val existing = parts[fileName] ?: ByteArray(0)
        parts[fileName] = existing + bytes.copyOfRange(offset, offset + length)
    }

    override suspend fun resetPart(fileName: String) {
        parts.remove(fileName)
    }

    override suspend fun sha256OfPart(fileName: String): String =
        sha256Hex(parts[fileName] ?: ByteArray(0))

    override suspend fun promotePart(fileName: String) {
        parts.remove(fileName)?.let { finals[fileName] = it }
    }

    override suspend fun deletePart(fileName: String) {
        parts.remove(fileName)
    }

    override suspend fun deleteFinal(fileName: String) {
        finals.remove(fileName)
    }
}
