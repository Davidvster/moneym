package com.dv.moneym.data.remotebackup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionPassphraseTest {

    @Test
    fun setAndGet_returnsCopy() {
        val session = SessionPassphrase()
        val original = "secret".toCharArray()
        session.set(original)
        val a = session.get()
        val b = session.get()
        assertNotNull(a); assertNotNull(b)
        assertContentEquals(a, b)
        a!![0] = 'X'
        assertContentEquals("secret".toCharArray(), session.get())
        assertTrue(session.isSet.value)
    }

    @Test
    fun clear_zeroizesAndUnsets() {
        val session = SessionPassphrase()
        session.set("secret".toCharArray())
        session.clear()
        assertNull(session.get())
        assertFalse(session.isSet.value)
    }
}
