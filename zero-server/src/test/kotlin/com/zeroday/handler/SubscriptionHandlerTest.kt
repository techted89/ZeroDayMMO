package com.zeroday.handler

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubscriptionHandlerTest {
    private val handler = com.zeroday.handler.handlers.SubscriptionHandler()
    private val router = MessageRouter(emptyList())

    @Test
    fun `subscribe adds channels`() = runTest {
        val conn = ConnectionInfo(sessionId = "s1", playerId = "p1")
        val ctx = HandlerContext(FakeWebSocketSession(), conn, services(), router)
        val payload = buildJsonObject {
            put("channels", buildJsonArray { add(JsonPrimitive("inventory")); add(JsonPrimitive("tasks")) })
        }
        val result = handler.handle(ctx, "subscribe", payload)
        assertTrue(result is HandlerResult.Ok)
        val env = result.toEnvelope()
        assertEquals("subscribed", env["type"])
        assertTrue(conn.isSubscribed("inventory"))
        assertTrue(conn.isSubscribed("tasks"))
    }

    @Test
    fun `unsubscribe removes channels`() = runTest {
        val conn = ConnectionInfo(sessionId = "s2", playerId = "p2")
        conn.subscribedChannels.add("inventory")
        conn.subscribedChannels.add("tasks")
        val ctx = HandlerContext(FakeWebSocketSession(), conn, services(), router)
        val payload = buildJsonObject {
            put("channels", buildJsonArray { add(JsonPrimitive("tasks")) })
        }
        handler.handle(ctx, "unsubscribe", payload)
        assertTrue(conn.isSubscribed("inventory"))
        assertTrue(!conn.isSubscribed("tasks"))
    }

    @Test
    fun `default channels cannot be unsubscribed`() = runTest {
        val conn = ConnectionInfo(sessionId = "s3", playerId = "p3")
        assertTrue(conn.isSubscribed("player"))
        assertTrue(conn.isSubscribed("notifications"))
        val ctx = HandlerContext(FakeWebSocketSession(), conn, services(), router)
        val payload = buildJsonObject {
            put("channels", buildJsonArray { add(JsonPrimitive("player")) })
        }
        handler.handle(ctx, "unsubscribe", payload)
        assertTrue(conn.isSubscribed("player"), "Default channels are protected")
    }

    @Test
    fun `invalid channel names are reported`() = runTest {
        val conn = ConnectionInfo(sessionId = "s4", playerId = "p4")
        val ctx = HandlerContext(FakeWebSocketSession(), conn, services(), router)
        val payload = buildJsonObject {
            put("channels", buildJsonArray { add(JsonPrimitive("bogus")) })
        }
        val result = handler.handle(ctx, "subscribe", payload)
        val env = result.toEnvelope()
        assertEquals("subscribed", env["type"])
        // "bogus" is not a valid channel so it should appear in "invalid"
        @Suppress("UNCHECKED_CAST")
        val invalid = env["invalid"] as? List<String> ?: emptyList()
        assertTrue(invalid.contains("bogus"), "Invalid channel should be reported: $invalid")
    }

    private fun services(): ServiceRegistry {
        val ps = com.zeroday.service.PlayerService(com.zeroday.security.BcryptPasswordHasher())
        return ServiceRegistry.minimal(ps, com.zeroday.util.AppScope.scope)
    }
}
