package com.zeroday.handler

import com.zeroday.protocol.MessageProtocol
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Top-level WebSocket entrypoint. Owns the per-connection lifecycle:
 *
 * 1. registers a [ConnectionInfo] in [ConnectionRegistry];
 * 2. pumps incoming text frames, parses them, and hands them off to
 *    [MessageRouter];
 *
 * Resource regen is now a *single* ticker driven by
 * [com.zeroday.service.ResourceRegenTicker], not a per-connection job.
 * That removes N coroutines and N timer wakeups per second at the cost
 * of one extra mutex acquisition per player per tick.
 */
class WebSocketHandler(
    private val services: ServiceRegistry,
    private val router: MessageRouter,
    private val registry: ConnectionRegistry
) {
    private val log = LoggerFactory.getLogger(WebSocketHandler::class.java)
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    suspend fun handle(session: WebSocketSession) {
        val remote = runCatching {
            @Suppress("DEPRECATION")
            (session as? io.ktor.server.websocket.WebSocketServerSession)?.call?.request?.local?.remoteHost
        }.getOrNull()
        val result = registry.register(session, remoteAddress = remote)
        val entry = when (result) {
            is ConnectionRegistry.RegisterResult.Rejected -> {
                log.info("WebSocket connection rejected: reason={} remote={}", result.reason, remote)
                return
            }
            is ConnectionRegistry.RegisterResult.Accepted -> result.entry
        }
        val sessionId = entry.info.sessionId
        log.info("WebSocket open: sessionId={} remote={}", sessionId, entry.info.remoteAddress)

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        if (text.length > com.zeroday.config.ServerConfig.MAX_INBOUND_BYTES) {
                            log.warn(
                                "Dropping oversized frame from sessionId={} ({} bytes > {})",
                                sessionId, text.length, com.zeroday.config.ServerConfig.MAX_INBOUND_BYTES
                            )
                            runCatching {
                                session.close(CloseReason(CloseReason.Codes.TOO_BIG, "Frame too large"))
                            }
                            break
                        }
                        val response = processFrame(sessionId, text)
                        if (response != null) session.send(Frame.Text(response))
                    }
                    is Frame.Close -> {
                        log.info("WebSocket close received: sessionId={}", sessionId)
                        break
                    }
                    else -> {}
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("WebSocket error: sessionId={}", sessionId, e)
        } finally {
            cleanup(sessionId, entry)
        }
    }

    private suspend fun processFrame(sessionId: String, raw: String): String? {
        val conn = registry.get(sessionId)?.info
            ?: run {
                log.warn("Dropping frame for unknown sessionId={}", sessionId)
                return null
            }
        registry.touch(sessionId)
        return try {
            val msg = json.parseToJsonElement(raw).jsonObject
            val type = msg[MessageProtocol.FIELD_TYPE]?.jsonPrimitive?.contentOrNull ?: "unknown"
            val payload = msg[MessageProtocol.FIELD_PAYLOAD]?.jsonObject ?: JsonObject(emptyMap())
            val ctx = HandlerContext(
                session = registry.get(sessionId)!!.session,
                connection = conn,
                services = services,
                router = router
            )
            router.dispatch(type, payload, ctx)
        } catch (e: Exception) {
            log.warn("Failed to parse/dispatch frame from sessionId={}: {}", sessionId, e.message)
            null
        }
    }

    private fun cleanup(sessionId: String, entry: ConnectionEntry) {
        entry.perPlayerJob?.cancel()
        registry.unregister(sessionId)
    }
}
