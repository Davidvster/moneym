package com.dv.moneym.core.oauth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PkceTest {

    @Test
    fun generate_producesUrlSafeVerifierAndChallenge() = runTest {
        val pair = Pkce.generate()
        assertTrue(pair.verifier.length >= 43)
        assertTrue(pair.challenge.length in 43..86)
        assertTrue(pair.verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertTrue(pair.challenge.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun generate_producesDistinctValues() = runTest {
        val a = Pkce.generate()
        val b = Pkce.generate()
        assertNotEquals(a.verifier, b.verifier)
        assertNotEquals(a.challenge, b.challenge)
    }

    @Test
    fun randomState_isUnique() {
        val a = Pkce.randomState()
        val b = Pkce.randomState()
        assertNotEquals(a, b)
        assertTrue(a.length >= 16)
    }
}
