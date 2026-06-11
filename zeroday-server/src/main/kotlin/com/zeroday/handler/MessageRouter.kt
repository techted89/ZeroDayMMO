package com.zeroday.handler

import com.zeroday.protocol.ErrorCategory
import com.zeroday.protocol.MessageProtocol
import com.zeroday.protocol.ServerError
import com.zeroday.security.AuditLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Routes incoming WebSocket messages to the correct [MessageHandler] and
 * serialises the result. Handlers are registered in the constructor — there
 * is no reflection or string-`when` so dispatching is O(1) and adding a new
 * handler is a single line in [WebSocketHandler].
 *
 * Also performs two pre-dispatch checks:
 *   1. [commandRateLimiter] — caps the rate of *gameplay* requests per
 *      player (auth requests are throttled separately by [com.zeroday.handler.handlers.AuthHandler]).
 *   2. [idempotencyCache] — dedupes short-lived retries using a
 *      `requestId` field on the inbound envelope. If the same id is
 *      replayed within [idempotencyWindowMs] the cached response is
 *      returned without re-running the handler.
 */
class MessageRouter(
    handlers: List<MessageHandler>,
    private val commandRateLimiter: com.zeroday.security.RateLimiter =
        com.zeroday.security.RateLimiter(maxRequests = 30, windowMs = 1_000L),
    val idempotencyWindowMs: Long = 30_000L
) {
    @Volatile private var connectionRegistry: ConnectionRegistry? = null
    private val log = LoggerFactory.getLogger(MessageRouter::class.java)
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val registry: Map<String, MessageHandler> = handlers
        .flatMap { handler -> handler.handledTypes.map { it to handler } }
        .toMap()

    /** Last-seen response per requestId, with a timestamp. Garbage collected lazily on hit. */
    private data class IdempotencyEntry(val response: String, val ts: Long)
    private val idempotencyCache = ConcurrentHashMap<String, IdempotencyEntry>()

    fun supports(type: String): Boolean = registry.containsKey(type)

    fun attachConnectionRegistry(registry: ConnectionRegistry) {
        connectionRegistry = registry
    }

    suspend fun bindPlayer(sessionId: String, playerId: String, username: String) {
        connectionRegistry?.bindPlayer(sessionId, playerId, username)
    }

    fun registry(): ConnectionRegistry? = connectionRegistry

    fun encode(envelope: Map<String, Any?>): String {
        val obj = buildJsonObject {
            for ((k, v) in envelope) {
                putAny(k, v)
            }
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putAny(key: String, value: Any?) {
        when (value) {
            null -> put(key, kotlinx.serialization.json.JsonNull)
            is String -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
            is Boolean -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
            is Number -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
            is kotlinx.serialization.json.JsonElement -> put(key, value)
            is Map<*, *> -> {
                val nested = buildJsonObject {
                    for ((nk, nv) in value) {
                        if (nk is String) putAny(nk, nv)
                    }
                }
                put(key, nested)
            }
            is Iterable<*> -> {
                val list = kotlinx.serialization.json.buildJsonArray {
                    for (item in value) {
                        when (item) {
                            null -> add(kotlinx.serialization.json.JsonNull)
                            is String -> add(kotlinx.serialization.json.JsonPrimitive(item))
                            is Boolean -> add(kotlinx.serialization.json.JsonPrimitive(item))
                            is Number -> add(kotlinx.serialization.json.JsonPrimitive(item))
                            is kotlinx.serialization.json.JsonElement -> add(item)
                            else -> add(kotlinx.serialization.json.JsonPrimitive(item.toString()))
                        }
                    }
                }
                put(key, list)
            }
            else -> put(key, kotlinx.serialization.json.JsonPrimitive(value.toString()))
        }
    }

    /**
     * Convenience used by [WebSocketHandler] to capture the optional
     * `requestId` from the top-level message body.
     */
    fun extractRequestId(payload: JsonObject): String? =
        (payload["requestId"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content

    /**
     * If the inbound payload carries a `requestId` we've already responded
     * to within the window, return the cached response and short-circuit.
     * Otherwise run the handler and remember the response.
     */
    private fun idempotencyLookup(requestId: String?): String? {
        if (requestId == null) return null
        val now = System.currentTimeMillis()
        val entry = idempotencyCache[requestId] ?: return null
        if (now - entry.ts > idempotencyWindowMs) {
            idempotencyCache.remove(requestId)
            return null
        }
        return entry.response
    }

    private fun idempotencyStore(requestId: String?, response: String) {
        if (requestId == null) return
        idempotencyCache[requestId] = IdempotencyEntry(response, System.currentTimeMillis())
    }

    suspend fun dispatch(
        type: String,
        payload: JsonObject,
        ctx: HandlerContext
    ): String {
        // 1. Idempotency replay check
        val requestId = extractRequestId(payload)
        idempotencyLookup(requestId)?.let { cached ->
            return cached
        }

        // 2. Per-player command rate limit. Applied to *all* authenticated
        //    gameplay traffic; auth handler has its own pre-login limiter.
        val playerKey = ctx.connection.playerId ?: ctx.connection.sessionId
        if (!commandRateLimiter.tryAcquire("cmd:$playerKey")) {
            AuditLog.event("cmd.rate_limited", playerId = ctx.connection.playerId, ip = ctx.connection.remoteAddress, extra = mapOf("type" to type))
            return encode(
                HandlerResult.Err(
                    ServerError.fromMessage("Slow down: too many requests", ErrorCategory.RATE_LIMITED)
                ).toEnvelope()
            )
        }

        val handler = registry[type]
        if (handler == null) {
            log.warn("Unknown message type from session={}: {}", ctx.connection.sessionId, type)
            return encode(
                HandlerResult.Err(
                    ServerError.fromMessage("Unknown message type: $type", ErrorCategory.VALIDATION)
                ).toEnvelope()
            )
        }
        if (handler.requiresAuth && !ctx.connection.isAuthenticated()) {
            return encode(
                HandlerResult.Err(
                    ServerError.fromMessage("Authentication required for '$type'", ErrorCategory.AUTHENTICATION)
                ).toEnvelope()
            )
        }

        val response = try {
            val result = handler.handle(ctx, type, payload)
            encode(result.toEnvelope())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Handler '{}' threw for session={}", type, ctx.connection.sessionId, e)
            encode(
                HandlerResult.Err(
                    ServerError.fromMessage(e.message ?: "Internal error", ErrorCategory.INTERNAL)
                ).toEnvelope()
            )
        }
        idempotencyStore(requestId, response)
        return response
    }

    companion object {
        const val TIMESTAMP_FIELD = MessageProtocol.FIELD_TIMESTAMP
    }
}
