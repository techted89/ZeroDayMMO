package com.zeroday.service

import com.zeroday.model.BanRecord
import com.zeroday.security.AuditLog
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class BanService(
    private val playerService: PlayerService
) {
    private val log = LoggerFactory.getLogger(BanService::class.java)
    private val bans = ConcurrentHashMap<String, BanRecord>()

    init {
        log.info("BanService initialized")
    }

    suspend fun banPlayer(
        playerId: String,
        bannedBy: String,
        reason: String,
        durationHours: Long? = null
    ): Result<BanRecord> {
        val player = playerService.getPlayer(playerId)
            ?: return Result.failure(Exception("Player not found"))

        val expiresAt = durationHours?.let { System.currentTimeMillis() + it * 3600_000L }
        val record = BanRecord(
            playerId = playerId,
            username = player.username,
            bannedBy = bannedBy,
            reason = reason,
            expiresAt = expiresAt,
            isActive = true
        )
        bans[playerId] = record

        playerService.withPlayerAction(playerId) { p ->
            p.isBanned = true
            p.banReason = reason
            p.isOnline = false
        }

        AuditLog.event("player_banned", playerId = playerId, extra = mapOf(
            "bannedBy" to bannedBy,
            "reason" to reason,
            "username" to player.username,
            "durationHours" to (durationHours ?: "permanent")
        ))

        log.info("Player {} ({}) banned by {}: {}", player.username, playerId, bannedBy, reason)
        return Result.success(record)
    }

    suspend fun unbanPlayer(playerId: String, unbannedBy: String): Result<String> {
        val record = bans[playerId] ?: return Result.failure(Exception("Player is not banned"))
        bans.remove(playerId)

        playerService.withPlayerAction(playerId) { p ->
            p.isBanned = false
            p.banReason = null
        }

        val player = playerService.getPlayer(playerId)
        AuditLog.event("player_unbanned", playerId = playerId, extra = mapOf(
            "unbannedBy" to unbannedBy,
            "username" to (player?.username ?: "unknown")
        ))

        log.info("Player {} ({}) unbanned by {}", player?.username ?: playerId, playerId, unbannedBy)
        return Result.success("Player unbanned successfully")
    }

    fun isBanned(playerId: String): Boolean {
        val record = bans[playerId] ?: return false
        if (!record.isActive) return false
        if (record.expiresAt != null && System.currentTimeMillis() > record.expiresAt) {
            bans.remove(playerId)
            return false
        }
        return true
    }

    fun getBanRecord(playerId: String): BanRecord? = bans[playerId]

    fun getAllBans(): List<BanRecord> = bans.values
        .filter { it.isActive && (it.expiresAt == null || System.currentTimeMillis() < it.expiresAt) }
        .toList()

    fun clearExpiredBans(): Int {
        val now = System.currentTimeMillis()
        val expired = bans.values.filter { it.expiresAt != null && now > it.expiresAt }
        expired.forEach { bans.remove(it.playerId) }
        return expired.size
    }
}
