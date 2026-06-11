package com.zeroday.service

import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FastHasher : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

class PlayerServiceTest {
    private val service = PlayerService(FastHasher())

    @Test
    fun registerCreatesPlayerAndStoresHashedPassword() {
        val r = runBlocking { service.register("alice", "secret123") }
        assertTrue(r.isSuccess)
        val p = r.getOrThrow()
        assertEquals("alice", p.username)
        assertNotNull(p.passwordHash)
    }

    @Test
    fun registerRejectsDuplicateUsernames() {
        runBlocking { service.register("bob", "secret123") }
        val second = runBlocking { service.register("bob", "different") }
        assertTrue(second.isFailure)
    }

    @Test
    fun loginSucceedsWithRightPassword() {
        runBlocking { service.register("carol", "secret123") }
        val ok = runBlocking { service.login("carol", "secret123") }
        assertTrue(ok.isSuccess)
        val bad = runBlocking { service.login("carol", "wrong") }
        assertTrue(bad.isFailure)
    }

    @Test
    fun loginUsesConstantMessageForUnknownAndWrong() {
        val unknown = runBlocking { service.login("nobody", "x") }
        assertTrue(unknown.isFailure)
        runBlocking { service.register("dave", "secret123") }
        val wrong = runBlocking { service.login("dave", "bad") }
        assertTrue(wrong.isFailure)
        assertEquals(unknown.exceptionOrNull()?.message, wrong.exceptionOrNull()?.message)
    }

    @Test
    fun getSnapshotReturnsNullForUnknownPlayers() {
        val result = runBlocking { service.getSnapshot("missing") }
        assertEquals(null, result)
    }
}
