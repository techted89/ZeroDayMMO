package com.zeroday.handler.handlers

import com.zeroday.handler.HandlerContext
import com.zeroday.handler.HandlerResult
import com.zeroday.handler.MessageHandler
import com.zeroday.handler.reqStr
import com.zeroday.model.toMap
import com.zeroday.model.toSnapshot
import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.RequestTypes
import com.zeroday.protocol.ResponseTypes
import com.zeroday.protocol.ServerError
import com.zeroday.security.AuditLog
import com.zeroday.security.RateLimiter
import kotlinx.serialization.json.JsonObject

/**
 * Handles connection lifecycle messages: registration, login, logout, ping.
 *
 * Auth handlers do *not* require an authenticated player (they're the thing
 * that produces one), so we override [requiresAuth] to false. Every other
 * handler in the system relies on a player id being present in the
 * connection by the time it runs.
 */
class AuthHandler(
    private val rateLimiter: RateLimiter,
    private val dailyLoginService: com.zeroday.service.DailyLoginService? = null
) : MessageHandler {
    override val handledTypes: Set<String> = setOf(
        RequestTypes.REGISTER,
        RequestTypes.LOGIN,
        RequestTypes.LOGOUT,
        RequestTypes.PING
    )
    override val requiresAuth: Boolean = false

    override suspend fun handle(
        ctx: HandlerContext,
        type: String,
        payload: JsonObject
    ): HandlerResult = when (type) {
        RequestTypes.REGISTER -> handleRegister(ctx, payload)
        RequestTypes.LOGIN -> handleLogin(ctx, payload)
        RequestTypes.LOGOUT -> handleLogout(ctx)
        RequestTypes.PING -> HandlerResult.Ok(
            type = ResponseTypes.PONG,
            payload = mapOf("timestamp" to System.currentTimeMillis())
        )
        else -> HandlerResult.Err(
            ServerError.fromMessage("Unsupported auth request: $type", ErrorCategory.VALIDATION)
        )
    }

    private suspend fun handleRegister(ctx: HandlerContext, payload: JsonObject): HandlerResult {
        val username = try { payload.reqStr("username") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Username required", ErrorCategory.VALIDATION))
        }
        val password = try { payload.reqStr("password") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Password required", ErrorCategory.VALIDATION))
        }
        // Validate shape *before* hitting the rate limiter so a script
        // that fires obviously-bad requests doesn't burn our budget.
        val u = com.zeroday.security.InputValidation.validateUsername(username)
        if (u is com.zeroday.security.InputValidation.ValidationResult.Invalid) {
            return HandlerResult.Err(ServerError.fromMessage(u.reason, ErrorCategory.VALIDATION))
        }
        if (com.zeroday.security.InputValidation.isReservedUsername(username)) {
            return HandlerResult.Err(
                ServerError.fromMessage("That username is reserved", ErrorCategory.VALIDATION)
            )
        }
        val p = com.zeroday.security.InputValidation.validatePassword(password)
        if (p is com.zeroday.security.InputValidation.ValidationResult.Invalid) {
            return HandlerResult.Err(ServerError.fromMessage(p.reason, ErrorCategory.VALIDATION))
        }
        val key = "register:${ctx.connection.remoteAddress ?: "anon"}"
        if (!rateLimiter.tryAcquire(key)) {
            return HandlerResult.Err(ServerError.fromMessage("Too many registration attempts; slow down", ErrorCategory.RATE_LIMITED))
        }

        return ctx.services.playerService.register(username, password).fold(
            onSuccess = { player ->
                ctx.connection.username = player.username
                AuditLog.event("auth.register.success", playerId = player.id, ip = ctx.connection.remoteAddress)
                HandlerResult.Ok(
                    type = ResponseTypes.REGISTER_SUCCESS,
                    payload = mapOf<String, Any?>(
                        "player" to player.toSnapshot().toMap(),
                        "message" to "Account created. Welcome, ${player.username}!"
                    )
                )
            },
            onFailure = { error ->
                AuditLog.event("auth.register.failure", ip = ctx.connection.remoteAddress, extra = mapOf("username" to username, "reason" to (error.message ?: "unknown")))
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Registration failed", ErrorCategory.CONFLICT))
            }
        )
    }

    private suspend fun handleLogin(ctx: HandlerContext, payload: JsonObject): HandlerResult {
        val username = try { payload.reqStr("username") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Username required", ErrorCategory.VALIDATION))
        }
        val password = try { payload.reqStr("password") } catch (_: Exception) {
            return HandlerResult.Err(ServerError.fromMessage("Password required", ErrorCategory.VALIDATION))
        }
        val u = com.zeroday.security.InputValidation.validateUsername(username)
        if (u is com.zeroday.security.InputValidation.ValidationResult.Invalid) {
            return HandlerResult.Err(ServerError.fromMessage(u.reason, ErrorCategory.VALIDATION))
        }
        val key = "login:${ctx.connection.remoteAddress ?: "anon"}:$username"
        if (!rateLimiter.tryAcquire(key)) {
            AuditLog.event("auth.login.rate_limited", ip = ctx.connection.remoteAddress, extra = mapOf("username" to username))
            return HandlerResult.Err(ServerError.fromMessage("Too many login attempts; slow down", ErrorCategory.RATE_LIMITED))
        }

        val playerResult = ctx.services.playerService.login(username, password)
        return playerResult.fold(
            onSuccess = { player ->
                ctx.router.bindPlayer(ctx.connection.sessionId, player.id, player.username)
                AuditLog.event("auth.login.success", playerId = player.id, ip = ctx.connection.remoteAddress)

                // Process daily login (streak, idle rewards, challenge rotation).
                val loginInfo = dailyLoginService?.processLogin(player.id, ctx.connection.remoteAddress)

                val msg = buildString {
                    append("Welcome back, ${player.username}! You are level ${player.level}.")
                    if (loginInfo != null && loginInfo.message.isNotBlank()) {
                        append("\n${loginInfo.message}")
                    }
                }

                HandlerResult.Ok(
                    type = ResponseTypes.LOGIN_SUCCESS,
                    payload = mapOf<String, Any?>(
                        "player" to player.toSnapshot().toMap(),
                        "message" to msg,
                        "login_streak" to (loginInfo?.streak ?: 0),
                        "offline_reward" to loginInfo?.offlineReward?.let { r ->
                            mapOf(
                                "cpu" to r.cpuGained,
                                "ram" to r.ramGained,
                                "credits" to r.creditsGained,
                                "hours" to r.offlineHours
                            )
                        }
                    )
                )
            },
            onFailure = { error ->
                AuditLog.event("auth.login.failure", ip = ctx.connection.remoteAddress, extra = mapOf("username" to username, "reason" to (error.message ?: "unknown")))
                HandlerResult.Err(ServerError.fromMessage(error.message ?: "Login failed", ErrorCategory.AUTHENTICATION))
            }
        )
    }

    private suspend fun handleLogout(ctx: HandlerContext): HandlerResult {
        // Side effects (clearing session, stopping regen job) are handled in
        // ConnectionRegistry on session close. The client just gets an ack here.
        return HandlerResult.Ok(
            type = ResponseTypes.LOGOUT_SUCCESS,
            payload = mapOf("message" to "Logged out")
        )
    }
}
