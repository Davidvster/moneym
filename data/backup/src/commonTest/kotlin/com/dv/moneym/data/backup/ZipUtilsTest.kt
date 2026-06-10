package com.dv.moneym.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZipUtilsTest {

    @Test
    fun roundTripPreservesEntries() {
        val files = listOf(
            "moneym_accounts.db" to byteArrayOf(1, 2, 3),
            "moneym_categories.db" to byteArrayOf(4, 5),
        )
        val extracted = extractZip(createZip(files))

        assertEquals(2, extracted.size)
        assertTrue(extracted["moneym_accounts.db"]!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(extracted["moneym_categories.db"]!!.contentEquals(byteArrayOf(4, 5)))
    }

    @Test
    fun rejectsParentTraversalEntry() {
        val malicious = createZip(listOf("../../evil.db" to byteArrayOf(0)))
        assertFailsWith<IllegalArgumentException> { extractZip(malicious) }
    }

    @Test
    fun rejectsAbsolutePathEntry() {
        val malicious = createZip(listOf("/data/data/com.dv.moneym/evil" to byteArrayOf(0)))
        assertFailsWith<IllegalArgumentException> { extractZip(malicious) }
    }

    @Test
    fun rejectsBackslashEntry() {
        val malicious = createZip(listOf("..\\evil" to byteArrayOf(0)))
        assertFailsWith<IllegalArgumentException> { extractZip(malicious) }
    }

    @Test
    fun rejectsControlCharEntry() {
        val malicious = createZip(listOf("evil\tname.db" to byteArrayOf(0)))
        assertFailsWith<IllegalArgumentException> { extractZip(malicious) }
    }

    @Test
    fun safeNameValidation() {
        assertTrue(isSafeEntryName("moneym_accounts.db"))
        assertTrue(isSafeEntryName("moneym_transactions.db-wal"))
        assertFalse(isSafeEntryName(""))
        assertFalse(isSafeEntryName("."))
        assertFalse(isSafeEntryName(".."))
        assertFalse(isSafeEntryName("a/b"))
        assertFalse(isSafeEntryName("a\\b"))
        assertFalse(isSafeEntryName("a\tb"))
    }
}
