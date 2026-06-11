package com.zeroday.service

import com.zeroday.model.Notification
import com.zeroday.model.NotificationType
import com.zeroday.model.Player
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Player inbox. Each player has a bounded notification list (newest at
 * the end) so the client can render an unread badge. When the cap is
 * reached the oldest read notification is dropped first; unread
 * notifications are never silently deleted.
 */
class NotificationService(val perPlayerCap: Int = 100) {
    private val mutex = Mutex()

    suspend fun push(
        player: Player,
        type: NotificationType,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ): Notification = mutex.withLock {
        val n = Notification(type = type, title = title, message = message, data = data)
        player.notifications.add(n)
        trim(player)
        n
    }

    /**
     * Push a notification, but if an unread one of the same
     * [coalesceKey] was pushed within the last [windowMs] milliseconds,
     * *replace* it with a single merged notification whose data
     * `count` is the running total. This is what keeps the inbox from
     * being spammed when 12 achievements unlock at once or 5 world
     * events fire in the same second.
     *
     * Returns the (new or merged) notification.
     */
    suspend fun pushCoalescing(
        player: Player,
        type: NotificationType,
        title: String,
        message: String,
        coalesceKey: String,
        windowMs: Long = 5_000L,
        data: Map<String, String> = emptyMap()
    ): Notification = mutex.withLock {
        val now = System.currentTimeMillis()
        val existing = player.notifications.lastOrNull {
            !it.read &&
                it.type == type &&
                it.createdAt >= now - windowMs &&
                it.data["coalesceKey"] == coalesceKey
        }
        if (existing != null) {
            val prevCount = existing.data["count"]?.toIntOrNull() ?: 1
            val mergedData = existing.data.toMutableMap()
            mergedData["count"] = (prevCount + 1).toString()
            val updated = existing.copy(
                title = title,
                message = message,
                data = mergedData,
                createdAt = now
            )
            val idx = player.notifications.indexOf(existing)
            player.notifications[idx] = updated
            updated
        } else {
            val n = Notification(
                type = type,
                title = title,
                message = message,
                data = data + ("coalesceKey" to coalesceKey) + ("count" to "1")
            )
            player.notifications.add(n)
            trim(player)
            n
        }
    }

    suspend fun markRead(player: Player, notificationId: String): Boolean = mutex.withLock {
        val n = player.notifications.firstOrNull { it.id == notificationId } ?: return@withLock false
        n.read = true
        true
    }

    suspend fun markAllRead(player: Player): Int = mutex.withLock {
        var n = 0
        for (notif in player.notifications) {
            if (!notif.read) {
                notif.read = true
                n++
            }
        }
        n
    }

    fun unreadCount(player: Player): Int = player.notifications.count { !it.read }

    fun list(player: Player, includeRead: Boolean = true, limit: Int = 50): List<Notification> {
        val filtered = if (includeRead) player.notifications else player.notifications.filter { !it.read }
        return if (filtered.size <= limit) filtered else filtered.takeLast(limit)
    }

    private fun trim(player: Player) {
        if (player.notifications.size <= perPlayerCap) return
        val overflow = player.notifications.size - perPlayerCap
        // Drop oldest *read* notifications first to avoid losing unread state.
        val read = player.notifications.filter { it.read }
        val toRemove = read.take(overflow.coerceAtMost(read.size)).toSet()
        if (toRemove.isNotEmpty()) player.notifications.removeAll(toRemove)
        // If still over cap, drop oldest unread (cap is a hard ceiling).
        while (player.notifications.size > perPlayerCap) {
            player.notifications.removeAt(0)
        }
    }
}
