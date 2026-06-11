package com.zeroday.service

import com.zeroday.config.AdminConfig
import com.zeroday.handler.ConnectionEntry
import com.zeroday.handler.ConnectionRegistry
import com.zeroday.model.*
import com.zeroday.security.AuditLog
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

class AdminService(
    private val playerService: PlayerService,
    private val banService: BanService,
    private val cheatDetectionService: CheatDetectionService,
    private val connectionRegistry: ConnectionRegistry
) {
    private val log = LoggerFactory.getLogger(AdminService::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val adminSessions = ConcurrentHashMap<String, AdminSession>()
    private val actionLog = ConcurrentLinkedDeque<AdminAction>()
    private val maxActionLogEntries = 200
    private val secureRandom = SecureRandom()

    private val serverLogBuffer = ConcurrentLinkedDeque<ServerLog>()
    private val maxServerLogEntries = 1000

    data class AdminSession(
        val token: String,
        val username: String,
        val createdAt: Long = System.currentTimeMillis(),
        var lastActivity: Long = System.currentTimeMillis()
    )

    data class ServerLog(
        val id: String = java.util.UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val level: String,
        val logger: String,
        val message: String
    )

    init {
        log.info("Admin service initialized")
    }

    fun authenticate(username: String, password: String): Result<String> {
        if (username != AdminConfig.ADMIN_USERNAME) {
            AuditLog.event("admin_auth_failed", extra = mapOf("username" to username, "reason" to "invalid_username"))
            return Result.failure(Exception("Invalid admin credentials"))
        }
        if (password != AdminConfig.ADMIN_PASSWORD) {
            AuditLog.event("admin_auth_failed", extra = mapOf("username" to username, "reason" to "invalid_password"))
            return Result.failure(Exception("Invalid admin credentials"))
        }

        val token = generateToken()
        adminSessions[token] = AdminSession(token = token, username = username)
        AuditLog.event("admin_login", extra = mapOf("username" to username))
        log.info("Admin {} logged in", username)
        return Result.success(token)
    }

    fun validateToken(token: String): AdminSession? {
        val session = adminSessions[token] ?: return null
        session.lastActivity = System.currentTimeMillis()
        return session
    }

    fun invalidateToken(token: String) {
        adminSessions.remove(token)
    }

    fun getServerOverview(): Map<String, Any?> {
        return mapOf(
            "onlinePlayers" to playerService.getOnlinePlayers().size,
            "totalPlayers" to playerService.getAllPlayers().size,
            "activeConnections" to connectionRegistry.totalConnections(),
            "activeBans" to banService.getAllBans().size,
            "cheatAlerts" to cheatDetectionService.getAlertCount(),
            "cheatAlertsBySeverity" to cheatDetectionService.getAlertCountBySeverity(),
            "serverUptimeMs" to System.currentTimeMillis(),
            "adminSessions" to adminSessions.size,
            "logEntriesBuffered" to serverLogBuffer.size
        )
    }

    fun getAllPlayersView(): List<AdminPlayerView> =
        playerService.getAllPlayers().map { it.toAdminView() }

    fun getPlayerView(playerId: String): AdminPlayerView? =
        playerService.getPlayer(playerId)?.toAdminView()

    suspend fun modifyPlayer(
        adminUsername: String,
        playerId: String,
        field: String,
        value: String
    ): Result<String> {
        val player = playerService.getPlayer(playerId)
            ?: return Result.failure(Exception("Player not found"))

        return playerService.withPlayerAction(playerId) { p ->
            val oldValue: Any?
            when (field) {
                "credits" -> { oldValue = p.credits; p.credits = value.toLong() }
                "level" -> { oldValue = p.level; p.level = value.toInt(); p.experienceToNext = (100 * 1.5.pow(p.level - 1)).toLong() }
                "experience" -> { oldValue = p.experience; p.experience = value.toLong() }
                "cpu" -> { oldValue = p.maxCpu; p.maxCpu = value.toInt(); p.cpu = p.maxCpu }
                "ram" -> { oldValue = p.maxRam; p.maxRam = value.toInt(); p.ram = p.maxRam }
                "bandwidth" -> { oldValue = p.maxBandwidth; p.maxBandwidth = value.toInt(); p.bandwidth = p.maxBandwidth }
                "reputation" -> { oldValue = p.reputation; p.reputation = value.toInt() }
                "skillPoints" -> { oldValue = p.skillPoints; p.skillPoints = value.toInt() }
                "prestigeLevel" -> { oldValue = p.prestigeLevel; p.prestigeLevel = value.toInt() }
                "heatLevel" -> { oldValue = p.heatLevel; p.heatLevel = value.toInt() }
                "justicePoints" -> { oldValue = p.justicePoints; p.justicePoints = value.toInt() }
                "currentZone" -> { oldValue = p.currentZoneId; p.currentZoneId = value }
                "careerPath" -> {
                    oldValue = p.careerPath
                    if (value in listOf("undecided", "whitehat", "blackhat")) p.careerPath = value
                    else return@withPlayerAction Result.failure(Exception("Invalid career path: $value"))
                }
                else -> return@withPlayerAction Result.failure(Exception("Unknown field: $field"))
            }
            logAction(adminUsername, "modify_player", playerId, p.username, "Modified $field: '$oldValue' -> '$value'")
            AuditLog.event("admin_modify_player", playerId = playerId, extra = mapOf(
                "admin" to adminUsername, "field" to field, "oldValue" to (oldValue?.toString() ?: "null"), "newValue" to value
            ))
            Result.success("$field updated successfully")
        } ?: Result.failure(Exception("Player not found"))
    }

    suspend fun kickPlayer(adminUsername: String, playerId: String, reason: String = "Kicked by admin"): Result<String> {
        val player = playerService.getPlayer(playerId)
            ?: return Result.failure(Exception("Player not found"))

        val entry = connectionRegistry.getByPlayer(playerId)
        if (entry == null) return Result.failure(Exception("Player is not online"))

        runCatching {
            entry.session.close(CloseReason(CloseReason.Codes.NORMAL, reason))
        }

        playerService.withPlayerAction(playerId) { p ->
            p.isOnline = false
        }

        logAction(adminUsername, "kick_player", playerId, player.username, reason)
        AuditLog.event("player_kicked", playerId = playerId, extra = mapOf("admin" to adminUsername, "reason" to reason))
        return Result.success("Player kicked: ${player.username}")
    }

    suspend fun broadcastMessage(title: String, message: String, messageType: String, priority: String): Result<String> {
        val envelope = mapOf<String, Any?>(
            "type" to "push_server_announcement",
            "title" to title,
            "message" to message,
            "messageType" to messageType,
            "priority" to priority,
            "timestamp" to System.currentTimeMillis()
        )
        connectionRegistry.broadcastToChannel("player", envelope)
        log.info("Admin broadcast: [{}] {} - {}", title, messageType, message)
        return Result.success("Broadcast sent to all online players")
    }

    fun getServerLogs(limit: Int = 100, level: String? = null): List<ServerLog> {
        val all = serverLogBuffer.toList()
        return if (level != null) all.filter { it.level == level }.take(limit)
        else all.take(limit)
    }

    fun addServerLog(level: String, logger: String, message: String) {
        serverLogBuffer.addFirst(ServerLog(level = level, logger = logger, message = message))
        if (serverLogBuffer.size > maxServerLogEntries) serverLogBuffer.removeLast()
    }

    fun getEscalatedCheatAlerts(): List<CheatAlert> {
        val candidates = cheatDetectionService.getAutoBanCandidates()
        return candidates
    }

    suspend fun autoBanCheaters(adminUsername: String = "system"): Int {
        var banned = 0
        for (alert in cheatDetectionService.getAutoBanCandidates()) {
            val result = banService.banPlayer(
                playerId = alert.playerId,
                bannedBy = adminUsername,
                reason = "Auto-ban: ${alert.alertType.name} - ${alert.description}",
                durationHours = 24
            )
            if (result.isSuccess) {
                cheatDetectionService.resolveAlert(alert.id)
                banned++
                logAction(adminUsername, "auto_ban", alert.playerId, alert.username,
                    "Auto-banned for ${alert.alertType.name}: ${alert.description}")
            }
        }
        return banned
    }

    fun searchPlayers(query: String): List<AdminPlayerView> {
        val q = query.lowercase()
        return playerService.getAllPlayers()
            .filter { it.username.lowercase().contains(q) || it.id.lowercase().contains(q) }
            .map { it.toAdminView() }
    }

    fun getActionLog(limit: Int = 50): List<AdminAction> = actionLog.toList().take(limit)

    private fun logAction(admin: String, action: String, targetId: String?, targetName: String?, details: String) {
        actionLog.addFirst(AdminAction(
            action = action,
            adminUsername = admin,
            targetPlayerId = targetId,
            targetUsername = targetName,
            details = details
        ))
        if (actionLog.size > maxActionLogEntries) actionLog.removeLast()
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "admin_$token"
    }

    private fun Int.pow(exp: Int): Int = when {
        exp < 0 -> throw IllegalArgumentException("Negative exponent")
        exp == 0 -> 1
        else -> (1..exp).fold(1) { acc, _ -> acc * this }
    }

    private fun Double.pow(exp: Int): Double = when {
        exp < 0 -> throw IllegalArgumentException("Negative exponent")
        exp == 0 -> 1.0
        else -> (1..exp).fold(1.0) { acc, _ -> acc * this }
    }
}
