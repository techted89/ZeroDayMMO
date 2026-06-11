package com.zeroday.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BanRecord(
    val playerId: String,
    val username: String,
    val bannedBy: String,
    val reason: String,
    val bannedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isActive: Boolean = true
)

@Serializable
data class AdminAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val action: String,
    val adminUsername: String,
    val targetPlayerId: String? = null,
    val targetUsername: String? = null,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String? = null
)

@Serializable
data class CheatAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val playerId: String,
    val username: String,
    val alertType: CheatAlertType,
    val description: String,
    val severity: CheatSeverity,
    val detectedAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    @Transient
    val metadata: Map<String, Any?> = emptyMap()
)

enum class CheatAlertType {
    SPEED_HACK, RESOURCE_TAMPER, XP_TAMPER, CREDIT_TAMPER,
    COMMAND_SPAM, RAPID_TRAVEL, IMPOSSIBLE_ACTION,
    PACKET_TAMPERING, SUSPICIOUS_LOGIN, LEVEL_JUMP,
    FACTION_EXPLOIT, ECONOMY_ABUSE
}

enum class CheatSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
data class AdminPlayerView(
    val id: String,
    val username: String,
    val level: Int,
    val experience: Long,
    val experienceToNext: Long,
    val cpu: Int,
    val maxCpu: Int,
    val ram: Int,
    val maxRam: Int,
    val bandwidth: Int,
    val maxBandwidth: Int,
    val credits: Long,
    val reputation: Int,
    val prestigeLevel: Int,
    val isOnline: Boolean,
    val isBanned: Boolean,
    val banReason: String? = null,
    val careerPath: String,
    val heatLevel: Int,
    val justicePoints: Int,
    val currentZoneId: String,
    val loginStreak: Int,
    val totalLogins: Int,
    val lastLoginIp: String,
    val lastLoginAt: Long,
    val factionId: String?,
    val partyId: String?,
    val unlockedCommandsCount: Int,
    val discoveredNodesCount: Int,
    val completedTasksCount: Int,
    val totalPlayTimeMs: Long = 0L,
    val skillPoints: Int,
    val unlockedSkillCount: Int,
    val timesArrested: Int
)

fun Player.toAdminView() = AdminPlayerView(
    id = id,
    username = username,
    level = level,
    experience = experience,
    experienceToNext = experienceToNext,
    cpu = cpu,
    maxCpu = maxCpu,
    ram = ram,
    maxRam = maxRam,
    bandwidth = bandwidth,
    maxBandwidth = maxBandwidth,
    credits = credits,
    reputation = reputation,
    prestigeLevel = prestigeLevel,
    isOnline = isOnline,
    isBanned = isBanned,
    banReason = banReason,
    careerPath = careerPath,
    heatLevel = heatLevel,
    justicePoints = justicePoints,
    currentZoneId = currentZoneId,
    loginStreak = loginStreak,
    totalLogins = totalLogins,
    lastLoginIp = lastLoginIp,
    lastLoginAt = lastLoginAt,
    factionId = factionId,
    partyId = partyId,
    unlockedCommandsCount = unlockedCommands.size,
    discoveredNodesCount = discoveredNodes.size,
    completedTasksCount = completedTasks.size,
    totalPlayTimeMs = 0L,
    skillPoints = skillPoints,
    unlockedSkillCount = unlockedSkills.size,
    timesArrested = timesArrested
)

@Serializable
data class ModifyPlayerRequest(
    val field: String,
    val value: String
)

@Serializable
data class BroadcastRequest(
    val title: String,
    val message: String,
    val messageType: String = "announcement",
    val priority: String = "normal"
)

@Serializable
data class ServerLogEntry(
    val id: String,
    val timestamp: Long,
    val level: String,
    val logger: String,
    val message: String
)
