package com.zeroday.service

import com.zeroday.model.*
import com.zeroday.security.AuditLog
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

class CheatDetectionService(
    private val playerService: PlayerService,
    private val banService: BanService
) {
    private val log = LoggerFactory.getLogger(CheatDetectionService::class.java)
    private val alerts = ConcurrentLinkedDeque<CheatAlert>()
    private val maxAlerts = 500

    private val commandFrequency = ConcurrentHashMap<String, MutableList<Long>>()
    private val commandWindowMs = 1000L
    private val maxCommandsPerWindow = 30

    private val travelHistory = ConcurrentHashMap<String, MutableList<Long>>()
    private val minTravelIntervalMs = 10_000L

    private val resourceHistory = ConcurrentHashMap<String, MutableList<ResourceSnapshot>>()
    private val maxCreditDeltaPerMinute = 100_000L
    private val maxXpDeltaPerMinute = 50_000L

    data class ResourceSnapshot(
        val timestamp: Long,
        val credits: Long,
        val experience: Long,
        val level: Int
    )

    fun checkCommandSpam(playerId: String): Boolean {
        val now = System.currentTimeMillis()
        val times = commandFrequency.getOrPut(playerId) { mutableListOf() }
        times.add(now)
        times.removeAll { now - it > commandWindowMs }

        if (times.size > maxCommandsPerWindow) {
            val player = playerService.getPlayer(playerId)
            createAlert(
                playerId = playerId,
                username = player?.username ?: "unknown",
                alertType = CheatAlertType.COMMAND_SPAM,
                description = "Sent ${times.size} commands in 1 second (limit: $maxCommandsPerWindow)",
                severity = if (times.size > maxCommandsPerWindow * 2) CheatSeverity.HIGH else CheatSeverity.MEDIUM,
                metadata = mapOf("commandCount" to times.size, "window" to commandWindowMs)
            )
            return true
        }
        return false
    }

    fun checkTravelSpeed(playerId: String): Boolean {
        val now = System.currentTimeMillis()
        val times = travelHistory.getOrPut(playerId) { mutableListOf() }
        if (times.isNotEmpty()) {
            val elapsed = now - times.last()
            if (elapsed < minTravelIntervalMs) {
                val player = playerService.getPlayer(playerId)
                createAlert(
                    playerId = playerId,
                    username = player?.username ?: "unknown",
                    alertType = CheatAlertType.RAPID_TRAVEL,
                    description = "Travel interval ${elapsed}ms < minimum ${minTravelIntervalMs}ms",
                    severity = CheatSeverity.MEDIUM,
                    metadata = mapOf("elapsedMs" to elapsed, "minIntervalMs" to minTravelIntervalMs)
                )
                return true
            }
        }
        times.add(now)
        if (times.size > 100) times.removeAt(0)
        return false
    }

    suspend fun checkResourceAnomaly(playerId: String): Boolean {
        val player = playerService.getPlayer(playerId) ?: return false
        val now = System.currentTimeMillis()
        val history = resourceHistory.getOrPut(playerId) { mutableListOf() }
        history.add(ResourceSnapshot(now, player.credits, player.experience, player.level))
        history.removeAll { now - it.timestamp > 60_000L }

        if (history.size >= 2) {
            val oldest = history.first()
            val creditDelta = player.credits - oldest.credits
            val xpDelta = player.experience - oldest.experience

            if (creditDelta > maxCreditDeltaPerMinute) {
                createAlert(
                    playerId = playerId,
                    username = player.username,
                    alertType = CheatAlertType.CREDIT_TAMPER,
                    description = "Credit gain ${creditDelta} in 1 minute (limit: $maxCreditDeltaPerMinute)",
                    severity = CheatSeverity.HIGH,
                    metadata = mapOf("creditDelta" to creditDelta, "limit" to maxCreditDeltaPerMinute)
                )
                return true
            }

            if (xpDelta > maxXpDeltaPerMinute) {
                createAlert(
                    playerId = playerId,
                    username = player.username,
                    alertType = CheatAlertType.XP_TAMPER,
                    description = "XP gain ${xpDelta} in 1 minute (limit: $maxXpDeltaPerMinute)",
                    severity = CheatSeverity.HIGH,
                    metadata = mapOf("xpDelta" to xpDelta, "limit" to maxXpDeltaPerMinute)
                )
                return true
            }
        }
        return false
    }

    fun checkLevelJump(playerId: String, oldLevel: Int, newLevel: Int): Boolean {
        if (newLevel > oldLevel + 3) {
            val player = playerService.getPlayer(playerId)
            createAlert(
                playerId = playerId,
                username = player?.username ?: "unknown",
                alertType = CheatAlertType.LEVEL_JUMP,
                description = "Level jump from $oldLevel to $newLevel in single operation",
                severity = CheatSeverity.CRITICAL,
                metadata = mapOf("oldLevel" to oldLevel, "newLevel" to newLevel)
            )
            return true
        }
        return false
    }

    fun checkImpossibleAction(playerId: String, action: String): Boolean {
        val player = playerService.getPlayer(playerId) ?: return false
        val zoneId = player.currentZoneId
        val careerPath = player.careerPath

        val impossible = when (action) {
            "arrest_self" -> true
            "faction_upgrade_no_faction" -> player.factionId == null
            "travel_locked_zone" -> zoneId == "nexus" && player.level < 25
            "career_change_after_chosen" -> careerPath != "undecided" && action == "career_change"
            else -> false
        }

        if (impossible) {
            createAlert(
                playerId = playerId,
                username = player.username,
                alertType = CheatAlertType.IMPOSSIBLE_ACTION,
                description = "Attempted impossible action: $action",
                severity = CheatSeverity.HIGH,
                metadata = mapOf("action" to action, "zoneId" to zoneId, "careerPath" to careerPath)
            )
            return true
        }
        return false
    }

    private fun createAlert(
        playerId: String,
        username: String,
        alertType: CheatAlertType,
        description: String,
        severity: CheatSeverity,
        metadata: Map<String, Any?> = emptyMap()
    ): CheatAlert {
        val alert = CheatAlert(
            playerId = playerId,
            username = username,
            alertType = alertType,
            description = description,
            severity = severity,
            metadata = metadata
        )
        alerts.addFirst(alert)
        if (alerts.size > maxAlerts) alerts.removeLast()

        AuditLog.event("cheat_detected", playerId = playerId, extra = mapOf(
            "alertType" to alertType.name,
            "severity" to severity.name,
            "description" to description
        ))

        when (severity) {
            CheatSeverity.CRITICAL -> log.error("CHEAT DETECTED [{}] {}: {} - {}", severity, username, alertType, description)
            CheatSeverity.HIGH -> log.warn("Cheat detected [{}] {}: {} - {}", severity, username, alertType, description)
            else -> log.info("Cheat flag [{}] {}: {} - {}", severity, username, alertType, description)
        }

        return alert
    }

    fun getAlerts(severity: CheatSeverity? = null, limit: Int = 50): List<CheatAlert> {
        val all = alerts.toList()
        return if (severity != null) all.filter { it.severity == severity }.take(limit)
        else all.take(limit)
    }

    fun getAlertsForPlayer(playerId: String, limit: Int = 20): List<CheatAlert> =
        alerts.filter { it.playerId == playerId }.take(limit)

    fun getAlertCount(): Int = alerts.size

    fun getAlertCountBySeverity(): Map<CheatSeverity, Int> =
        alerts.groupBy { it.severity }.mapValues { it.value.size }

    fun resolveAlert(alertId: String): Boolean {
        val alert = alerts.find { it.id == alertId } ?: return false
        alerts.remove(alert)
        alerts.addFirst(alert.copy(resolved = true))
        return true
    }

    fun getAutoBanCandidates(threshold: CheatSeverity = CheatSeverity.CRITICAL): List<CheatAlert> =
        alerts.filter { it.severity >= threshold && !it.resolved }
            .groupBy { it.playerId }
            .filter { it.value.size >= 3 }
            .flatMap { it.value.take(1) }

    fun clearResolvedAlerts(): Int {
        val count = alerts.count { it.resolved }
        alerts.removeAll { it.resolved }
        return count
    }
}
