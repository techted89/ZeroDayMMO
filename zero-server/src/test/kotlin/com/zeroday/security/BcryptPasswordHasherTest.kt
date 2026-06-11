package com.zeroday.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BcryptPasswordHasherTest {
    private val hasher = BcryptPasswordHasher()

    @Test
    fun `hash produces a non-empty value that is not the plaintext`() {
        val plaintext = "correct horse battery staple"
        val hash = hasher.hash(plaintext)
        assertTrue(hash.isNotEmpty())
        assertNotEquals(plaintext, hash)
    }

    @Test
    fun `verify accepts the correct password`() {
        val plaintext = "letmein123"
        val hash = hasher.hash(plaintext)
        assertTrue(hasher.verify(plaintext, hash))
    }

    @Test
    fun `verify rejects the wrong password`() {
        val hash = hasher.hash("letmein123")
        assertFalse(hasher.verify("letmein124", hash))
        assertFalse(hasher.verify("", hash))
    }

    @Test
    fun `two hashes of the same plaintext differ (salted)`() {
        val a = hasher.hash("hunter2")
        val b = hasher.hash("hunter2")
        assertNotEquals(a, b, "BCrypt should produce different salts on each call")
        assertTrue(hasher.verify("hunter2", a))
        assertTrue(hasher.verify("hunter2", b))
    }
}
