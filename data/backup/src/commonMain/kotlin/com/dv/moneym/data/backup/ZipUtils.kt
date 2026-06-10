package com.dv.moneym.data.backup

internal object Crc32 {
    private val table = IntArray(256).also { t ->
        for (n in 0..255) {
            var c = n
            repeat(8) { c = if (c and 1 != 0) 0xEDB88320.toInt() xor (c ushr 1) else c ushr 1 }
            t[n] = c
        }
    }

    fun calculate(data: ByteArray): Int {
        var c = -1
        for (b in data) c = table[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
        return c xor -1
    }
}

private class ByteBuf {
    private var data = ByteArray(4096)
    private var pos = 0

    fun write(b: Int) {
        grow(1)
        data[pos++] = b.toByte()
    }

    fun write(bytes: ByteArray) {
        grow(bytes.size)
        bytes.copyInto(data, pos)
        pos += bytes.size
    }

    fun writeLE16(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF) }

    fun writeLE32(v: Int) {
        write(v and 0xFF); write((v shr 8) and 0xFF)
        write((v shr 16) and 0xFF); write((v shr 24) and 0xFF)
    }

    fun size() = pos
    fun toByteArray() = data.copyOf(pos)

    private fun grow(n: Int) {
        if (pos + n > data.size) data = data.copyOf(maxOf(data.size * 2, pos + n))
    }
}

fun createZip(files: List<Pair<String, ByteArray>>): ByteArray {
    val buf = ByteBuf()
    val offsets = IntArray(files.size)

    files.forEachIndexed { i, (name, data) ->
        offsets[i] = buf.size()
        val nameBytes = name.encodeToByteArray()
        val crc = Crc32.calculate(data)

        buf.writeLE32(0x04034b50)
        buf.writeLE16(20)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE32(crc)
        buf.writeLE32(data.size)
        buf.writeLE32(data.size)
        buf.writeLE16(nameBytes.size)
        buf.writeLE16(0)
        buf.write(nameBytes)
        buf.write(data)
    }

    val centralStart = buf.size()

    files.forEachIndexed { i, (name, data) ->
        val nameBytes = name.encodeToByteArray()
        val crc = Crc32.calculate(data)

        buf.writeLE32(0x02014b50)
        buf.writeLE16(20)
        buf.writeLE16(20)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE32(crc)
        buf.writeLE32(data.size)
        buf.writeLE32(data.size)
        buf.writeLE16(nameBytes.size)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE16(0)
        buf.writeLE32(0)
        buf.writeLE32(offsets[i])
        buf.write(nameBytes)
    }

    val centralSize = buf.size() - centralStart

    buf.writeLE32(0x06054b50)
    buf.writeLE16(0)
    buf.writeLE16(0)
    buf.writeLE16(files.size)
    buf.writeLE16(files.size)
    buf.writeLE32(centralSize)
    buf.writeLE32(centralStart)
    buf.writeLE16(0)

    return buf.toByteArray()
}

fun extractZip(zip: ByteArray): Map<String, ByteArray> {
    val result = mutableMapOf<String, ByteArray>()
    var pos = 0

    while (pos <= zip.size - 30) {
        if (zip.le32(pos) != 0x04034b50) { pos++; continue }

        val compression = zip.le16(pos + 8)
        val compressedSize = zip.le32(pos + 18)
        val nameLen = zip.le16(pos + 26)
        val extraLen = zip.le16(pos + 28)
        val dataStart = pos + 30 + nameLen + extraLen

        if (dataStart + compressedSize > zip.size) break

        val name = zip.decodeToString(pos + 30, pos + 30 + nameLen)
        require(isSafeEntryName(name)) { "Unsafe zip entry name: $name" }
        if (compression == 0) {
            result[name] = zip.copyOfRange(dataStart, dataStart + compressedSize)
        }

        pos = dataStart + compressedSize
    }

    return result
}

// Backup archives are always a flat set of database filenames — no directories. Any path
// separator, traversal segment, or control/whitespace char (code <= 0x20, which includes the
// null byte) means a crafted (zip-slip) archive trying to escape the destination directory,
// so reject the whole archive rather than the entry.
internal fun isSafeEntryName(name: String): Boolean {
    if (name.isEmpty() || name == "." || name == "..") return false
    return name.none { it == '/' || it == '\\' || it.code <= 0x20 }
}

private fun ByteArray.le16(o: Int): Int = (this[o].toInt() and 0xFF) or ((this[o + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.le32(o: Int): Int =
    (this[o].toInt() and 0xFF) or
    ((this[o + 1].toInt() and 0xFF) shl 8) or
    ((this[o + 2].toInt() and 0xFF) shl 16) or
    ((this[o + 3].toInt() and 0xFF) shl 24)
