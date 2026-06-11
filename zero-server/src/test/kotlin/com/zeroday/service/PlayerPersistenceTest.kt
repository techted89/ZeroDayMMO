package com.zeroday.service

import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FastHasherP : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

class PlayerPersistenceTest {
    private val tmpDir: File = Files.createTempDirectory("zeroday-persist-test").toFile()

    @Test
    fun saveAndRestoreRoundTrip() = runBlocking {
        val ps = PlayerService(FastHasherP())
        ps.register("alice", "pw1")
        ps.register("bob", "pw2")

        val persistence = PlayerPersistence(ps, tmpDir, intervalMs = 60_000L)
        val saved = persistence.saveNow()
        assertEquals(2, saved, "Should save 2 players")

        val ps2 = PlayerService(FastHasherP())
        val restored = PlayerPersistence(ps2, tmpDir, intervalMs = 60_000L).restore()
        assertEquals(2, restored)

        val a = ps2.getPlayerByUsername("alice")!!
        assertEquals(1, a.level)
        assertEquals(0L, a.lifetimeCreditsEarned)
        assertTrue(!a.isOnline, "Restored players must be marked offline")
        assertEquals(0, a.lastLevelNotified, "Transient fields must be reset")

        val b = ps2.getPlayerByUsername("bob")
        assertTrue(b != null)
    }

    @Test
    fun restoreFromMissingDirIsNoOp() = runBlocking {
        val missing = File(tmpDir, "does-not-exist")
        val ps = PlayerService(FastHasherP())
        val restored = PlayerPersistence(ps, missing, intervalMs = 60_000L).restore()
        assertEquals(0, restored)
    }

    @Test
    fun statusReportsSnapshotTime() = runBlocking {
        val ps = PlayerService(FastHasherP())
        ps.register("c", "pw")
        val persistence = PlayerPersistence(ps, tmpDir, intervalMs = 60_000L)
        persistence.saveNow()
        val s = persistence.status()
        assertTrue((s["lastSnapshotAt"] as Long) > 0L, "lastSnapshotAt should be set after a save")
        assertEquals(1L, s["lastSavedCount"])
    }
}
