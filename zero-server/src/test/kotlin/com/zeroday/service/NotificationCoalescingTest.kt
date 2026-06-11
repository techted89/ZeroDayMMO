package com.zeroday.service

import com.zeroday.model.NotificationType
import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FastHasherNC : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

class NotificationCoalescingTest {
    private val svc = NotificationService(perPlayerCap = 100)
    private val ps = PlayerService(FastHasherNC())

    @Test
    fun rapidPushesAreCoalesced() = runBlocking {
        val p = ps.register("alice", "pw").getOrThrow()
        // First push creates a notification
        svc.pushCoalescing(p, NotificationType.ACHIEVEMENT_UNLOCKED, "1 unlock", "x", coalesceKey = "k")
        // Second push within the window should replace, not append
        svc.pushCoalescing(p, NotificationType.ACHIEVEMENT_UNLOCKED, "2 unlocks", "y", coalesceKey = "k")
        val all = svc.list(p)
        assertEquals(1, all.size, "Should have 1 merged notification, not 2")
        assertEquals("2 unlocks", all[0].title)
        assertEquals("2", all[0].data["count"], "Count should be 2")
    }

    @Test
    fun differentCoalesceKeysProduceSeparateNotifications() = runBlocking {
        val p = ps.register("bob", "pw").getOrThrow()
        svc.pushCoalescing(p, NotificationType.ACHIEVEMENT_UNLOCKED, "A", "a", coalesceKey = "k1")
        svc.pushCoalescing(p, NotificationType.ACHIEVEMENT_UNLOCKED, "B", "b", coalesceKey = "k2")
        assertEquals(2, svc.list(p).size)
    }

    @Test
    fun differentTypesDoNotCoalesce() = runBlocking {
        val p = ps.register("c", "pw").getOrThrow()
        svc.pushCoalescing(p, NotificationType.ACHIEVEMENT_UNLOCKED, "a", "m", coalesceKey = "k")
        svc.pushCoalescing(p, NotificationType.CHALLENGE_COMPLETED, "c", "m", coalesceKey = "k")
        assertEquals(2, svc.list(p).size, "Different types should not coalesce even with same key")
    }
}
