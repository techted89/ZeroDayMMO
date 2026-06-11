package com.zeroday.service

import com.zeroday.model.NotificationType
import com.zeroday.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FastHasherN : PasswordHasher {
    override fun hash(plaintext: String): String = "hashed:$plaintext"
    override fun verify(plaintext: String, storedHash: String): Boolean = storedHash == "hashed:$plaintext"
}

class NotificationServiceTest {
    private val svc = NotificationService(perPlayerCap = 5)
    private val playerService = PlayerService(FastHasherN())

    @Test
    fun pushAndList() = runBlocking {
        val p = playerService.register("alice", "pw").getOrThrow()
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "Hello", "world")
        svc.push(p, NotificationType.REWARD_GRANTED, "Credits", "+100")
        assertEquals(2, svc.list(p).size)
        assertEquals(2, svc.unreadCount(p))
    }

    @Test
    fun markReadDropsUnreadCount() = runBlocking {
        val p = playerService.register("bob", "pw").getOrThrow()
        val n = svc.push(p, NotificationType.SYSTEM_MESSAGE, "X", "y")
        assertEquals(1, svc.unreadCount(p))
        svc.markRead(p, n.id)
        assertEquals(0, svc.unreadCount(p))
    }

    @Test
    fun markAllReadClearsAllUnread() = runBlocking {
        val p = playerService.register("c", "pw").getOrThrow()
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "1", "a")
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "2", "b")
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "3", "c")
        assertEquals(3, svc.unreadCount(p))
        svc.markAllRead(p)
        assertEquals(0, svc.unreadCount(p))
    }

    @Test
    fun dropsOldestReadWhenAtCapacity() = runBlocking {
        val p = playerService.register("d", "pw").getOrThrow()
        val first = svc.push(p, NotificationType.SYSTEM_MESSAGE, "1", "a")
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "2", "b")
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "3", "c")
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "4", "d")
        svc.push(p, NotificationType.SYSTEM_MESSAGE, "5", "e")
        svc.markRead(p, first.id)
        // One more: should drop read "1" and append "6"
        val sixth = svc.push(p, NotificationType.SYSTEM_MESSAGE, "6", "f")
        val ids = svc.list(p).map { it.id }
        assertEquals(5, ids.size)
        assertTrue(!ids.contains(first.id), "Oldest read should be evicted")
        assertTrue(ids.contains(sixth.id))
    }
}
