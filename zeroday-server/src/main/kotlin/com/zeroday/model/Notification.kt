package com.zeroday.model

import kotlinx.serialization.Serializable

/**
 * A persistent in-game message for the player. Notifications are appended
 * to the player's inbox (newest last) and capped to a fixed size so the
 * payload stays bounded. The client is expected to display the unread
 * ones in a notification center.
 */
@Serializable
data class Notification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    var read: Boolean = false,
    /** Optional structured payload so the client can deep-link. */
    val data: Map<String, String> = emptyMap()
)

enum class NotificationType {
    ACHIEVEMENT_UNLOCKED,
    CHALLENGE_COMPLETED,
    CHALLENGE_EXPIRED,
    STORYLINE_AVAILABLE,
    FACTION_INVITED,
    LEVEL_UP,
    WORLD_EVENT_STARTED,
    WORLD_EVENT_JOINABLE,
    SYSTEM_MESSAGE,
    REWARD_GRANTED,
    DAILY_LOGIN
}
