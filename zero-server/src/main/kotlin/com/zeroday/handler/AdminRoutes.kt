package com.zeroday.handler

import com.zeroday.config.AdminConfig
import com.zeroday.model.*
import com.zeroday.service.AdminService
import com.zeroday.service.BanService
import com.zeroday.service.CheatDetectionService
import com.zeroday.service.PlayerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun Route.adminRoutes(
    adminService: AdminService,
    playerService: PlayerService,
    banService: BanService,
    cheatDetectionService: CheatDetectionService
) {
    val log = LoggerFactory.getLogger("com.zeroday.admin.AdminRoutes")
    val json = Json { prettyPrint = false; ignoreUnknownKeys = true; isLenient = true }

    suspend fun ApplicationCall.requireAdmin(): String? {
        val auth = request.header("Authorization")
        if (auth == null || !auth.startsWith("Bearer ")) {
            respondText("""{"error":"Missing or invalid authorization header"}""", ContentType.Application.Json, HttpStatusCode.Unauthorized)
            return null
        }
        val token = auth.removePrefix("Bearer ")
        val session = adminService.validateToken(token)
        if (session == null) {
            respondText("""{"error":"Invalid or expired admin token"}""", ContentType.Application.Json, HttpStatusCode.Unauthorized)
            return null
        }
        return session.username
    }

    fun Map<String, Any?>.toJson(): String = json.encodeToString(this)

    @Serializable
    data class AuthRequest(val username: String, val password: String)

    @Serializable
    data class AuthResponse(val token: String, val username: String)

    @Serializable
    data class BanRequest(val playerId: String, val reason: String, val durationHours: Long? = null)

    @Serializable
    data class ModifyRequest(val playerId: String, val field: String, val value: String)

    @Serializable
    data class BroadcastReq(val title: String, val message: String, val messageType: String = "announcement", val priority: String = "normal")

    route("/admin") {
        get("/health") {
            call.respondText(mapOf(
                "status" to "ok",
                "adminService" to "running",
                "timestamp" to System.currentTimeMillis()
            ).toJson(), ContentType.Application.Json)
        }

        post("/auth") {
            val body = try { call.receive<AuthRequest>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val result = adminService.authenticate(body.username, body.password)
            if (result.isSuccess) {
                call.respond(AuthResponse(result.getOrThrow(), body.username))
            } else {
                call.respondText("""{"error":"${result.exceptionOrNull()?.message}"}""",
                    ContentType.Application.Json, HttpStatusCode.Unauthorized)
            }
        }

        post("/logout") {
            val admin = call.requireAdmin() ?: return@post
            val auth = call.request.header("Authorization")?.removePrefix("Bearer ") ?: ""
            adminService.invalidateToken(auth)
            call.respondText("""{"status":"logged_out","username":"$admin"}""", ContentType.Application.Json)
        }

        get("/overview") {
            val admin = call.requireAdmin() ?: return@get
            call.respondText(adminService.getServerOverview().toJson(), ContentType.Application.Json)
        }

        get("/players") {
            val admin = call.requireAdmin() ?: return@get
            val search = call.request.queryParameters["search"]
            val players = if (search != null) adminService.searchPlayers(search) else adminService.getAllPlayersView()
            call.respondText(mapOf("players" to players, "count" to players.size).toJson(), ContentType.Application.Json)
        }

        get("/players/{id}") {
            val admin = call.requireAdmin() ?: return@get
            val id = call.parameters["id"] ?: return@get
            val view = adminService.getPlayerView(id)
            if (view != null) {
                val banRecord = banService.getBanRecord(id)
                val cheatAlerts = cheatDetectionService.getAlertsForPlayer(id)
                call.respondText(mapOf(
                    "player" to view,
                    "banRecord" to banRecord,
                    "cheatAlerts" to cheatAlerts
                ).toJson(), ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"Player not found"}""", ContentType.Application.Json, HttpStatusCode.NotFound)
            }
        }

        post("/players/ban") {
            val admin = call.requireAdmin() ?: return@post
            val body = try { call.receive<BanRequest>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val result = banService.banPlayer(body.playerId, admin, body.reason, body.durationHours)
            if (result.isSuccess) {
                call.respondText(mapOf("status" to "banned", "record" to result.getOrThrow()).toJson(), ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"${result.exceptionOrNull()?.message}"}""",
                    ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        post("/players/unban") {
            val admin = call.requireAdmin() ?: return@post
            val body = try { call.receive<Map<String, String>>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val playerId = body["playerId"] ?: return@post
            val result = banService.unbanPlayer(playerId, admin)
            if (result.isSuccess) {
                call.respondText("""{"status":"unbanned","message":"${result.getOrThrow()}"}""", ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"${result.exceptionOrNull()?.message}"}""",
                    ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        post("/players/kick") {
            val admin = call.requireAdmin() ?: return@post
            val body = try { call.receive<Map<String, String>>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val playerId = body["playerId"] ?: return@post
            val reason = body["reason"] ?: "Kicked by admin"
            val result = adminService.kickPlayer(admin, playerId, reason)
            if (result.isSuccess) {
                call.respondText("""{"status":"kicked","message":"${result.getOrThrow()}"}""", ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"${result.exceptionOrNull()?.message}"}""",
                    ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        post("/players/modify") {
            val admin = call.requireAdmin() ?: return@post
            val body = try { call.receive<ModifyRequest>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val result = adminService.modifyPlayer(admin, body.playerId, body.field, body.value)
            if (result.isSuccess) {
                call.respondText("""{"status":"modified","message":"${result.getOrThrow()}"}""", ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"${result.exceptionOrNull()?.message}"}""",
                    ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        get("/bans") {
            val admin = call.requireAdmin() ?: return@get
            val bans = banService.getAllBans()
            call.respondText(mapOf("bans" to bans, "count" to bans.size).toJson(), ContentType.Application.Json)
        }

        post("/broadcast") {
            val admin = call.requireAdmin() ?: return@post
            val body = try { call.receive<BroadcastReq>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val result = adminService.broadcastMessage(body.title, body.message, body.messageType, body.priority)
            if (result.isSuccess) {
                call.respondText("""{"status":"broadcast_sent","message":"${result.getOrThrow()}"}""", ContentType.Application.Json)
            } else {
                call.respondText("""{"error":"${result.exceptionOrNull()?.message}"}""",
                    ContentType.Application.Json, HttpStatusCode.InternalServerError)
            }
        }

        get("/cheats") {
            val admin = call.requireAdmin() ?: return@get
            val severity = call.request.queryParameters["severity"]?.let { CheatSeverity.valueOf(it.uppercase()) }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val alerts = cheatDetectionService.getAlerts(severity, limit)
            call.respondText(mapOf(
                "alerts" to alerts,
                "count" to alerts.size,
                "severityBreakdown" to cheatDetectionService.getAlertCountBySeverity(),
                "escalated" to adminService.getEscalatedCheatAlerts()
            ).toJson(), ContentType.Application.Json)
        }

        post("/cheats/resolve") {
            val admin = call.requireAdmin() ?: return@post
            val body = try { call.receive<Map<String, String>>() } catch (e: Exception) {
                call.respondText("""{"error":"Invalid request body"}""", ContentType.Application.Json, HttpStatusCode.BadRequest)
                return@post
            }
            val alertId = body["alertId"] ?: return@post
            val resolved = cheatDetectionService.resolveAlert(alertId)
            call.respondText("""{"resolved":$resolved}""", ContentType.Application.Json)
        }

        post("/cheats/auto-ban") {
            val admin = call.requireAdmin() ?: return@post
            val count = adminService.autoBanCheaters(admin)
            call.respondText("""{"status":"auto_ban_completed","banned":$count}""", ContentType.Application.Json)
        }

        get("/logs") {
            val admin = call.requireAdmin() ?: return@get
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val level = call.request.queryParameters["level"]
            val logs = adminService.getServerLogs(limit, level)
            call.respondText(mapOf("logs" to logs, "count" to logs.size).toJson(), ContentType.Application.Json)
        }

        get("/logs/actions") {
            val admin = call.requireAdmin() ?: return@get
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val actions = adminService.getActionLog(limit)
            call.respondText(mapOf("actions" to actions, "count" to actions.size).toJson(), ContentType.Application.Json)
        }

        get("/stats") {
            val admin = call.requireAdmin() ?: return@get
            val online = playerService.getOnlinePlayers()
            val all = playerService.getAllPlayers()
            val totalCredits = all.sumOf { it.credits }
            val totalXp = all.sumOf { it.experience }
            val avgLevel = if (all.isNotEmpty()) all.map { it.level }.average() else 0.0
            val careerBreakdown = all.groupBy { it.careerPath }.mapValues { it.value.size }

            call.respondText(mapOf(
                "totalPlayers" to all.size,
                "onlinePlayers" to online.size,
                "totalCreditsInEconomy" to totalCredits,
                "totalExperienceEarned" to totalXp,
                "averageLevel" to avgLevel,
                "careerBreakdown" to careerBreakdown,
                "activeBans" to banService.getAllBans().size,
                "cheatAlertCount" to cheatDetectionService.getAlertCount(),
                "onlinePlayerList" to online.map { mapOf(
                    "id" to it.id,
                    "username" to it.username,
                    "level" to it.level,
                    "zone" to it.currentZoneId,
                    "career" to it.careerPath
                )}
            ).toJson(), ContentType.Application.Json)
        }
    }
}
